package ru.feamor.aliasserver.components;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Calendar;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.base.RunnableExecutor;
import ru.feamor.aliasserver.base.UpdateThreadController;
import ru.feamor.aliasserver.base.UpdateThreadController.ThreadUpdated;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor;
import ru.feamor.aliasserver.core.ClientInProcessor;
import ru.feamor.aliasserver.core.Component;
import ru.feamor.aliasserver.game.connectors.GameConnectorFactory;
import ru.feamor.aliasserver.game.types.GameTypeCollector;
import ru.feamor.aliasserver.games.Authorizator;
import ru.feamor.aliasserver.games.BaseGame;
import ru.feamor.aliasserver.netty.NettyClient;
import ru.feamor.aliasserver.users.GameClient;
import ru.feamor.aliasserver.users.UsersPool;
import ru.feamor.aliasserver.utils.Log;

public class GameManager extends Component  {
	
	public static final int MIN_UPDATE_INTERVAL = 200;
	
	private GameTypeCollector typeController;	
	private TIntObjectHashMap<BaseGame> activeGames;
	private Authorizator authorizator;
	private GamesFactory gamesFactory;
	private GameConnectorFactory connectorFactory;
	private UpdateThreadController GameLogicExecutor;
	private Thread managerThread;	
	private int minUpdateInterval = MIN_UPDATE_INTERVAL;
	private long lastUpdateTime = 0;
	private UsersPool usersPool;
	
	public GameManager() {
	
	}
	
	public static GameManager get() {
		return (GameManager)Components.gameManager.compenent;
	}
	
	public static void runAsGameLogic(ThreadUpdated executed) {
		get().getThreadController().addUpdateObject(executed);
	}
	
	@Override
	public void create() {
		super.create();
		gamesFactory = new GamesFactory();
		connectorFactory = new GameConnectorFactory();
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
		
//		GameLogicExecutor.addUpdateObject(systemCommandsProcessor);
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
				usersPool.newUser(client);
			}
			authorizator.getAuthorizedPlayers().removeAll();
		}
	}
	
	public UsersPool getUsersPool() {
		return usersPool;
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
		public void onResumed() {
			
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
	
	public GamesFactory getGamesFactory() {
		return gamesFactory;
	}
	
	public GameConnectorFactory getConnectorFactory() {
		return connectorFactory;
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
 