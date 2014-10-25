package ru.feamor.aliasserver.commands;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;

public class GameCommandHolder {
	private byte type;
	DoubleLinkedList commands;
	
	public GameCommandHolder(int type) {
		this.type = (byte) type;
		commands = new DoubleLinkedList();
	}
	
	public byte getType() {
		return type;
	}
	
	public void addCommand(GameCommand command) {
		DoubleLinkedListNode node = new DoubleLinkedListNode(command);
		command.setCommandNode(node);
		commands.addLast(node);
	}
	
	public void removeCommand(GameCommand command) {
		DoubleLinkedListNode node = command.getCommandNode();
		if (node!=null) {
			commands.remove(node);
			command.setCommandNode(null);
		}
	}
	
	public DoubleLinkedList getCommands() {
		return commands;
	}
}
