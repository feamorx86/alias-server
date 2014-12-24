package ru.feamor.aliasserver.games;

import io.netty.buffer.ByteBuf;

import org.apache.jcs.utils.struct.DoubleLinkedListNode;
import org.json.JSONObject;

import ru.feamor.aliasserver.commands.GameCommand;
import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.components.NettyManager;
import ru.feamor.aliasserver.components.GameManager.Executed;
import ru.feamor.aliasserver.game.GameClient;
import ru.feamor.aliasserver.game.GameType;

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
	
	private int id;
	
	protected GameType gameType;
	
	public synchronized void askStop() {
		needStop = true;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public synchronized boolean isNeedStop() {
		return needStop;
	}
	
	public void setGameType(GameType gameType) {
		this.gameType = gameType;
	}
	
	public GameType getGameType() {
		return gameType;
	}
	
	protected void update() {
		
	}
	
//	public void config(JSONObject config) {
//		config_maxExecuteTimeout = config.optLong("maxExecuteTime", DEFAULT_MAX_EXECUTE_TIME);
//	}
	
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
	
	protected GameCommand createCommand(short commandId) {
		GameCommand command = new GameCommand(getTypeId(), commandId);
		ByteBuf buf = NettyManager.get().getCommandAllocator().buffer();
		command.setData(buf);
		return command;
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
	
	public void doUpdate() {
		getUpdate().run();
	}
	
	public void onStarted() {
		
	}
	
	protected DoubleLinkedListNode myUpdateNode = new DoubleLinkedListNode(this);
	
	public DoubleLinkedListNode getMyUpdateNode() {
		return myUpdateNode;
	}
}
