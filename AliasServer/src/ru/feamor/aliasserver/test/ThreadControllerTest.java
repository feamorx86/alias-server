package ru.feamor.aliasserver.test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;
import org.json.JSONObject;

import ru.feamor.aliasserver.base.UpdateThreadController;
import ru.feamor.aliasserver.utils.Log;

public class ThreadControllerTest extends BaseTest {
	
	UpdateThreadController controller;
	
	class TestExecuted implements UpdateThreadController.ThreadUpdated {
		
		int count;
		int minWait;
		int maxWait;
		String name;
		Random rand = new Random();
		boolean hasError = false;
		String errorMessage ="";

		public TestExecuted(int count, int minWait, int maxWait, String name, boolean hasError) {
			super();
			this.count = count;
			this.minWait = minWait;
			this.maxWait = maxWait;
			this.name = name;
			this.hasError = true;
			if (hasError) {
				errorMessage = " (E) ";
			}
			Log.i("ThreadObject", "Create: <"+name+"> ("+count+"), sleep "+minWait+" - "+maxWait+" ms"+errorMessage);
		}

		DoubleLinkedListNode node;
		
		@Override
		public void setUpdateNode(DoubleLinkedListNode node) {
			this.node = node;
		}

		@Override
		public DoubleLinkedListNode getUpdateNode() {
			return node;
		}
		
		boolean needStop = false;

		@Override
		public boolean needStopUpdate() {
			return needStop;
		}
		
		@Override
		public void update() throws InterruptedException {
			if (count <= 0) {
				needStop = true;
				if (hasError) {
					Log.i("ThreadObject", "Start Error! <"+name+"> ");
					throw new RuntimeException("Special error for <"+name+">");
				} else {
					Log.i("ThreadObject", "Complete <"+name+"> ");
				}
			} else {
				int delta = maxWait - minWait;
				if (delta < 0) delta = -delta;
				delta = rand.nextInt(delta) + minWait;
				if (delta > 0) {
					try {
						Log.i("ThreadObject", "Executed <"+name+"> ("+count+"), will sleep "+delta+" ms"+errorMessage);
						Thread.sleep(delta);
					} catch (InterruptedException e) {
						e.printStackTrace();
						throw e;
					}
				}
				count--;
			}
		}

		@Override
		public void onProblem(Integer problemType, Object problem) {
			if (problemType!=null) {
				switch (problemType.intValue()) {
				case UpdateThreadController.ExecutionProblems.THREAD_UPDATE_TIMEOUT:
					needStop = true;
					ImmutablePair<Long, Long> timers = (ImmutablePair<Long, Long>) problem;
					String timeMessage = "";
					if (timers!=null) {
						long now = timers.left;
						long started = timers.right;
						timeMessage = String.format("Timeout: %d, started at: %d, finish at %d", now-started, now, started);
					}
					Log.e("ThreadObject", "ERROR: <"+name+">Object updated too long"+timeMessage );
					break;
				case UpdateThreadController.ExecutionProblems.UPDATE_EXEPTION:
					Log.e("ThreadObject", "ERROR <"+name+"> in update: Exception", (Throwable)problem);
					needStop = true;
					break;
				}
			} else {
				Log.e("ThreadObject", "ERROR: <"+name+"> Occour was found unexpected error, object will be stopped");
				needStop = true;
			}
		}

		@Override
		public void wasError() {
			Log.e("ThreadObject", "ERROR: at <"+name+">");
			handler.submit(checkErrors);
		}		
	}
		
	void fillData() {
		Random r = new Random();
		for (int i = 0; i < 21; i++) {
			int minWait = r.nextInt(9000);
			int maxWait = r.nextInt(60000);
			int count = r.nextInt(30)+3;
			String name = "Obj-"+i;
			boolean hasError = false;
			if (i%5 == 0) hasError = true;
			TestExecuted exec = new TestExecuted(count, minWait, maxWait, name, hasError);
			controller.addUpdateObject(exec);
		}
	}
	

	private void waitExecution() {
		if (System.console()!=null) {
			Log.i("Press any key to stop");
			System.console().readLine();
		} else {
			int timeout = 2 * 60 * 1000; 
			Log.i("Can`t get Console, wait "+(timeout / 1000)+" sec");
			try {
				Thread.sleep(timeout);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private ExecutorService handler;
	private Runnable checkErrors = new Runnable() {
		
		@Override
		public void run() {
			controller.checkProblems();
		}
	};
		
	@Override
	public boolean test() {
		Log.i("Start test");
		handler = Executors.newSingleThreadExecutor();
		controller = new UpdateThreadController("test");
		Log.i("Controller configure");
		
		controller.setConfig_maxUpdateTime(10000);
		controller.setConfig_timerInterval(500);
		controller.setConfig_checkThreadsInterval(2000);
		controller.setConfig_timerInterval(2000);
		
		Log.i("Start controller");
		controller.startController();
		Log.i("Fill data");
		fillData();
		waitExecution();
		Log.i("Wait for stopp controoler");
		controller.stopController();
//		waitExecution();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		handler.shutdown();
		Log.i("Complete");
		return true;
	}
	
	public static void main(String[] args) {
		BaseTest.test(ThreadControllerTest.class);
	}
}
