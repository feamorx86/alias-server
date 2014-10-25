package ru.feamor.aliasserver.components;

import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.Future;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.concurrent.Callable;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;
import org.apache.logging.log4j.core.util.KeyValuePair;

import ru.feamor.aliasserver.core.Component;
import ru.feamor.aliasserver.game.GameClient;
import ru.feamor.aliasserver.game.GameTags;
import ru.feamor.aliasserver.game.types.GameTypeCollector;
import ru.feamor.aliasserver.games.BaseGame;
import ru.feamor.aliasserver.utils.Log;

public class GameManager extends Component  {
	
	public static final int DEFAULT_MAX_GAME_LOGIC_THREADS =5;
	
	private GameTypeCollector typeController;	
	
	private DoubleLinkedList authorizedPlayers;
	private DoubleLinkedList newPlayers;
	
	private HashMap<Integer, BaseGame> activeGames;
	private DefaultEventExecutorGroup gameLogicEventLoop;
	private int config_maxGameLogicThreads = DEFAULT_MAX_GAME_LOGIC_THREADS;
	private Object playersLocker = new Object();
	private Thread managerThread;
	
	public GameManager() {
		
	}
	
	@Override
	public void create() {
		super.create();
		typeController = new GameTypeCollector();
		authorizedPlayers = new DoubleLinkedList();
		newPlayers = new DoubleLinkedList();
		activeGames = new HashMap<Integer, BaseGame>();
	}
	
	private boolean running = false;
	public synchronized boolean isRunning() {
		return running;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		gameLogicEventLoop = new DefaultEventExecutorGroup(config_maxGameLogicThreads);
		managerThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				synchronized(GameManager.this) {
					running = true;
				}
				updateLoop();
				synchronized(GameManager.this) {
					running = false;
				}
			}
		}, "GameManagerThread");
		managerThread.start();
	}
	
	private void updateLoop() {
		Log.i(GameManager.class, "Start game logic");
		while (!Thread.interrupted()) {
			long startTime = TimeManager.get().getNow();
			updateNewPlayers();
			updateUserPoll();
			updateGameLogic();
			long stopTime =  TimeManager.get().getNow();
			long delta = stopTime - startTime;
			//TODO: add better time managment
			if (delta < confing_minUpdateTime) {
				try {
					Thread.sleep(confing_minUpdateTime - delta);
				} catch(InterruptedException ex) {
					break;
				}
			}
		}
	}
	
	public void stop() {
		managerThread.interrupt();
	}
	
	public void addNewPlayer(GameClient gameClient) {
		synchronized (playersLocker) {
			DoubleLinkedListNode clientNode = new DoubleLinkedListNode(gameClient);
			gameClient.putTag(GameTags.TAG_GAME_CLIENT_NEW_PLAYER_NODE, clientNode);
			newPlayers.addLast(clientNode);	
		}		
	}
	
	public void updateNewPlayers() {
		//update disconnected users
		//update timeout of users
		//add new users
		//check user commands
			//process authorization
			//move user to authorized poll
	}
	
	public void updateUserPoll() {
		//check user timeout (long seat on server 3h+)
		//check user commands
			//process user commands
				// - change user info
				// - start game
				// - resume game
				// - ...
	}
	
	private DoubleLinkedList executedUpdates = new DoubleLinkedList();
	
	private DoubleLinkedList executedForRemove = new DoubleLinkedList();
	private DoubleLinkedList completeExecuted = new DoubleLinkedList();

	private DoubleLinkedList newGames = new DoubleLinkedList();
	private DoubleLinkedList newGamesTemp = new DoubleLinkedList();
	private Object newGamesLocker = new Object();
	
	private DoubleLinkedList stoppingGames = new DoubleLinkedList();
	private DoubleLinkedList stoppingGamesTemp = new DoubleLinkedList();
	private Object stoppingGamesLocker = new Object();
	
		
	public void stopGame(BaseGame game) {
		synchronized(stoppingGamesLocker) {
			stoppingGames.addLast(new DoubleLinkedListNode(game));
		}
	}
		
	public void addGame(BaseGame game) {
		synchronized(newGamesLocker) {
			newGames.addLast(new DoubleLinkedListNode(game));
		}
	}
		
	public void updateGameLogic() {
//		long startTime = TimeManager.get().getNow();
		long now;
		//update new games
		synchronized(newGamesLocker) {
			DoubleLinkedList temp = newGamesTemp;
			newGamesTemp = newGames;
			newGames = temp;
		}
		
		for(DoubleLinkedListNode i = newGamesTemp.getFirst(); i!=null; i=i.next) {
			BaseGame game = (BaseGame) i.getPayload();
			DoubleLinkedListNode node = new DoubleLinkedListNode(game.getExecuted());
			completeExecuted.addLast(node);
			try {				
				game.onStarted();
			} catch(Throwable ex) {
				//TODO: add handle errors
				game.onError(ex);
				stopGame(game);
			}
		}
		newGamesTemp.removeAll();
		
		//TODO: add check of time
		
		//update stopped games
		synchronized(stoppingGamesLocker) {
			DoubleLinkedList temp = stoppingGamesTemp;
			stoppingGamesTemp = stoppingGames;
			stoppingGames = temp;
		}
		
		for(DoubleLinkedListNode i = stoppingGamesTemp.getFirst(); i!=null; i=i.next) {
			BaseGame game = (BaseGame) i.getPayload();
			game.getExecuted().needStopAfterExecute = true;
		}
		stoppingGamesTemp.removeAll();
		
		//TODO: add check of time
		
		//check if game time is out
		now = TimeManager.get().getNow();
		for(DoubleLinkedListNode i = executedUpdates.getFirst(); i!=null; i=i.next) {
			Executed exec = ((BaseGame) i.getPayload()).getExecuted();
			if (exec.future.isDone()) {
				completeExecuted.addLast(new DoubleLinkedListNode(exec));
				executedForRemove.addLast(new DoubleLinkedListNode(i));
			} else {
				if (exec.isTimeout(now)) {
					exec.future.cancel(true);
				}
			}
		}
		//Remove already executed
		for(DoubleLinkedListNode i = executedForRemove.getFirst(); i!=null; i=i.next) {
			DoubleLinkedListNode node = (DoubleLinkedListNode) i.getPayload();
			executedUpdates.remove(node);
		}
		executedForRemove.removeAll();
		
		//Add new updates
		for(DoubleLinkedListNode i = completeExecuted.getFirst(); i!=null; i=i.next) {
			BaseGame game = (BaseGame) i.getPayload();
			if (game.getExecuted().needStopAfterExecute) {
				try {
					game.onStop();
				} catch(Throwable ex) {
					game.onError(ex);
				}
				game.getExecuted().needStopAfterExecute = false;
			} else {
				Future<?> future = gameLogicEventLoop.submit(game.getUpdate());
				if (future!=null) {
					game.getExecuted().setStart(TimeManager.get().getNow());
					game.getExecuted().setFuture(future);
					executedUpdates.addLast(new DoubleLinkedListNode(game));
				} else {
					//TODO: need add handle
				}
			}
		}
		completeExecuted.removeAll();//TODO: может тут стоит добавить запуск отложенных тасков, на которые не хватило потоков 
		
		//TODO: add better time managment
	}
	
	public static final long MIN_UPDATE_TIME = 500;   
	private long confing_minUpdateTime = MIN_UPDATE_TIME;
		
	public GameTypeCollector getTypeController() {
		return typeController;
	}
	
	public void setConfig_maxGameLogicThreads(int config_maxGameLogicThreads) {
		this.config_maxGameLogicThreads = config_maxGameLogicThreads;
	}
	
	public static class Executed {
		private BaseGame game;
		private long start;
		private long maxTime;
		private Future<?> future;		
		private boolean needStopAfterExecute;
		
		public boolean isTimeout(long now) {
			boolean result = now - start > maxTime;
			return result;
		}
		
		public void setFuture(Future<?> future) {
			this.future = future;
		}
		
		public Future<?> getFuture() {
			return future;
		}
		
		public void setGame(BaseGame game) {
			this.game = game;
		}
		
		public BaseGame getGame() {
			return game;
		}
		
		public void setMaxTime(long maxTime) {
			this.maxTime = maxTime;
		}
		
		public long getMaxTime() {
			return maxTime;
		}
		
		public void setStart(long start) {
			this.start = start;
		}
		
		public long getStart() {
			return start;
		}
	}
}
