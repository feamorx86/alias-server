package ru.feamor.aliasserver.commands;

import io.netty.buffer.ByteBuf;

import org.apache.jcs.utils.struct.DoubleLinkedListNode;


public class GameCommand {
	
	/**
	 * Maximum size of command : 100k bytes
	 */
	public static final int MAX_COMMAND_SIZE = 1024 * 100; 
	public static final int LENGTH_OFFSET = Byte.SIZE + Short.SIZE;
	public static final int HEADER_SIZE = Byte.SIZE + Short.SIZE + Integer.SIZE;
	
	private byte type;
	private short id;
	private int length;
	private ByteBuf data;
	private long receiveTime;
	private DoubleLinkedListNode commandNode;
	
	public GameCommand() {
		type = CommandTypes.UNKNOWN.TYPE;
		id = CommandTypes.UNKNOWN.UNKNOWN;
		length = 0;
		data = null;
	}
	
	public GameCommand(byte type, short id) {
		this.type = type;
		this.id = id;
		length = 0;
		data = null;
	}
	
	
	public short getId() {
		return id;
	}
	
	public int getLength() {
		return length;
	}
	
	 
	
	public int getFullLength() {
		int staticLength = HEADER_SIZE; 
		int dataLength = getLength();
		return staticLength + dataLength;
	}
	
	public byte getType() {
		return type;
	}
		
	public void setId(short id) {
		this.id = id;
	}
	
	public void setLength(int length) {
		this.length = length;
	}
	
	public void setType(byte type) {
		this.type = type;
	}
	
	public long getReceiveTime() {
		return receiveTime;
	}
	
	public void setReceiveTime(long receiveTime) {
		this.receiveTime = receiveTime;
	}
	
	public DoubleLinkedListNode getCommandNode() {
		return commandNode;
	}
	
	public void setCommandNode(DoubleLinkedListNode commandNode) {
		this.commandNode = commandNode;
	}
	
	public ByteBuf getData() {
		return data;
	}
	
	public void setData(ByteBuf data) {
		this.data = data;
	}
	
	public GameCommand retain() {
		if (data!=null) {
			data.retain();
		}
		return this;
	}
	
	public GameCommand recycle() {
		if (data!=null) {
			data.release();
		}
		return this;
	}
}
