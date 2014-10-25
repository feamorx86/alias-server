package ru.feamor.aliasserver.games;

import org.json.JSONObject;

import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.components.GameManager.Executed;
import ru.feamor.aliasserver.game.GameClient;

public abstract class BaseGame {
	/***
	 * 10 Seconds
	 */
	public static final long DEFAULT_MAX_EXECUTE_TIME = 10 * 1000;
	public abstract byte getTypeId();
	public abstract void onNewPlayer(GameClient player);
	public abstract void onPlayerDisconnect(GameClient player);
	
	private boolean needStop = false;
	private GameManager.Executed executed;
	private long config_maxExecuteTimeout;
	
	public synchronized void askStop() {
		needStop = true;
	}
	
	public synchronized boolean isNeedStop() {
		return needStop;
	}
	
	protected void update() {
		
	}
	
	public void config(JSONObject config) {
		config_maxExecuteTimeout = config.optLong("maxExecuteTime", DEFAULT_MAX_EXECUTE_TIME);
	}
	
	public void start() {
		executed = new GameManager.Executed();
		executed.setGame(this);
		executed.setMaxTime(config_maxExecuteTimeout);

		updateRunnable = new Runnable() {
			
			@Override
			public void run() {
				update();
			}
		};
	}
	
	public void onUpdateTimeout() {
		
	}
	
	public void onError(Object problem) {
		
	}
	
	public void onStop() {
		
	}
	
	public BaseGame() {
		
	}
	
	public GameManager.Executed getExecuted() {
		return executed;
	}
	
	protected Runnable updateRunnable;
	
	public Runnable getUpdate() {
		return updateRunnable;
	}
	
	public void onStarted() {
		
	}
}
