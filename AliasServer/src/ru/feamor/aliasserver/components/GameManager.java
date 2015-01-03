package ru.feamor.aliasserver.components;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Calendar;

import gnu.trove.map.hash.TIntObjectHashMap;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;
import org.json.JSONObject;

import ru.feamor.aliasserver.base.DataObject;
import ru.feamor.aliasserver.base.RunnableExecutor;
import ru.feamor.aliasserver.base.UpdateThreadController;
import ru.feamor.aliasserver.base.WithRequestId;
import ru.feamor.aliasserver.commands.CommandTypes;
import ru.feamor.aliasserver.commands.CommandTypes.SYSTEM;
import ru.feamor.aliasserver.commands.GameCommand;
import ru.feamor.aliasserver.commands.GameCommandHolder;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor;
import ru.feamor.aliasserver.core.ClientInProcessor;
import ru.feamor.aliasserver.core.ClientsProcessor;
import ru.feamor.aliasserver.core.Component;
import ru.feamor.aliasserver.db.DBRequest;
import ru.feamor.aliasserver.db.RunWithParamsDBRequest;
import ru.feamor.aliasserver.db.DBRequest.RequestExecutor;
import ru.feamor.aliasserver.db.requests.Requests;
import ru.feamor.aliasserver.game.GameClient;
import ru.feamor.aliasserver.game.GameTags;
import ru.feamor.aliasserver.game.UsersPool;
import ru.feamor.aliasserver.game.models.GameTypeModel;
import ru.feamor.aliasserver.game.types.GameTypeCollector;
import ru.feamor.aliasserver.games.Authorizator;
import ru.feamor.aliasserver.games.BaseGame;
import ru.feamor.aliasserver.games.Authorizator.TypeEmailPassword;
import ru.feamor.aliasserver.netty.NettyClient;
import ru.feamor.aliasserver.utils.Log;
import ru.feamor.aliasserver.utils.RunWithParams;
import ru.feamor.aliasserver.utils.TextUtils;

public class GameManager extends Component  {
	
	public static final int MIN_UPDATE_INTERVAL = 200;
	
	private GameTypeCollector typeController;	
	private TIntObjectHashMap<BaseGame> activeGames;
	private Authorizator authorizator;
	private GamesFactory gamesFactory = new GamesFactory();
	private UpdateThreadController GameLogicExecutor;
	private Thread managerThread;	
	private int minUpdateInterval = MIN_UPDATE_INTERVAL;
	private long lastUpdateTime = 0;
	private UsersPool usersPool;
	private SystemCommandsProcessor systemCommandsProcessor = new SystemCommandsProcessor();
	
	public GameManager() {
	
	}
	
	public static GameManager get() {
		return (GameManager)Components.gameManager.compenent;
	}
	
	@Override
	public void create() {
		super.create();
		gamesFactory = new GamesFactory();
		typeController = new GameTypeCollector();
		activeGames = new TIntObjectHashMap<BaseGame>();
		GameLogicExecutor = new UpdateThreadController("GameLogic");
		authorizator = new Authorizator();
		usersPool = new UsersPool();
	}
		
	@Override
	public void onStart() {
		super.onStart();
		GameLogicExecutor.startController();
		usersPool.startUpdate();
		managerThread = new Thread(updateRunnable, "GameManagerThread");
		managerThread.start();
		
		GameLogicExecutor.addUpdateObject(systemCommandsProcessor);
	}
	
	private DoubleLinkedList runInUpdate = new DoubleLinkedList();
	private DoubleLinkedList runInUpdateTemp = new DoubleLinkedList();
	private Object runInUpdateLocker = new Object();
	
	private Runnable updateRunnable = new Runnable() {
		
		@Override
		public void run() {
			Log.i(GameManager.class, "Start game logic");
			while (!Thread.interrupted()) {
				updateNewPlayers();
				executeUpdateRunnables();
//				updateUserPoll();
				long now = Calendar.getInstance().getTimeInMillis();
				if (now - lastUpdateTime < getMinUpdateInterval()) {
					try {
						Thread.sleep(getMinUpdateInterval());
					} catch (InterruptedException iex) {
						return;
					}
					lastUpdateTime = Calendar.getInstance().getTimeInMillis();
				} else {
					lastUpdateTime = now;	
				}
			}
		}
	};
	
	private void executeUpdateRunnables() {
		synchronized (runInUpdateLocker) {
			runInUpdateTemp.removeAll();
			DoubleLinkedList temp = runInUpdateTemp;
			runInUpdateTemp = runInUpdate;
			runInUpdate = temp;
		}
		
		for(DoubleLinkedListNode i = runInUpdateTemp.getFirst(); i!=null; i=i.next) {
			Runnable r = (Runnable) i.getPayload();
			try {
				r.run();
			} catch(Throwable ex) {
				if (ex instanceof InterruptedException) {
					throw ex;
				} else {
					Log.e(GameManager.class, "fail to run updates", ex);
				}
			}
		}
	}
	
	private RunnableExecutor updateExecutor = new RunnableExecutor() {
		
		@Override
		public void executeRunnable(Runnable r) {
			executeInUpdate(r);
		}
	};
	
	public RunnableExecutor getUpdateExecutor() {
		return updateExecutor;
	}
	
	public void executeInUpdate(Runnable r) {
		synchronized (runInUpdateLocker) {
			runInUpdate.addFirst(new DoubleLinkedListNode(r));
		}
	}
			
	public void stop() {
		authorizator.setNeedStop(true);
		managerThread.interrupt();
		GameLogicExecutor.stopController();
	}
	
	public void addNewPlayer(NettyClient gameClient) {
		authorizator.onNewConnection(gameClient);
	}
	
	public void updateNewPlayers() {
		authorizator.update();
		synchronized (authorizator.getAuthorizedPlayersLocker()) {
			for (DoubleLinkedListNode i = authorizator.getAuthorizedPlayers().getFirst(); i!=null; i=i.next) {
				GameClient client = (GameClient) i.getPayload();
				
				ClientInUsersPool userInPool = new ClientInUsersPool();

				userInPool.clinet = client;
				userInPool.addedAt = Calendar.getInstance().getTimeInMillis();
				userInPool.processprNode = new DoubleLinkedListNode(userInPool);
				userInPool.lastActivity = userInPool.addedAt;
				systemCommandsProcessor.addClient(userInPool);
			}
			authorizator.getAuthorizedPlayers().removeAll();
		}
	}
	
	
	public static class ClientInUsersPool implements ClientInProcessor {
		GameClient clinet = null;
		long addedAt = 0;
		DoubleLinkedListNode processprNode = new DoubleLinkedListNode(this);
		boolean inGame = false;
		boolean isStopped = false;
		
		private int state;
		
		long lastActivity = 0;

		@Override
		public DoubleLinkedListNode getProcessorNode() {
			return processprNode; 
		}

		@Override
		public GameClient getGameClient() {
			return clinet;
		}

		@Override
		public void onAdded() {
			
		}

		@Override
		public void onRemoved() {
			
		}
		
		@Override
		public int getState() {
			return state;
		}
		
		@Override
		public void setState(int newState) {
			state = newState;
		}
	}
	
//	public void updateUserPoll() {
//		//¬ажно! чтобы колекци€ не измен€лась одновременно с перебором
//		
//		for (DoubleLinkedListNode i = playersPoll.getFirst(); i!=null; i=i.next) {
//			ClientInUsersPool userInPool = (ClientInUsersPool) i.getPayload();
//			if (userInPool.isStopped) {
//				userInPool.clinet.getConnection().close();
//				DoubleLinkedListNode next = i.next;
//				playersPoll.remove(i);
//				i = next;
//			} else {
//				if (userInPool.inGame) {
//					//game should check client
//				} else {
//					//test on new SYSTEM commands
//					GameCommand command = userInPool.clinet.getConnection().getFirstReceivedCommand(CommandTypes.SYSTEM.TYPE);
//					if (command!=null) {
//						userInPool.lastActivity = Calendar.getInstance().getTimeInMillis();
//						processClientCommand(userInPool, command);
//					} else {
//						//check client timeout
//						long now = Calendar.getInstance().getTimeInMillis();
//						if (now - userInPool.lastActivity > 60 * 1000) {
//							userInPool.isStopped = true;
//						}
//					}
//				}
//			}
//		}
//	}
		
//	private void processClientCommand(ClientInUsersPool userInPool, GameCommand command) {
//		
//	}

	public GameTypeCollector getTypeController() {
		return typeController;
	}
	
	private static int lastGameId = 0;
	
	private synchronized int generateGameId() {
		int result = 0;
		boolean found = false;
		int iteration = Integer.MIN_VALUE;
		while (!found) {
			if (lastGameId + 1 >= Integer.MAX_VALUE) {
				lastGameId = Integer.MIN_VALUE;
			}
			
			if (!activeGames.contains(lastGameId)) {
				found = true;
				result = lastGameId;
			} else {
				iteration++;
				if (iteration+1 >=Integer.MAX_VALUE) {
					Log.e(GameManager.class, "WTF!!!!!  ончились идентификаторы дл€ игр Ѕл€€€€! »гр больше 4 млрд!!");
					found = true;
				}
			}
			
		}		
		return result;
	}

	public BaseGame startGame(int typeId, Object params) {
		BaseGame game = gamesFactory.createGame(typeId, params);
		if (game != null) {
			int gameId = generateGameId();
			game.setId(gameId);
			activeGames.put(gameId, game);
			GameLogicExecutor.addUpdateObject(game);
		}
		return game;
	}
	
	public GamesFactory getGamesFactory() {
		return gamesFactory;
	}
	
	public UpdateThreadController getThreadController() {
		return GameLogicExecutor;
	}
	
	public Authorizator getAuthorizator() {
		return authorizator;
	}
	
	public int getMinUpdateInterval() {
		return minUpdateInterval;
	}
}
 