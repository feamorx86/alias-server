package ru.feamor.aliasserver.users;

import java.util.Calendar;

import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import gnu.trove.map.hash.TIntObjectHashMap;
import ru.feamor.aliasserver.base.DataObject;

public class UserInPool {
	
	public static final byte OFF_LINE = 0;
	public static final byte ONLINE_IN_GAME = 1;
	public static final byte ONLINE_IN_MENU = 2;
	public static final byte ONLINE_WAIT_GAME = 3;
	public static final byte RESUMED = 4;
	
	private GameClient client;
	private long userId;
	private byte state;
	private int currentGameType;
	private TIntObjectHashMap<DataObject> extraData = new TIntObjectHashMap<DataObject>();
	private DoubleLinkedListNode node;
	private long startTime;
	private long lastActionTime;
	
	public UserInPool(GameClient client) {
		this.client = client;
		this.state = ONLINE_IN_MENU;
		this.userId = client.getPlayer().getId();
		node = new DoubleLinkedListNode(this);
		startTime = Calendar.getInstance().getTimeInMillis();
	}
	
	public DoubleLinkedListNode getNode() {
		return node;
	}
	
	public GameClient getClient() {
		return client;
	}
	
	public long getUserId() {
		return userId;
	}
	
	public byte getState() {
		return state;
	}
	
	public void changeState(byte newState) {
		this.state = newState;
	}
	
	boolean isOnline() {
		return state != OFF_LINE;
	}
	
	boolean isInGame() {
		return state == ONLINE_IN_GAME || state == ONLINE_WAIT_GAME;
	}
	
	public int getCurrentGameType() {
		return currentGameType;
	}
	
	public void setCurrentGameType(int currentGameType) {
		this.currentGameType = currentGameType;
	}
	
	public DataObject getExtra(int key) {
		return extraData.get(key);
	}
	
	public void putExtra(int key, DataObject data) {
		extraData.put(key, data);
	}
	
	public long getLastActionTime() {
		return lastActionTime;
	}
	
	public void setLastActionTime(long lastActionTime) {
		this.lastActionTime = lastActionTime;
	}
	
	public long getStartTime() {
		return startTime;
	}
}
