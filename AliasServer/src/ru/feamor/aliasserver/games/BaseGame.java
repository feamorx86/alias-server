package ru.feamor.aliasserver.games;

import java.util.Calendar;

import io.netty.buffer.ByteBuf;

import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.base.UpdateThreadController.ExecutionProblems;
import ru.feamor.aliasserver.base.UpdateThreadController.ThreadUpdated;
import ru.feamor.aliasserver.commands.GameCommand;
import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.components.NettyManager;
import ru.feamor.aliasserver.game.GameClient;
import ru.feamor.aliasserver.game.GameType;

public abstract class BaseGame implements ThreadUpdated {
	/***
	 * 10 Seconds
	 */
	public static final long DEFAULT_MAX_EXECUTE_TIME = 10 * 1000;
	
	protected boolean needStop = false;
	protected DoubleLinkedListNode myUpdateNode = new DoubleLinkedListNode(this);
	private int id;
	protected GameType gameType;
	protected boolean initialized = false;
	private long lastUpdateTime = 0;
	
	public BaseGame() {
		
	}
	
	public abstract byte getTypeId();
	public abstract void onNewPlayer(GameClient player);
	public abstract void onPlayerDisconnect(GameClient player);

	public boolean isInitialized() {
		return initialized;
	}	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public void setGameType(GameType gameType) {
		this.gameType = gameType;
	}
	
	public GameType getGameType() {
		return gameType;
	}
	
	@Override
	public void update() throws InterruptedException {
		long now = Calendar.getInstance().getTimeInMillis();
		if (now - lastUpdateTime < GameManager.get().getMinUpdateInterval()) {
			Thread.sleep(GameManager.get().getMinUpdateInterval());
			lastUpdateTime = Calendar.getInstance().getTimeInMillis();
		} else {
			lastUpdateTime = now;	
		}
		 
	}
	
	public void stop() {
		needStop = true;
	}
	
	@Override
	public void onProblem(Integer problemType, Object problem) {
		if (problemType == null) {
			//TODO: ???
			onError(problem);
		} else {
			switch(problemType.intValue()) {
			case ExecutionProblems.UPDATE_EXEPTION:
				onError(problem);
				break;
			case ExecutionProblems.THREAD_UPDATE_TIMEOUT :
				onUpdateTimeout();
				break;
			case ExecutionProblems.NO_PROBLEM : 
				//TODO????
				break;
			default:
				//TODO: ???
				onError(problem);
				break;
			}
		}
	}
	
	protected void onUpdateTimeout() {
		
	}
	
	protected void onError(Object problem) {
		
	}
	
	protected GameCommand createCommand(short commandId) {
		GameCommand command = new GameCommand(getTypeId(), commandId);
		ByteBuf buf = NettyManager.get().getCommandAllocator().buffer();
		command.setData(buf);
		return command;
	}
		
	
	@Override
	public void setUpdateNode(DoubleLinkedListNode node) {
		myUpdateNode = node;
	}
	
	@Override
	public DoubleLinkedListNode getUpdateNode() {
		return myUpdateNode;
	}
	
	@Override
	public synchronized boolean needStopUpdate() {
		return needStop;
	}
	
	public synchronized void askStop() {
		needStop = true;
	}
		
	
	@Override
	public void wasError() {
		
	}
}
