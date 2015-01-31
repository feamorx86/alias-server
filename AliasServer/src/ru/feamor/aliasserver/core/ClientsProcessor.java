package ru.feamor.aliasserver.core;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.Calendar;
import java.util.Iterator;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.base.RunnableExecutor;
import ru.feamor.aliasserver.base.UpdateThreadController;

public abstract class ClientsProcessor implements UpdateThreadController.ThreadPendingUpdated{
	public static final long DEFAULT_MIN_UPDATE_TIME = 200;
	
	protected TLongObjectHashMap<ClientInProcessor> clients = new TLongObjectHashMap<ClientInProcessor>();
	protected DoubleLinkedList newClients = new DoubleLinkedList();
	protected DoubleLinkedList clintsForRemove = new DoubleLinkedList();
	
	private Object clientsLocker = new Object();
	private DoubleLinkedListNode updateNode;
	protected boolean needStop = false;
	private long lastUpdateTime;
	
	public void addNewClient(ClientInProcessor client) {
		synchronized (newClients) {
			newClients.addLast(client.getProcessorNode());
		}
		client.onAdded();
	}
	
	public ClientInProcessor getClientById(long clientId) {
		return clients.get(clientId);
	}
	
	public void addResuedClient(ClientInProcessor client) {
		ClientInProcessor oldClient = clients.get(client.getGameClient().getPlayer().getId());
		if (oldClient!=null) {
			removeClient(oldClient);
		}
		
		synchronized (newClients) {
			newClients.addLast(client.getProcessorNode());
		}
		client.onResumed();
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
	
	protected boolean checkIsItTimeToUpdate() {
		boolean needUpdate = true;
		
		pending = false;
		long now = Calendar.getInstance().getTimeInMillis();
		long delta = now - lastUpdateTime;
		lastUpdateTime = now;
		
		if (delta < getMinUpdateTime()) {
			nextUpdateTime = now + delta;
			pending = true;
			needUpdate = false;
		}
		
		return needUpdate;
	}
	
	@Override
	public void update() throws InterruptedException {
		if (!needStop && checkIsItTimeToUpdate()) {
			synchronized (clintsForRemove) {
				DoubleLinkedListNode node = clintsForRemove.getFirst();
				while(node!=null) {
					DoubleLinkedListNode next = node.next;
					clintsForRemove.remove(node);
					ClientInProcessor client = (ClientInProcessor) node.getPayload();
					synchronized (clientsLocker) {
						clients.remove(client.getGameClient().getPlayer().getId());
					}
					node = next;
				}
			}
			
			synchronized (newClients) {
				DoubleLinkedListNode node = newClients.getFirst();
				while(node!=null) {
					DoubleLinkedListNode next = node.next;
					newClients.remove(node);
					ClientInProcessor client = (ClientInProcessor) node.getPayload();
					synchronized (clientsLocker) {
						clients.put(client.getGameClient().getPlayer().getId(), client);
					}					
					node = next;
				}
			}
							
			if (needStop) return;
			synchronized (clientsLocker) {
				for ( TLongObjectIterator<ClientInProcessor> it = clients.iterator(); it.hasNext(); ) {
					it.advance();
					ClientInProcessor client = clients.iterator().value();
					if (needStop) {
						return;
					}
					processClient(client);
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