package ru.feamor.aliasserver.test;

import java.io.Console;
import java.util.Random;

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

		public TestExecuted(int count, int minWait, int maxWait, String name) {
			super();
			this.count = count;
			this.minWait = minWait;
			this.maxWait = maxWait;
			this.name = name;
			Log.i("ThreadObject", "Create: <"+name+"> ("+count+"), sleep "+minWait+" - "+maxWait+" ms");
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
				Log.i("ThreadObject", "Complete <"+name+"> ");
			} else {
				int delta = maxWait - minWait;
				delta = rand.nextInt(delta) + minWait;
				if (delta > 0) {
					try {
						Log.i("ThreadObject", "Executed <"+name+"> ("+count+"), will sleep "+delta+" ms");
						Thread.sleep(delta);
					} catch (InterruptedException e) {
						e.printStackTrace();
						throw e;
					}
				}
				count--;
			}
		}		
	}
	
	void fillData() {
		Random r = new Random();
		for (int i = 0; i < 20; i++) {
			int minWait = r.nextInt(500)+100;
			int maxWait = r.nextInt(1000)+200;
			int count = r.nextInt(30)+3;
			String name = "Obj-"+i;			
			TestExecuted exec = new TestExecuted(count, minWait, maxWait, name);
			controller.addUpdateObject(exec);
		}
	}
	

	private void waitExecution() {
		if (System.console()!=null) {
			Log.i("Press any key to stop");
			System.console().readLine();
		} else {
			int timeout = 10 * 1000; 
			Log.i("Can`t get Console, wait "+(timeout / 1000)+" sec");
			try {
				Thread.sleep(timeout);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
		
	@Override
	public boolean test() {
		Log.i("Start test");
		controller = new UpdateThreadController();
		Log.i("Configure");
		controller.configure(new JSONObject());
		Log.i("Strt controller");
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
		Log.i("Complete");
		return true;
	}
	
	public static void main(String[] args) {
		BaseTest.test(ThreadControllerTest.class);
	}
}
