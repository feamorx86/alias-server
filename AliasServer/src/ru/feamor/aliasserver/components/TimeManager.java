package ru.feamor.aliasserver.components;

import java.util.Calendar;

import org.json.JSONObject;

import ru.feamor.aliasserver.core.Component;

public class TimeManager  extends Component {
	
	public static TimeManager get() {
		return (TimeManager)Components.timeManager.compenent;
	}
	
	public synchronized long getNow() {
		return Calendar.getInstance().getTimeInMillis();
	}
	
	@Override
	public void config(JSONObject config) {
		//there is no configuration
	}
}
