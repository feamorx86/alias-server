package ru.feamor.aliasserver.core;

import java.util.Calendar;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.base.RunnableExecutor;
import ru.feamor.aliasserver.base.UpdateThreadController;

public abstract class ClientsProcessor implements UpdateThreadController.ThreadPendingUpdated{
	public static final long DEFAULT_MIN_UPDATE_TIME = 200;
	
	protected DoubleLinkedList clients = new DoubleLinkedList();
	protected DoubleLinkedList newClients = new DoubleLinkedList();
	protected DoubleLinkedList clintsForRemove = new DoubleLinkedList();
	
	private Object clientsLocker = new Object();
	private DoubleLinkedListNode updateNode;
	protected boolean needStop = false;
	private long lastUpdateTime;
	
	public void addClient(ClientInProcessor client) {
		synchronized (newClients) {
			newClients.addLast(client.getProcessorNode());
		}
		client.onAdded();
	}
	
	public void removeClient(ClientInProcessor client) {
		synchronized (clintsForRemove) {
			clintsForRemove.remove(client.getProcessorNode());
		}
		client.onRemoved();
	}
	
	public abstract void processClient(ClientInProcessor client);
	
	public abstract RunnableExecutor getExecutor();
	
	public void stopProcessor() {
		needStop = true;
	}
	
	public long getMinUpdateTime() {
		return DEFAULT_MIN_UPDATE_TIME;
	}
	
	private boolean pending = false;
	private long nextUpdateTime;
	
	@Override
	public boolean isPending() {
		return pending;
	}
	
	@Override
	public long getExecuteTime() {
		return nextUpdateTime;
	}
	
	@Override
	public void update() throws InterruptedException {
		if (!needStop) {
			pending = false;
			long now = Calendar.getInstance().getTimeInMillis();
			long delta = now - lastUpdateTime;
			
			if (delta < getMinUpdateTime()) {
				nextUpdateTime = now + delta;
				lastUpdateTime = now;
				pending = true;			
			} else {
				lastUpdateTime = now;
				synchronized (newClients) {
					DoubleLinkedListNode node = newClients.getFirst();
					while(node!=null) {
						DoubleLinkedListNode next = node.next;
						newClients.remove(node);
						synchronized (clientsLocker) {
							clients.addLast(node);
						}					
						node = next;
					}
				}
				
				synchronized (clintsForRemove) {
					DoubleLinkedListNode node = clintsForRemove.getFirst();
					while(node!=null) {
						DoubleLinkedListNode next = node.next;
						clintsForRemove.remove(node);
						synchronized (clientsLocker) {
							clients.addLast(node);
						}
						node = next;
					}
				}
				
				if (needStop) return;
				synchronized (clientsLocker) {
					for(DoubleLinkedListNode i = clients.getFirst(); i!=null; i = i.next) {
						ClientInProcessor client = (ClientInProcessor) i.getPayload();
						if (needStop) {
							return;
						}
						processClient(client);
					}
				}
			}
		}
	}
	
	@Override
	public DoubleLinkedListNode getUpdateNode() {
		return updateNode;
	}
	
	@Override
	public void setUpdateNode(DoubleLinkedListNode node) {
		updateNode = node;
	}
	
	@Override
	public boolean needStopUpdate() {
		return needStop;
	}
	
	@Override
	public void onProblem(Integer problemType, Object problem) {
		
	}
	
	@Override
	public void wasError() {
		
	}		
}