package ru.feamor.aliasserver.commands;

import java.util.Calendar;

import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.base.DataObject;
import ru.feamor.aliasserver.base.WithRequestId;
import ru.feamor.aliasserver.core.ClientInProcessor;
import ru.feamor.aliasserver.users.GameClient;

public class SystemCommandsClient implements ClientInProcessor, WithRequestId {

	private DoubleLinkedListNode node = new DoubleLinkedListNode(this);
	private GameClient client;
	private int state;
	
	private long lastCommandTime = 0;
	private int lastRequestId = 0;
	
	@Override
	public int nextRequestId() {
		lastRequestId++;
		return lastRequestId;
	}
	
	@Override
	public int currentRequestId() {
		return lastRequestId;
	}
			
	public SystemCommandsClient(GameClient client) {
		this.client = client;
		lastCommandTime = Calendar.getInstance().getTimeInMillis();
	}
	
	@Override
	public DoubleLinkedListNode getProcessorNode() {
		return node;
	}

	@Override
	public GameClient getGameClient() {
		return client;
	}

	@Override
	public int getState() {
		return state;
	}

	@Override
	public void setState(int newState) {
		state = newState;
	}

	@Override
	public void onAdded() {
		
	}
	
	@Override
	public void onRemoved() {
		
	}
	
	@Override
	public void onResumed() {
		// TODO Auto-generated method stub
		
	}
	
	public long getLastCommandTime() {
		return lastCommandTime;
	}
	
	public void setLastCommandTime(long lastCommandTime) {
		this.lastCommandTime = lastCommandTime;
	}

	public void disconnectBy(short reasom, DataObject message) {
		GameCommand disconnectCommand = client.createCommand(CommandTypes.SYSTEM.TYPE, CommandTypes.SYSTEM.DISCONNECT_BY);
		disconnectCommand.getData().writeShort(reasom);
		DataObject.write(message, disconnectCommand.getData());
		client.getConnection().sendLastCommandAndClose(disconnectCommand);
	}
	
}