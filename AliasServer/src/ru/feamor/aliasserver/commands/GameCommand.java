package ru.feamor.aliasserver.commands;

import io.netty.buffer.ByteBuf;

import org.apache.jcs.utils.struct.DoubleLinkedListNode;


public class GameCommand {
	
	/**
	 * Maximum size of command : 100k bytes
	 */
	public static final int MAX_COMMAND_SIZE = 1024 * 100; 
	public static final int LENGTH_OFFSET = 1 + 2;
	public static final int HEADER_SIZE = 1 + 2 + 4;
	
	private byte type;
	private short id;
	private ByteBuf data;
	private long receiveTime;
	private DoubleLinkedListNode commandNode;
	
	public GameCommand() {
		type = CommandTypes.UNKNOWN.TYPE;
		id = CommandTypes.UNKNOWN.UNKNOWN;
		data = null;
	}
	
	public GameCommand(byte type, short id) {
		this.type = type;
		this.id = id;
		data = null;
	}
	
	
	public short getId() {
		return id;
	}
	
	public int getDataLength() {
		int lenght = 0;
		if (data!=null) {
			lenght = data.readableBytes();
		}
		return lenght;
	}
		
	public byte getType() {
		return type;
	}
		
	public void setId(short id) {
		this.id = id;
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
