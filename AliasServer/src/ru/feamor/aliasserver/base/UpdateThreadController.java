package ru.feamor.aliasserver.base;

import java.util.Calendar;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;
import org.json.JSONObject;

public class UpdateThreadController {
	
	public static final long DEFAULT_MAX_TIME_FOR_TAST = 10 * 1000; //20 sec
	public static final long DEFAULT_CHECK_INTERVAL = 5 * 1000; //20 sec
	private static final int DEFAULT_UPDATE_THREADS_COUNT = 5;
	
	private DoubleLinkedList activeQeue;
	private DoubleLinkedList problems;
	private DoubleLinkedList threads;
	
	private java.util.Timer timerControl;

	private boolean needStop = false;
	private Thread updateThread;
	private Semaphore updateSemaphore;
	private int config_updateThreadsCount = DEFAULT_UPDATE_THREADS_COUNT;
	
	public UpdateThreadController() {
		activeQeue = new DoubleLinkedList();
		problems = new DoubleLinkedList();
		threads = new DoubleLinkedList();
		updateSemaphore = new Semaphore(1);
	}
	
	
	public void configure(JSONObject json) {
		//default - no configuration
	}
		
	public void startController() {
		
		updateThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				updateGames();
			}
		}, "UpdateController");
		
		if (needStop) {
			throw new IllegalStateException("Controller was stopped, can`t restart controller, please re-create them!");
		}
		createThreads(config_updateThreadsCount);
		updateThread.start();
		startTimerController();
	}
	
	public void stopController() {
		stopTimerController();
		needStop = true;
		updateThread.interrupt();
		synchronized (threads) {
			for (DoubleLinkedListNode i = threads.getFirst(); i != null; i=i.next) {
				if (i.getPayload()!=null) {
					UpdateObjectThread thread = (UpdateObjectThread) i.getPayload();
					thread.stop();
				}
			}
			threads.removeAll();
		}
		
		synchronized (activeQeue) {
			activeQeue.removeAll();
		}
		
		updateSemaphore.release();
	}
	
	
	public void addUpdateObject(ThreadUpdated executed) {
		synchronized (activeQeue) {
			if (executed.getUpdateNode() == null) {
				DoubleLinkedListNode node = new DoubleLinkedListNode(executed);
				executed.setUpdateNode(node);
			}
			activeQeue.addLast(executed.getUpdateNode());
		}
		updateSemaphore.release();
	}
	
	public void removeUpdateObject(ThreadUpdated executed) {
		synchronized (activeQeue) {
			DoubleLinkedListNode node = executed.getUpdateNode();
			if (node != null) {
				activeQeue.remove(node);
			}
		}
	}
	
	public void wasProblemsInUpdate(ThreadUpdated executed, Integer problemType, Object problem) {
		synchronized (problems) {
			ImmutableTriple<ThreadUpdated, Integer, Object> problemDescription = new ImmutableTriple<UpdateThreadController.ThreadUpdated, Integer, Object>(executed, problemType, problem);				
			problems.addLast(new DoubleLinkedListNode(problemDescription));
		}
	}
	
	public void clearProblems() {
		synchronized (problems) {
			problems.removeAll();
		}
	}
	
	private void createThreads(int count) {
		UpdateObjectThread thread;
		synchronized (threads) {
			for (int i=0; i<count; i++) {
				thread = new UpdateObjectThread(i, this);
				thread.startThread();
				threads.addLast(thread.getMyNode());
			}
		}
	}
	
	private UpdateObjectThread getFreeThread() {
		UpdateObjectThread result = null;
		synchronized (threads) {
			for( DoubleLinkedListNode i = threads.getFirst(); i!=null; i=i.next) {
				UpdateObjectThread thread = (UpdateObjectThread) i.getPayload();
				if (!thread.isInUpdate()) {
					result = thread;
					break;
				}
			}
		}
		return result;
	}
	
	private void checkTooLongWorkingThreads() {
		synchronized (threads) {
//			DoubleLinkedList toRemove = null;
//			for( DoubleLinkedListNode i = threads.getFirst(); i!=null; i=i.next) {
//				GameThread thread = (GameThread) i.getPayload();
//				if (!thread.isFree()) {
//					long now = TimeManager.get().getNow();
//					long lastUpdate = thread.getStartUpdateTime();
//					long delta = now - lastUpdate;
//					if (delta > maxThreadTimePerTask) {
//						thread.getThread().interrupt();
//						if (toRemove == null) {
//							toRemove = new DoubleLinkedList();
//						}
//						toRemove.addLast(new DoubleLinkedListNode(i));
//						pushErrorInUpdate(thread.getExecuted(), new TimeoutException("too long working "+delta));
//					}
//				}
//			}
//			
//			if (toRemove!=null) {
//				for( DoubleLinkedListNode i = threads.getFirst(); i!=null; i=i.next) {
//					threads.remove((DoubleLinkedListNode) i.getPayload());
//					GameThread newThread = new GameThread(threads.size(), this);
//					newThread.startThread();
//					threads.addLast(newThread.getMyNode());
//				}
//			}
		}
	}
	
	private void startTimerController() {
//		stopTimerController();
//		timerControl = new Timer();
//		timerControl.schedule(new TimerTask() {
//			
//			@Override
//			public void run() {
//				checkTooLongWorkingThreads();
//			}
//		}, maxThreadTimePerTask, maxThreadTimePerTask);		
	}
	
	private void stopTimerController() {
		if (timerControl!=null) {
			timerControl.cancel();
			timerControl = null;
		}
	}
	
	public void notifyThreadFinish(UpdateObjectThread thread) {
		//TODO: check if thread was interupred
		updateSemaphore.release();
	}
	
	private long startWaitThreadTime = 0;
	private long startWaitObjectTime = 0;
	
	private void updateGames() {		
		
		while(!needStop) {
			if (Thread.interrupted()) {
				needStop = true;	
			} else {
				ThreadUpdated objectToUpdate = null;			
				UpdateObjectThread freeThread = getFreeThread();
				if (freeThread!=null) {
					synchronized (activeQeue) {
						DoubleLinkedListNode node = activeQeue.getFirst();
						if (node!=null) {
							objectToUpdate = (ThreadUpdated) node.getPayload();
							activeQeue.remove(node);
						}
					}
					
					if (objectToUpdate != null) {
						freeThread.executeGame(objectToUpdate);
					} else {
						startWaitObjectTime = Calendar.getInstance().getTimeInMillis();
						try {
							updateSemaphore.drainPermits();
							updateSemaphore.acquire();
						} catch(InterruptedException iex) {
							needStop = true;
						}
					}
				} else {
					startWaitThreadTime = Calendar.getInstance().getTimeInMillis();
					try {
						updateSemaphore.drainPermits();
						updateSemaphore.acquire();
					} catch (InterruptedException e) {
						needStop = true;
					}
				}			
			}
		}
	}
	
	public interface ThreadUpdated {
		void setUpdateNode(DoubleLinkedListNode node);
		DoubleLinkedListNode getUpdateNode();
		
		boolean needStopUpdate();
		
		void update() throws InterruptedException;
	}
	
	private static class UpdateObjectThread implements Runnable {
		
		private Thread thread;
		private Semaphore semaphore;
		
		private ThreadUpdated executed;
		private boolean needStop;
		private int id;
		
		private long startUpdateTime;
		private UpdateThreadController controller;
		private DoubleLinkedListNode myNode;
		private boolean inUpdate;
		
		public UpdateObjectThread(int id, UpdateThreadController controller) {
			this.id = id;
			thread = new Thread(this, "UpdateThread_"+id);
			semaphore = new Semaphore(0);
			needStop = false;
			inUpdate = true;
			myNode = new DoubleLinkedListNode(this);
			this.controller = controller;
			startUpdateTime = -1;
		}
		
		public void startThread() {
			thread.start();		
		}
			
		public synchronized boolean isInUpdate() {
			return inUpdate;
		}
			
		public void stop() {
			needStop = true;
			thread.interrupt();
		}
				
		@Override
		public void run() {
			while (!needStop) {
				if (Thread.interrupted()) {
					needStop = true;
				} else {
					if (executed != null && !executed.needStopUpdate()) {
						startUpdateTime = Calendar.getInstance().getTimeInMillis();
						try {
							executed.update();
							if (!executed.needStopUpdate()) {
								controller.addUpdateObject(executed);
							}
						} catch (Throwable ex) {
							if (ex instanceof InterruptedException) {
								needStop = true;
								executed = null;
								continue;
							}
							controller.wasProblemsInUpdate(executed, null, ex);
						}
					}
					executed = null;
					synchronized (UpdateObjectThread.this) {
						inUpdate = false;
					}
					try {
						semaphore.drainPermits();
						semaphore.acquire();
					} catch (InterruptedException e) {
						needStop = true;
					}
				}
			}
		}
		
		public long getStartUpdateTime() {
			return startUpdateTime;
		}
		
		public void executeGame(UpdateThreadController.ThreadUpdated game) {
			if (inUpdate) {
				throw new IllegalStateException("Can`t start Execute, because thread Execute another object");
			}
			synchronized (this) {
				inUpdate = true;
			}
			
			executed = game;
			semaphore.release();
		}
		
		public int getId() {
			return id;
		}
		
		public DoubleLinkedListNode getMyNode() {
			return myNode;
		}	
	}

	

}
