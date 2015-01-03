package ru.feamor.aliasserver.base;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;
import org.json.JSONObject;

import ru.feamor.aliasserver.utils.Log;

public class UpdateThreadController implements RunnableExecutor {

	public static class ExecutionProblems {
		public static final int NO_PROBLEM = 0;
		public static final int THREAD_UPDATE_TIMEOUT = 1;
		public static final int UPDATE_EXEPTION = 2;
	}
		
	/**
	 * Maximum time to execute One task. If task executed more then DEFAULT_MAX_UPDATE_INTERVAL - thread
	 * with this task will be stopped with UpdateObjectThread.stop(), executed  object (ThreadUpdated) - will be
	 * moved into problems and thread will be re-created (removed and later added).
	 */
	public static final long DEFAULT_MAX_UPDATE_INTERVAL = 20 * 1000;
	/**
	 * Interval for timer. Each operation : checkTooLongWorking, wakeUpdate and wakeThreads will be executed int this timer.
	 */
	public static final long DEFAULT_TIMER_UPDATE = 1000;
	/**
	 * How many threads will be created for update.
	 */
	private static final int DEFAULT_UPDATE_THREADS_COUNT = 5;
	
	public static final long DEFAULT_WAKE_UPDATE_NTERVAL= 5000;
	public static final long DEFAULT_WAKE_UPDATE_THREADS_NTERVAL= 7000;
	public static final long DEFAULT_CHECK_THREADS_NTERVAL= 10000;
	
	private DoubleLinkedList activeQeue;
	private DoubleLinkedList threads;
	
	private DoubleLinkedList problems;
	private DoubleLinkedList problemsTemp;
	private Object problemsLocker = new Object();
	
	private java.util.Timer timerControl;

	private boolean needStop = false;
	private Thread updateThread;
	private Semaphore updateSemaphore;
	private int config_updateThreadsCount = DEFAULT_UPDATE_THREADS_COUNT;
	private long config_timerInterval = DEFAULT_TIMER_UPDATE;
	private long config_maxUpdateTime = DEFAULT_MAX_UPDATE_INTERVAL;
	
	private long config_wakeUpdateInterval = DEFAULT_WAKE_UPDATE_NTERVAL;
	private long currentWakeUpdateInterval;
	
	private long config_wakeUpdateThreadsInterval = DEFAULT_WAKE_UPDATE_THREADS_NTERVAL;
	private long currentpdateThreadsInterval;
	
	private long config_checkThreadsInterval = DEFAULT_CHECK_THREADS_NTERVAL;
	private long currentThreadsInterval;

	private long lastUpdatedTime;
	private String name;
	
	public UpdateThreadController(String name) {
		activeQeue = new DoubleLinkedList();
		problems = new DoubleLinkedList();
		problemsTemp = new DoubleLinkedList();
		threads = new DoubleLinkedList();
		updateSemaphore = new Semaphore(1);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
			
	public void startController() {		
		updateThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				updateThreads();
			}
		}, name+"_Update");
		
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
	
	@Override
	public void executeRunnable(Runnable r) {
		RunnableInThreadUpdated updateRunnable = new RunnableInThreadUpdated(r);
		addUpdateObject(updateRunnable);
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
		synchronized (problemsLocker) {
			ImmutableTriple<ThreadUpdated, Integer, Object> problemDescription = new ImmutableTriple<UpdateThreadController.ThreadUpdated, Integer, Object>(executed, problemType, problem);				
			problems.addLast(new DoubleLinkedListNode(problemDescription));
		}
		if (executed!=null) {
			executed.wasError();
		} 
	}
	
	public void checkProblems() {
		if (problems.size() > 0) {
			synchronized (problemsLocker) {
				problemsTemp.removeAll();
				DoubleLinkedList temp = problemsTemp;
				problemsTemp = problems;
				problems = temp;
			}
			
			for( DoubleLinkedListNode i = problemsTemp.getFirst(); i!=null; i=i.next) {
				ImmutableTriple<ThreadUpdated, Integer, Object> problemDescription = (ImmutableTriple<ThreadUpdated, Integer, Object>) i.getPayload();
				ThreadUpdated problemObject = problemDescription.left;
				if (problemObject!=null) {
					problemObject.onProblem(problemDescription.middle, problemDescription.right);
				}
			}
		}
	}
	
	private int lastThreadId = 0;
	private void createAndStartThread() {
		UpdateObjectThread thread;
		thread = new UpdateObjectThread(lastThreadId, this);
		lastThreadId++;
		thread.setMyNode(new DoubleLinkedListNode(thread));
		thread.startThread();
		threads.addLast(thread.getMyNode());
	}
	
	
	private void createThreads(int count) {
		
		synchronized (threads) {
			for (int i=0; i<count; i++) {
				createAndStartThread();
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
			DoubleLinkedList toRemove = null;
			for( DoubleLinkedListNode i = threads.getFirst(); i!=null; i=i.next) {
				UpdateObjectThread thread = (UpdateObjectThread) i.getPayload();
				if (thread.isInUpdate()) {
					long now = Calendar.getInstance().getTimeInMillis();
					long lastUpdate = thread.getStartUpdateTime();
					long delta = now - lastUpdate;
					if (delta > config_maxUpdateTime) {
						thread.stop();
						if (toRemove == null) {
							toRemove = new DoubleLinkedList();
						}
						toRemove.addLast(new DoubleLinkedListNode(i));
						wasProblemsInUpdate(thread.executed, ExecutionProblems.THREAD_UPDATE_TIMEOUT, new ImmutablePair<Long, Long>(now, lastUpdate));
					}
				}
			}
			
			if (toRemove!=null) {
				for( DoubleLinkedListNode i = toRemove.getFirst(); i!=null; i=i.next) {
					threads.remove((DoubleLinkedListNode) i.getPayload());
					createAndStartThread();
				}
			}
		}
	}
	
	private void releaseUpdateThreadSemaphores() {
		synchronized (threads) {
			for( DoubleLinkedListNode i = threads.getFirst(); i!=null; i=i.next) {
				UpdateObjectThread thread = (UpdateObjectThread) i.getPayload();
				thread.releaseOneSemaphore();
			}
		}
	}
	
	private void releaeUpdatreSemaphore() {
		updateSemaphore.release();
	}
	
	private void startTimerController() {
		stopTimerController();
		timerControl = new Timer(name+"_timer");
		currentWakeUpdateInterval = 0;
		currentpdateThreadsInterval = 0;
		currentThreadsInterval = 0;		
		lastUpdatedTime = Calendar.getInstance().getTimeInMillis();		
		timerControl.schedule(new TimerTask() {
			
			@Override
			public void run() {
				if (!needStop) {
					long now = Calendar.getInstance().getTimeInMillis();
					long delta = now - lastUpdatedTime;
					currentWakeUpdateInterval+=delta;
					currentpdateThreadsInterval+=delta;
					currentThreadsInterval+=delta;
					if (currentWakeUpdateInterval>=config_wakeUpdateInterval) {
						currentWakeUpdateInterval = 0;
						releaeUpdatreSemaphore();
					}
					if (currentpdateThreadsInterval >= config_wakeUpdateThreadsInterval) {
						currentpdateThreadsInterval = 0;
						releaseUpdateThreadSemaphores();
					}
					if (currentThreadsInterval >= config_checkThreadsInterval) {
						currentThreadsInterval = 0;
						checkTooLongWorkingThreads();
					}				
				}
			}
		}, config_timerInterval, config_timerInterval);		
	}
	
	private void stopTimerController() {
		if (timerControl!=null) {
			timerControl.cancel();
			timerControl = null;
		}
	}
		
//	private long startWaitThreadTime = 0;
//	private long startWaitObjectTime = 0;
	/**
	 * Search object to update, and skip ThreadPendingUpdated, witch waiting. 
	 * @return ThreadUpdated
	 */
	
	private void updateThreads() {		
		while(!needStop) {
			if (Thread.interrupted()) {
				needStop = true;	
			} else {
				ThreadUpdated objectToUpdate = null;			
				UpdateObjectThread freeThread = getFreeThread();
				if (freeThread!=null) {
					long now = Calendar.getInstance().getTimeInMillis();
					boolean hasPending = false;
					long minWaitPending = Long.MAX_VALUE;
					synchronized (activeQeue) {
						DoubleLinkedListNode node = activeQeue.getFirst();
						while (node!=null) {
							objectToUpdate = (ThreadUpdated) node.getPayload();
							if (objectToUpdate instanceof ThreadPendingUpdated) {
								ThreadPendingUpdated pendingObject = (ThreadPendingUpdated) objectToUpdate;
								if (pendingObject.isPending()) {
									hasPending = true;
									if (now > pendingObject.getExecuteTime()) {
										activeQeue.remove(node);
										node = null;
									} else {
										if (pendingObject.getExecuteTime() < minWaitPending) {
											minWaitPending = pendingObject.getExecuteTime();
										}
										node = node.next;
									}
								}
							} else {
								activeQeue.remove(node);
								node = null;
							}
						}
					}

					if (objectToUpdate != null) {
						freeThread.execute(objectToUpdate);
					} else {
						
						if (hasPending) {
							long delta = minWaitPending - now;
//							startWaitObjectTime = Calendar.getInstance().getTimeInMillis();
							try {
								updateSemaphore.drainPermits();
								updateSemaphore.tryAcquire(delta, TimeUnit.MILLISECONDS);
								вот тут нужно проверить что да как "!!
							} catch(InterruptedException iex) {
								needStop = true;
							}
							
						} else {
//							startWaitObjectTime = Calendar.getInstance().getTimeInMillis();
							try {
								updateSemaphore.drainPermits();
								updateSemaphore.acquire();
							} catch(InterruptedException iex) {
								needStop = true;
							}
	
						}						
					}
				} else {
//					startWaitThreadTime = Calendar.getInstance().getTimeInMillis();
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
	
	public void configure(JSONObject config) {
		config_updateThreadsCount = config.optInt("threads", DEFAULT_UPDATE_THREADS_COUNT);
		config_timerInterval = config.optLong("updateTimerIntervel", DEFAULT_TIMER_UPDATE);
		config_maxUpdateTime = config.optLong("maxUpdateTime", DEFAULT_MAX_UPDATE_INTERVAL);
		config_wakeUpdateInterval = config.optLong("wakeUpdateInterval", DEFAULT_WAKE_UPDATE_NTERVAL);
		config_wakeUpdateThreadsInterval = config.optLong("wakeThreadsInterval", DEFAULT_WAKE_UPDATE_THREADS_NTERVAL);
		config_checkThreadsInterval = config.optLong("checkThreadsInterval", DEFAULT_CHECK_THREADS_NTERVAL);
	}
	
	public void setConfig_checkThreadsInterval(long config_checkThreadsInterval) {
		this.config_checkThreadsInterval = config_checkThreadsInterval;
	}
	
	public void setConfig_maxUpdateTime(long config_maxUpdateTime) {
		this.config_maxUpdateTime = config_maxUpdateTime;
	}
	
	public void setConfig_timerInterval(long config_timerInterval) {
		this.config_timerInterval = config_timerInterval;
	}
	
	public void setConfig_updateThreadsCount(int config_updateThreadsCount) {
		this.config_updateThreadsCount = config_updateThreadsCount;
	}
	
	public void setConfig_wakeUpdateInterval(long config_wakeUpdateInterval) {
		this.config_wakeUpdateInterval = config_wakeUpdateInterval;
	}
	
	public void setConfig_wakeUpdateThreadsInterval(
			long config_wakeUpdateThreadsInterval) {
		this.config_wakeUpdateThreadsInterval = config_wakeUpdateThreadsInterval;
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
			thread = new Thread(this, controller.getName()+"_th_"+id);
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
		
		public void releaseOneSemaphore() {
			semaphore.release();
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
							} else {
								controller.wasProblemsInUpdate(executed, ExecutionProblems.UPDATE_EXEPTION, ex);
							}
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
		
		public void execute(UpdateThreadController.ThreadUpdated game) {
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
		
		public void setMyNode(DoubleLinkedListNode myNode) {
			this.myNode = myNode;
		}
	}

	public interface ThreadUpdated {
		void setUpdateNode(DoubleLinkedListNode node);
		DoubleLinkedListNode getUpdateNode();
		
		boolean needStopUpdate();
		
		void update() throws InterruptedException;
		void onProblem(Integer problemType, Object problem);
		void wasError();
	}
	
	public interface ThreadPendingUpdated extends ThreadUpdated {
		boolean isPending();
		long getExecuteTime();
	}
	
	public class RunnableInThreadUpdated implements ThreadUpdated {
		protected DoubleLinkedListNode updateNode;
		protected Runnable runnable = null;
		protected boolean runnableStarted = false;
		
		public RunnableInThreadUpdated(Runnable r) {
			runnable = r;
		}
		
		@Override
		public void setUpdateNode(DoubleLinkedListNode node) {
			updateNode = node;
		}
		
		@Override
		public DoubleLinkedListNode getUpdateNode() {
			return updateNode;
		}
		
		@Override
		public boolean needStopUpdate() {
			return !runnableStarted;
		}
		
		@Override
		public void update() throws InterruptedException {
			if (!runnableStarted) {
				runnableStarted = true;
				if (runnable!=null) {
					runnable.run();
				}
			}
		}
		
		@Override
		public void onProblem(Integer problemType, Object problem) {
			if (problemType == null) {
				if (problem == null) {
					Log.e(getClass(), "Error in runnable. Error type= <null>, error = <null>");
				} else {
					Log.e(getClass(), "Error in runnable. Error type= <null>, error = "+problem);
				}
			} else {
				if (problem == null) {
					Log.e(getClass(), "Error in runnable. Error type= "+problemType+", error = <null>");
				} else {
					Log.e(getClass(), "Error in runnable. Error type= "+problemType+", error = "+problem);
				}
			}
			
		}
		
		@Override
		public void wasError() {
			
		}
	}
}
