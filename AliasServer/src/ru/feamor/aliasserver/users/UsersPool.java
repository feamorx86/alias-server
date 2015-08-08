package ru.feamor.aliasserver.users;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;
import org.json.JSONObject;

import ru.feamor.aliasserver.base.UpdateThreadController;
import ru.feamor.aliasserver.commands.CommandTypes.SYSTEM;
import ru.feamor.aliasserver.commands.GameCommand;
import ru.feamor.aliasserver.commands.SystemCommandsClient;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor.TypeGetGamesFor;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor.TypeUserCanPlayGame;
import ru.feamor.aliasserver.components.DBManager;
import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.db.DBRequest;
import ru.feamor.aliasserver.db.RunWithParamsDBRequest;
import ru.feamor.aliasserver.db.requests.Requests;
import ru.feamor.aliasserver.game.GameTags;
import ru.feamor.aliasserver.game.GameType;
import ru.feamor.aliasserver.game.connectors.ConnectorsLoader;
import ru.feamor.aliasserver.game.connectors.GameConnector;
import ru.feamor.aliasserver.game.connectors.GameConnectorFactory;
import ru.feamor.aliasserver.games.BaseGame;
import ru.feamor.aliasserver.utils.RunWithParams;
import ru.feamor.aliasserver.utils.TextUtils;

public class UsersPool {
	
	/**
	 * Storage of all on-line clients
	 */
	private TLongObjectHashMap<UserInPool> allClients = new TLongObjectHashMap<UserInPool>();
	
	private TIntObjectHashMap<UserQueue> activeTypes;
		
	private DoubleLinkedList newGameRequests, newGameRequestsTemp;
	private Object newGameRequestsLocker;
	
	
	private SystemCommandsProcessor systemCommandsProcessor;
	
//	private UpdateThreadController connectorsThreads;
	private ConnectorsLoader connectorsLoader;
	private GameConnectorFactory connectorFactory;
	
	private DoubleLinkedList newUsers;
	private DoubleLinkedList newUsersTemp;
	private Object newUsersLocker;
	
	public UsersPool() {
		activeTypes = new TIntObjectHashMap<>();
		newUsers = new DoubleLinkedList();
		newUsersTemp = new DoubleLinkedList();
		newUsersLocker = new Object();
		
		newGameRequests = new DoubleLinkedList();
		newGameRequestsTemp = new DoubleLinkedList();
		newGameRequestsLocker = new Object();
		
		systemCommandsProcessor = new SystemCommandsProcessor();
		connectorsLoader = new ConnectorsLoader();
//		connectorsThreads = new UpdateThreadController("GameConnectors");
	}
	
	
	
	public static class RequestedGameConnector {
		public int gameTypeId;
		private boolean success;
		public int gameTypeClassId;
		public JSONObject config;		
		public UserQueue userQueue;
	}
	
	public void start() {
		GameManager.runAsGameLogic(systemCommandsProcessor);
		GameManager.runAsGameLogic(connectorsLoader);
	}
		
	public void update() {
		смотреть где чего и как вызывается update, добавить его выполнение, смотреть что дальше происходит с пользователем после запроса игры
		
		updateLoadingConnectors();
		updateNewUsers();		
		updateConnectors();
		
	}
	
	private void updateConnectors() {
		// TODO Auto-generated method stub	
	}
	
	public static final byte ADD_USER_INVALID_ARGUMENTS = 0;
	public static final byte ADD_USER_ALREADY_EXIST = 1;
	public static final byte ADD_USER_NEW_ADDED = 2;
	
	
	public void newUser(GameClient client) {
		synchronized (newUsersLocker) {
			newUsers.addLast(new DoubleLinkedListNode(client));
		}
	}
	
	public void newGameRequest(SystemCommandsClient client, int gameId) {
		synchronized (newGameRequestsLocker) {
			newGameRequests.addLast(new DoubleLinkedListNode(new GameRequest(gameId, client)));
		}
	}
	
	private void updateGameRequests() {
		synchronized (newGameRequestsLocker) {
			if (newGameRequests.size() > 0) {
				newGameRequestsTemp.removeAll();
				DoubleLinkedList temp = newGameRequests;
				newGameRequests = newGameRequestsTemp;
				newGameRequestsTemp = temp;
			}
		}
		
		for(DoubleLinkedListNode i = newGameRequests.getFirst(); i!=null; i = i.next) {
			GameRequest request = (GameRequest) i.getPayload();
			
			UserQueue queue = activeTypes.get(request.gameTypeId);
			if (queue == null) {
				queue = new UserQueue(request.gameTypeId);
			}
			queue.addClient(request.client.getGameClient());			
		}
	}
		
	
	private void updateNewUsers() {
		synchronized (newUsersLocker) {
			if (newUsers.size() > 0) {
				newUsersTemp.removeAll();
				DoubleLinkedList temp = newUsers;
				newUsers = newUsersTemp;
				newUsersTemp = temp;
			}
		}
				
		for(DoubleLinkedListNode i = newUsersTemp.getFirst(); i!=null; i = i.next) {
			GameClient client = (GameClient) i.getPayload();
			
			if (client.getPlayer() == null || client.getPlayer().getId() < 0) {
				if (client.getConnection()!=null) {
					GameCommand lastCommand = client.createCommand(SYSTEM.TYPE, SYSTEM.USER_ADDED);
					lastCommand.getData().writeShort(SYSTEM.UserAdded.PROBLEM);
					TextUtils.writeToBuf("Add new user to game logic problems: no `Player` or invalid client id < 0.",	lastCommand.getData());
					client.getConnection().sendLastCommandAndClose(lastCommand);
				}
			} else {
				boolean isResumed = false;
				long userId = client.getPlayer().getId();
				UserInPool oldUser;
				synchronized (allClients) {
					oldUser = allClients.get(userId);
					if (oldUser == null) {
						isResumed = true;
					} else {
						UserInPool userInPool = new UserInPool(client);
						allClients.put(userId, userInPool);
					}
				}
				
				if (isResumed) {
					GameCommand command = client.createCommand(SYSTEM.TYPE, SYSTEM.USER_ADDED);
					command.getData().writeShort(SYSTEM.UserAdded.RESUMED);
					client.getConnection().sendCommand(command);
					systemCommandsProcessor.addResuedClient(new SystemCommandsClient(client));
//					- добавить обюратную связь от прцессоров к клиенту, чтобы при resume клиент мог знать куда он восстанавливается
//					- добавить в процессор возмнжность не только добавлять Новых клиентов но и Восстанавливать старых, а для клиента 
//						и добавить для процессоров не поддерживающих возможнось восстановления - возможность сброса параметров клиента
//						если он ранее существовал.
//					- в SystemClientProcessor-е доделать обработку комманды select game
//					- начинать добавлять обработку обработку создания игр.
//					- подумать над многопоточьностью межу списком всех клиентов \ новыми клиентами \ таймером бездействующих клиентов \ списком клиентов для подключения к игре .
				} else {
					GameCommand command = client.createCommand(SYSTEM.TYPE, SYSTEM.USER_ADDED);
					command.getData().writeShort(SYSTEM.UserAdded.SUCCESS);
					client.getConnection().sendCommand(command);
					systemCommandsProcessor.addNewClient(new SystemCommandsClient(client));
				}
			}
		}
	}
	
	private Timer checkClientsTimer;
	public static final long DEFAULT_CHECK_CLIENTS_PERIOD = 30 * 1000;
	private long checkClientsPeriod = DEFAULT_CHECK_CLIENTS_PERIOD;
	
	public void startCheckClientsTimer() {
		stopCheckClientsTimer();
		
		checkClientsTimer = new Timer("CheckCkientsTimer");
		checkClientsTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				checkClients();				
			}
		}, checkClientsPeriod, checkClientsPeriod);
	}
	
	public void stopCheckClientsTimer() {
		if (checkClientsTimer!=null) {
			checkClientsTimer.cancel();
			checkClientsTimer = null;
		}
	}

	private void updateLoadingConnectors() {
		
	}
	
	private void checkClients() {
		
	}
	
	public void startUpdate() {
		// TODO Auto-generated method stub
		
	}

	
	
	public static class GameRequest {
		public int gameTypeId;
		public SystemCommandsClient client;
		
		public GameRequest(int gameTypeId, SystemCommandsClient client) {
			super();
			this.gameTypeId = gameTypeId;
			this.client = client;
		}
	}
	
	public static class UserQueue {
		private int gameTypeId;
		private DoubleLinkedList users;
		private long createTime;
		private long lastCreatedGameTime;
		private GameConnector connector;
		private boolean isLoaded;
		
		public UserQueue(int gameTypeId) {
			this.gameTypeId = gameTypeId;
			users = new DoubleLinkedList();
			createTime = Calendar.getInstance().getTimeInMillis();
			connector = null;
			isLoaded = false;
		}
		
		public int getGameTypeId() {
			return gameTypeId;
		}
		
		public void addClient(GameClient client) {
			DoubleLinkedListNode node = new DoubleLinkedListNode(client);
			client.putTag(GameTags.TAG_GAME_CONNECTOR_QEUE_NODE, node);
			synchronized (users) {
				users.addLast(node);
			}
			
		}
		
		public DoubleLinkedList getUsers() {
			return users;
		}
		
	}
}
