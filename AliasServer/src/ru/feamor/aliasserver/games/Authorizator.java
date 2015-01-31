package ru.feamor.aliasserver.games;

import io.netty.buffer.ByteBuf;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.commands.CommandTypes;
import ru.feamor.aliasserver.commands.GameCommand;
import ru.feamor.aliasserver.components.DBManager;
import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.components.NettyManager;
import ru.feamor.aliasserver.components.TimeManager;
import ru.feamor.aliasserver.core.ComponentManager;
import ru.feamor.aliasserver.db.DBRequest;
import ru.feamor.aliasserver.db.RunWithParamsDBRequest;
import ru.feamor.aliasserver.db.DBRequest.RequestExecutor;
import ru.feamor.aliasserver.db.requests.Requests;
import ru.feamor.aliasserver.game.GamePlayer;
import ru.feamor.aliasserver.netty.NettyClient;
import ru.feamor.aliasserver.users.GameClient;
import ru.feamor.aliasserver.utils.Log;
import ru.feamor.aliasserver.utils.RunWithParams;
import ru.feamor.aliasserver.utils.TextUtils;

public class Authorizator {

	public static final byte INVALID_COMMAND_ID = 0;
	public static final byte INVALID_COMMAND_FORMAT = 1;
	
	public static class TypeEmailPassword {
		public static final int rq_pos_email = 0;
		public static final int rq_pos_password = 1;
		
		public static final int rs_pos_result = 0;
		
		public static final int RESULT_SUCCESS = 0;
		public static final int RESULT_NOT_FOUND = 1;
		public static final int RESULT_ERROR = 2;
		
		public static final int rs_pos_user_id = 1;
		public static final int rs_pos_user_alias = 2;
		
		public static final int rs_pos_error = 1;
	}
	
	public static final String Alias = "authorization";
	public static final int DEFAULT_TYPE_ID = 1;
	public static int TYPE_ID = DEFAULT_TYPE_ID;
	
	private DoubleLinkedList nonAuthorizedPlayers = new DoubleLinkedList();
	private DoubleLinkedList nonAuthorizedPlayersTemp = new DoubleLinkedList();
	private Object nonAuthorizedPlayersLocker = new Object();
	private DoubleLinkedList updatedClients = new DoubleLinkedList();
	private DoubleLinkedList playersToRemove = new DoubleLinkedList();
	private DoubleLinkedList authorizedPlayers = new DoubleLinkedList();
	private DoubleLinkedList authorizedPlayersTemp = new DoubleLinkedList();
	private Object authorizedPlayersLocker = new Object();
	
	private long timeToReceiveVersion;
	private long timeToReceiveAuthorization;
	
	private boolean needStop = false;
	
	
	public Object getAuthorizedPlayersLocker() {
		return authorizedPlayersLocker;
	}
	
	public DoubleLinkedList getAuthorizedPlayers() {
		return authorizedPlayers;
	}
	
	
	public byte getTypeId() {
		// TODO Auto-generated method stub
		return CommandTypes.NEW_PLAYER.TYPE;
	}

	public void onNewPlayer(GameClient client) {
		throw new RuntimeException("need use: onNewConnection for Authorization to catch new clients");
	}
	
	public void onNewConnection(NettyClient client) {
		synchronized (nonAuthorizedPlayersLocker) {
			AuthorizationInfo info = new AuthorizationInfo(client);
			DoubleLinkedListNode node = new DoubleLinkedListNode(info);
			nonAuthorizedPlayers.addLast(node);
		}
	}

	public void onPlayerDisconnect(GameClient player) {
		//nothin
	}
	
	protected GameCommand createCommand(short commandId) {
		GameCommand command = new GameCommand(getTypeId(), commandId);
		ByteBuf buf = NettyManager.get().getCommandAllocator().buffer();
		command.setData(buf);
		return command;
	}
	
	public boolean isNeedStop() {
		return needStop;
	}
	
	public void setNeedStop(boolean needStop) {
		this.needStop = needStop;
	}
	
	public void update() {
		if (needStop) {
			return;
		}
		
		synchronized (nonAuthorizedPlayersLocker) {
			nonAuthorizedPlayersTemp.removeAll();
			DoubleLinkedList list = nonAuthorizedPlayers;
			nonAuthorizedPlayers = nonAuthorizedPlayersTemp;
			nonAuthorizedPlayersTemp = list;
		}
		
		for (DoubleLinkedListNode i = nonAuthorizedPlayersTemp.getFirst(); i!=null; i=i.next) {
			AuthorizationInfo info = (AuthorizationInfo) i.getPayload();
			info.updateTime(getTimeToReceiveVersion());
			info.state = AuthorizationInfo.WAIT_VERSION;
			info.node = new DoubleLinkedListNode(info);
			updatedClients.addLast(info.node);
		}
		nonAuthorizedPlayersTemp.removeAll();
				
		long time = TimeManager.get().getNow();
		for (DoubleLinkedListNode i = updatedClients.getFirst(); i!=null; i=i.next) {
			AuthorizationInfo info = (AuthorizationInfo) i.getPayload();
			if (info.isTimeout(time)) {
				GameCommand lastCommand = createCommand(CommandTypes.NEW_PLAYER.ACTION_TIMEOUT);
				lastCommand.getData().writeShort(info.state);
				info.client.sendLastCommandAndClose(lastCommand);
				playersToRemove.addLast(new DoubleLinkedListNode(i));
				//TODO: add logs
			} else {
				GameCommand command;
				switch(info.state) {
				case AuthorizationInfo.WAIT_VERSION:
					command = info.client.getFirstReceivedCommand(getTypeId());
					if (command != null) {
						checkVersion(info, command, i);
					}
					break;
				case AuthorizationInfo.WAIT_AUTHORIZATION:
					command = info.client.getFirstReceivedCommand(getTypeId());
					if (command != null) {
						if (command.getId() == CommandTypes.NEW_PLAYER.AUTHORIZE) {
							getAuthorization(info, command, i);
						} else if (command.getId() == CommandTypes.NEW_PLAYER.REGISTER) {
							
						} else {
							//TODO: add logs
							GameCommand lastCommand = createCommand(CommandTypes.NEW_PLAYER.INVALID_COMMAND);
							lastCommand.getData().writeShort(CommandTypes.NEW_PLAYER.AUTHORIZE).writeShort(command.getId());
							info.client.sendLastCommandAndClose(lastCommand);
							playersToRemove.addLast(new DoubleLinkedListNode(i));
						}
					}
					break;
				}
			}
		}
		
		for (DoubleLinkedListNode i = playersToRemove.getFirst(); i!=null; i=i.next) {
			DoubleLinkedListNode removed = (DoubleLinkedListNode) i.getPayload(); 
			updatedClients.remove(removed);
		}
		playersToRemove.removeAll();
	}
		
	
	private RunWithParams<DBRequest> db_loginAndPassword = new RunWithParamsDBRequest() {
		
		@Override
		public void run() {
			//TODO: if param.hasError() -> AUTH_RESULT_ERROR
			GameCommand command;
			AuthorizationInfo info = (AuthorizationInfo) param.getSender();
			if (param.hasError()) {
				String errorMessage;
				if (param.getError()!=null) {
					errorMessage = param.getError().toString();
				} else {
					errorMessage = "Unknown Internal DB error on server side (((";
				}
				command = createCommand(CommandTypes.NEW_PLAYER.AUTHORIZE);
				command.getData().writeShort(CommandTypes.NEW_PLAYER.P_AUTH.AUTH_RESULT_ERROR);
				TextUtils.writeToBuf(errorMessage, command.getData());
				//TODO: check is commands executed async
				info.client.sendLastCommandAndClose(command);
			} else {			
				int result  = (Integer)param.getResult(TypeEmailPassword.rs_pos_result);
				switch(result) {
				case TypeEmailPassword.RESULT_SUCCESS:
					long userId = (Long)param.getResult(TypeEmailPassword.rs_pos_user_id);
					String alias = (String)param.getResult(TypeEmailPassword.rs_pos_user_alias);				
					GamePlayer player = new GamePlayer();
					player.setName(alias);
					player.setId(userId);
					info.player = player;
					
					GameClient client = new GameClient();
					client.setPlayer(player);
					client.setConnection(info.client);
					
					synchronized (authorizedPlayersLocker) {
						authorizedPlayers.addLast(new DoubleLinkedListNode(client));
					}
					command = createCommand(CommandTypes.NEW_PLAYER.AUTHORIZE);
					command.getData().writeShort(CommandTypes.NEW_PLAYER.P_AUTH.AUTH_RESULT_SUCCESS).writeLong(userId);
					//TODO: check is commands executed async 
					info.client.sendCommand(command);
					break;
				case TypeEmailPassword.RESULT_NOT_FOUND:
					command = createCommand(CommandTypes.NEW_PLAYER.AUTHORIZE);
					command.getData().writeShort(CommandTypes.NEW_PLAYER.P_AUTH.AUTH_RESULT_FAIL);
					//TODO: check is commands executed async
					info.client.sendLastCommandAndClose(command);
					break;
				case TypeEmailPassword.RESULT_ERROR:
					String errorMessage = (String)param.getResult(TypeEmailPassword.rs_pos_error);
					command= createCommand(CommandTypes.NEW_PLAYER.AUTHORIZE);
					command.getData().writeShort(CommandTypes.NEW_PLAYER.P_AUTH.AUTH_RESULT_ERROR);
					TextUtils.writeToBuf(errorMessage, command.getData());
					//TODO: check is commands executed async
					info.client.sendLastCommandAndClose(command);
					break;
				}		
			}
			DBManager.get().recycleRequest(param);
		}
	};
	
	private void getAuthorization(AuthorizationInfo info, GameCommand command, DoubleLinkedListNode currentNode) {
		short authorizationType = CommandTypes.NEW_PLAYER.P_AUTH.AUTH_TYPE_INVALID;
		try {
			authorizationType = command.getData().readShort();
		} catch(Throwable ex) {
			command.recycle();
			Log.e(Authorizator.class, "fail to read Authorization type", ex);
			GameCommand lastCommand = createCommand(CommandTypes.NEW_PLAYER.AUTHORIZE);
			lastCommand.getData()
				.writeShort(CommandTypes.NEW_PLAYER.P_AUTH.AUTH_RESULT_ERROR)
				.writeShort(CommandTypes.NEW_PLAYER.P_AUTH.AUTH_ERROR_INCORRECT_MESSAGE_FORMAT);
			//TODO: check is commands executed async
			info.client.sendLastCommandAndClose(lastCommand);
			playersToRemove.addLast(new DoubleLinkedListNode(currentNode));
			return;
		}
		switch (authorizationType) {
		case CommandTypes.NEW_PLAYER.P_AUTH.AUTH_TYPE_LOGIN_AND_PASSWORD:
			try {
				String login = TextUtils.readFromBuf(command.getData());
				String password = TextUtils.readFromBuf(command.getData());
				DBRequest request = DBManager.get().startRequest();
				request.setRequestParser(DBManager.commandFactory().getRequestParser(Requests.Authorization.EmailAndPassword.ID));
				request.putParameter(TypeEmailPassword.rq_pos_email, login);
				request.putParameter(TypeEmailPassword.rq_pos_password, password);
				request.setSender(info);
				request.setOnComplete(db_loginAndPassword);//TODO: add ability to make it possible to cancel if timeout.
				request.setOnExecuted(GameManager.get().getUpdateExecutor());
				DBManager.get().executeAsync(request);
				command.recycle();
			} catch(Throwable ex) {
				command.recycle();
			}
			playersToRemove.addLast(new DoubleLinkedListNode(currentNode));
			break;
		case CommandTypes.NEW_PLAYER.P_AUTH.AUTH_TYPE_COOKIE:
			command.recycle();
			break;
		case CommandTypes.NEW_PLAYER.P_AUTH.AUTH_TYPE_VKONTAKTE_ID:
			command.recycle();
			break;
		case CommandTypes.NEW_PLAYER.P_AUTH.AUTH_TYPE_FACEBOOK_ID:
			command.recycle();
			break;
		default:
			command.recycle();
			break;
		}
	}
	
	private void checkVersion(AuthorizationInfo info, GameCommand command, DoubleLinkedListNode currentNode) {
		if (command.getId() == CommandTypes.NEW_PLAYER.VERSION) {
			try {
				int clientVersion = command.getData().readInt();
				command.recycle();
				
				boolean isCorrect = ComponentManager.get().checkClientVersionSupported(clientVersion);
				GameCommand sendCommand = createCommand(CommandTypes.NEW_PLAYER.VERSION);
				if (isCorrect) {
					boolean canUpdate = ComponentManager.get().cliendCanUpdate(clientVersion);
					if (canUpdate) {
						sendCommand.getData().writeShort(CommandTypes.NEW_PLAYER.P_AUTH.VERSION_CAN_UPDATED);
					} else {
						sendCommand.getData().writeShort(CommandTypes.NEW_PLAYER.P_AUTH.VERSION_LAST);
					}
					info.state = AuthorizationInfo.WAIT_AUTHORIZATION;
					info.updateTime(getTimeToReceiveAuthorization());
					info.version = clientVersion;
					info.client.sendCommand(sendCommand);
				} else {
					sendCommand.getData().writeShort(CommandTypes.NEW_PLAYER.P_AUTH.VERSION_NOT_SUPPORTED);
					//TODO: check is commands executed async
					info.client.sendLastCommandAndClose(sendCommand);
					playersToRemove.addLast(new DoubleLinkedListNode(currentNode));
				}
			} catch(Exception ex) {
				command.recycle();
				Log.e(Authorizator.class, "fail to read Client Version, dicsconnect", ex);
				GameCommand lastCommand = createCommand(CommandTypes.NEW_PLAYER.INVALID_COMMAND);
				lastCommand.getData()
					.writeByte(INVALID_COMMAND_FORMAT)
					.writeShort(CommandTypes.NEW_PLAYER.VERSION);
				//TODO: check is commands executed async
				info.client.sendLastCommandAndClose(lastCommand);
				playersToRemove.addLast(new DoubleLinkedListNode(currentNode));								
			}
		} else {
			//TODO: add logs
			command.recycle();
			GameCommand lastCommand = createCommand(CommandTypes.NEW_PLAYER.INVALID_COMMAND);
			lastCommand.getData()
				.writeByte(INVALID_COMMAND_ID)
				.writeShort(CommandTypes.NEW_PLAYER.VERSION)
				.writeShort(command.getId());
			//TODO: check is commands executed async
			info.client.sendLastCommandAndClose(lastCommand);
			playersToRemove.addLast(new DoubleLinkedListNode(currentNode));
		}
	}
	
	public long getTimeToReceiveAuthorization() {
		return timeToReceiveAuthorization;
	}
	
	public void setTimeToReceiveAuthorization(long timeToReceiveAuthorization) {
		this.timeToReceiveAuthorization = timeToReceiveAuthorization;
	}
	
	public long getTimeToReceiveVersion() {
		return timeToReceiveVersion;
	}
	
	public void setTimeToReceiveVersion(long timeToReceiveVersion) {
		this.timeToReceiveVersion = timeToReceiveVersion;
	}
		
	private static class AuthorizationInfo {
		
		public static final int WAIT_VERSION = 0;
		public static final int WAIT_AUTHORIZATION = 1;
		
		public int state;
		public int version;
		public int authorizationMethod;
		public String auth1;
		public String auth2;
		public String auth3;
		public long userId;
		public long lastActionTime;
		public long timeForNextAction;
		public NettyClient client;
		public GamePlayer player;
		public DoubleLinkedListNode node;
		
		public AuthorizationInfo(NettyClient client) {
			this.client = client;
		}
		
		public void updateTime(long timeForNextAction) {
			this.lastActionTime = TimeManager.get().getNow();
			this.timeForNextAction = timeForNextAction;
		}
		
		public boolean isTimeout(long time) {
			boolean result = (time - lastActionTime > timeForNextAction);
			return result;				
		}
	}
}
