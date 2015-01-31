package ru.feamor.aliasserver.core;

import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.users.GameClient;

public interface ClientInProcessor {
	DoubleLinkedListNode getProcessorNode();
	GameClient getGameClient();
	int getState();
	void setState(int newState);			
	void onAdded();
	void onResumed();
	void onRemoved();
}
