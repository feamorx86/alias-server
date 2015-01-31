package ru.feamor.aliasserver.users;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.sql.Time;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;
import org.json.JSONObject;

import ru.feamor.aliasserver.base.DataObject;
import ru.feamor.aliasserver.base.ObjectCache;
import ru.feamor.aliasserver.base.UpdateThreadController;
import ru.feamor.aliasserver.commands.CommandTypes;
import ru.feamor.aliasserver.commands.CommandTypes.SYSTEM;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor.States;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor.TypeGetGamesFor;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor.TypeUserCanPlayGame;
import ru.feamor.aliasserver.commands.GameCommand;
import ru.feamor.aliasserver.commands.SystemCommandsClient;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor;
import ru.feamor.aliasserver.components.DBManager;
import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.db.DBRequest;
import ru.feamor.aliasserver.db.RunWithParamsDBRequest;
import ru.feamor.aliasserver.db.requests.Requests;
import ru.feamor.aliasserver.game.GameConnector;
import ru.feamor.aliasserver.game.GameConnectorFactory;
import ru.feamor.aliasserver.game.GameTags;
import ru.feamor.aliasserver.game.GameType;
import ru.feamor.aliasserver.games.BaseGame;
import ru.feamor.aliasserver.utils.RunWithParams;
import ru.feamor.aliasserver.utils.TextUtils;

public class UsersPool {
	
	/**
	 * Storage of all on-line clients
	 */
	private TLongObjectHashMap<UserInPool> allClients = new TLongObjectHashMap<UserInPool>();
	
	private TIntObjectHashMap<UserQueue> activeTypes;
	private DoubleLinkedList newUsers, newUsersTemp;
	private Object newUsersLocker;
	
	private DoubleLinkedList newGameRequests, newGameRequestsTemp;
	private Object newGameRequestsLocker;
	
	
	private SystemCommandsProcessor systemCommandsProcessor;
	
//	private UpdateThreadController connectorsThreads;
	private ConnectorsLoader connectorsLoader;
	private GameConnectorFactory connectorFactory;
	
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
	
	public static class ConnectorsLoader implements UpdateThreadController.ThreadPendingUpdated {

		private DoubleLinkedList newConnectors, newConnectorsTemp;
		private Object newConnectorsLocker;

		
		private DoubleLinkedListNode node;
		private boolean needStop;
		private boolean pending;
		private long lastUpdateTime;
		private long minUpdateTime = 300;
		private long nextUpdateTime = 0;
				
		public ConnectorsLoader() {
			lastUpdateTime = Calendar.getInstance().getTimeInMillis();
			pending = false;
			needStop = false;
		}
		
		public void loadGameType(UserQueue userQueue) {
			synchronized (newConnectorsLocker) {
				newConnectors.addFirst(new DoubleLinkedListNode(userQueue));
			}
		}
		
		@Override
		public void setUpdateNode(DoubleLinkedListNode node) {
			this.node = node;
		}

		@Override
		public DoubleLinkedListNode getUpdateNode() {
			return node;
		}

		@Override
		public boolean needStopUpdate() {
			return needStop;
		}
		
		private boolean checkIsItTimeToUpdate() {
			boolean needUpdate = true;
			
			pending = false;
			long now = Calendar.getInstance().getTimeInMillis();
			long delta = now - lastUpdateTime;
			lastUpdateTime = now;
			
			if (delta < minUpdateTime) {
				nextUpdateTime = now + delta;
				pending = true;
				needUpdate = false;
			}
			
			return needUpdate;
		}

		@Override
		public void update() throws InterruptedException {
			if (!needStop && checkIsItTimeToUpdate()) {
				checkNewRequests();
			}
		}

		private void checkNewRequests() {
			synchronized (newConnectorsLocker) {
				if (newConnectors.size() > 0) {
					newConnectorsTemp.removeAll();
					DoubleLinkedList temp = newConnectors;
					newConnectors = newConnectorsTemp;
					newConnectorsTemp = temp;
				}
			}
			
			for(DoubleLinkedListNode i = newConnectorsTemp.getFirst(); i!=null; i = i.next) {
				
				- вот тут нестыковочка выходит - нужно перепроверить чтобы везде, в подобных местах был Temp.!!!
				- дописать запроса данных
				- создание коннектора 
				- добавление и включение коннектора
				- тест дл€ коннектора
				
				UserQueue userQueue = (UserQueue) i.getPayload();
				RequestedGameConnector requestedGameConnector = new RequestedGameConnector();  
				DBRequest request = DBManager.get().startRequest();
				request.setRequestParser(DBManager.commandFactory().getRequestParser(Requests.SystemCommands.CheckcGameAvalableForUser.ID));
				request.putParameter(TypeUserCanPlayGame.rq_pos_user_id, userId);
				request.putParameter(TypeUserCanPlayGame.rq_pos_game_type_id, selectedGameId);
				request.putParameter(TypeGetGamesFor.rq_request_id_position, client.nextRequestId());
				request.setSender(requestedGameConnector);
				request.setOnComplete(db_getGameTypeInfo);//TODO: add ability to make it possible to cancel if timeout.
				request.setOnExecuted(getExecutor());
				DBManager.get().executeAsync(request);					
			}
		}

		@Override
		public void onProblem(Integer problemType, Object problem) {
			
		}

		@Override
		public void wasError() {
			
		}

		@Override
		public boolean isPending() {
			return pending;
		}

		@Override
		public long getExecuteTime() {
			return nextUpdateTime;
		}
		
		private RunWithParams<DBRequest> db_getGameTypeInfo = new RunWithParamsDBRequest() {
			@Override
			public void run() {
				RequestedGameConnector request = (RequestedGameConnector) param.getSender();
				
			}
		};
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
//					- добавить обюратную св€зь от прцессоров к клиенту, чтобы при resume клиент мог знать куда он восстанавливаетс€
//					- добавить в процессор возмнжность не только добавл€ть Ќовых клиентов но и ¬осстанавливать старых, а дл€ клиента 
//						и добавить дл€ процессоров не поддерживающих возможнось восстановлени€ - возможность сброса параметров клиента
//						если он ранее существовал.
//					- в SystemClientProcessor-е доделать обработку комманды select game
//					- начинать добавл€ть обработку обработку создани€ игр.
//					- подумать над многопоточьностью межу списком всех клиентов \ новыми клиентами \ таймером бездействующих клиентов \ списком клиентов дл€ подключени€ к игре .
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

	public static class SimpleCoutGameConnector extends GameConnector {
		public static final int STATE_NEW = 0;
		public static final int STATE_WAIT_USERS = 1;
		
		private int usersForGame;
		private int state;
		
		public SimpleCoutGameConnector(GameType gameType, UserQueue queue) {
			super(gameType, queue);
			state = STATE_NEW;
		}
		
		@Override
		public void update() throws InterruptedException {
			super.update();
			switch (state) {
				case STATE_NEW:
					firstStart();
					break;
				case STATE_WAIT_USERS:
					checkUsers();
					break;
			}
		}
				
		private void checkUsers() {
			DoubleLinkedList players = new DoubleLinkedList();
			synchronized (queue.users) {
				for (int i=0; i<usersForGame; i++) {
					DoubleLinkedListNode node = queue.users.getFirst();  
					queue.users.remove(node);
					players.addLast(node);
				}
			}
			
			if (queue.users.size() >= usersForGame) {
				BaseGame game = ((GameType.GameTypeWithFixedPlayersCount)gameType).createGame(players);
				
//				ƒобавить поведение сервера и клиента на ситуацию - ќшибка создани€ коннектора \ ошибка создани€ игры. 
//				добавить проверку на возможность пользовател€ играть в игру заданного типа.
//				ƒобавить дл€ пользовател€ "подключение к игре отколонено"
			}
		}

		private void firstStart() {
			if (gameType instanceof GameType.GameTypeWithFixedPlayersCount) {
				usersForGame = ((GameType.GameTypeWithFixedPlayersCount) gameType).getPlayersCount();
				
				if (usersForGame <= 0) throw new RuntimeException("users For game can`t be <= 0!");
				state = STATE_WAIT_USERS;
			} else {
				throw new RuntimeException("Incorrect Game type, Game type must be inherior of GameTypeWithFixedPlayersCount");
			}
		}
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
		
	}
}
