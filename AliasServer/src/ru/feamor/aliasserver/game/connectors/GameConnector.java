package ru.feamor.aliasserver.game.connectors;

import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.base.UpdateThreadController.ThreadUpdated;
import ru.feamor.aliasserver.game.GameType;
import ru.feamor.aliasserver.users.UsersPool.UserQueue;

public class GameConnector implements ThreadUpdated {
	
	private DoubleLinkedListNode updateNode;		
	protected GameType gameType;
	protected UserQueue queue;
	protected boolean needStop;
	
	public GameConnector(GameType gameType, UserQueue queue) {
		this.gameType = gameType;
		this.queue = queue;
	}
			
	@Override
	public void update() throws InterruptedException {
		
	}

	@Override
	public void onProblem(Integer problemType, Object problem) {
		
	}
	
	@Override
	public void wasError() {
		
	}
	
	@Override
	public boolean needStopUpdate() {
		return needStop;
	}

	
	@Override
	public void setUpdateNode(DoubleLinkedListNode node) {
		updateNode = node;
	}
	
	@Override
	public DoubleLinkedListNode getUpdateNode() {
		return updateNode;
	}
}