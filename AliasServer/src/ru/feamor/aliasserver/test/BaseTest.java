package ru.feamor.aliasserver.test;

import ru.feamor.aliasserver.utils.Log;

public class BaseTest {
	
	public String resultsMessage = "";
	
	public boolean test() {
		return true;
	}
	
	public static void test(Class<? extends BaseTest> test) {
		Log.initialize("config/log4j_test.xml");
		Log.i(ComplexTest.class, "Start Complex Test");
		try {
			BaseTest testingObject = test.newInstance();
			if (testingObject.test()) {
				Log.i(ComplexTest.class, "Test Success! Message = "+testingObject.resultsMessage);
			} else {
				Log.e(ComplexTest.class, "Test Fail! Message = "+testingObject.resultsMessage);
			}
		} catch (Throwable ex) {
			Log.e(ComplexTest.class, "Complex test error", ex);
		} 
		Log.i(ComplexTest.class, "Complex test finish");
	}
}
