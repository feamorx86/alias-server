package ru.feamor.aliasserver.base;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;

public abstract class ObjectCache<T extends ObjectCache.CachedObject> {
	
	public static final int DEAULT_START_SIZE = 0;
	public static final int DEAULT_MAX_SIZE = 100;
		
	private DoubleLinkedList objectCahce = new DoubleLinkedList();
	private int maxSize = DEAULT_MAX_SIZE;
	
	public abstract T create();
	
	public ObjectCache(int startSize, int maxSize) {
		objectCahce  = new DoubleLinkedList();
		this.maxSize = maxSize;
		for (int i = 0; i < startSize; i++) {
			T object = create();
			objectCahce.addLast(object.getCacheNode());
		}
	}
	
	public synchronized T get() {
		T result;
		if (objectCahce.size() > 0) {
			result = (T)objectCahce.removeLast().getPayload();
		} else {
			result = create();
		}		
		return result;
	}
	
	public synchronized void back(T item) {
		item.beforeReturn();
		if (objectCahce.size() < maxSize) {
			objectCahce.addLast(item.getCacheNode());
		}
	}
	
	public static interface CachedObject {
		DoubleLinkedListNode getCacheNode();
		void beforeReturn();
	}
	
	public static class SimpleCachedObject implements CachedObject {
		protected DoubleLinkedListNode nodeForCahce;
				
		public SimpleCachedObject() {
			nodeForCahce = new DoubleLinkedListNode(this);
		}
		
		@Override
		public void beforeReturn() {
			
		}
		
		@Override
		public DoubleLinkedListNode getCacheNode() {
			return nodeForCahce;
		}
	}
	
}
 