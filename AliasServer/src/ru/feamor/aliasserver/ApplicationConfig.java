package ru.feamor.aliasserver;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.feamor.aliasserver.commands.CommandTypes;
import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.utils.Log;

public class ApplicationConfig {
	private JSONObject config;
	private String filePath;
	
	public ApplicationConfig() {
		config = null;
	}
	
	public void fromFile(String filePath) {
		Log.i(ApplicationConfig.class, "use configuration: "+filePath);
		this.filePath = filePath;
		String jsonConfig = null;
		config = null;
		try {
			File configFile = new File(filePath);
			jsonConfig = FileUtils.readFileToString(configFile);
		} catch (IOException e) {
			Log.e(ApplicationConfig.class,"fail to read config, path="+filePath, e);
		} 
		try {
			config = new JSONObject(jsonConfig);
		} catch (JSONException e) {
			Log.e(ApplicationConfig.class, "fail to parse config, path="+filePath, e);
			config = null;
		}
	}
	
	public Object optProperty(String name) {
		return config.opt(name);
	}

	public String optStringProperty(String name, String devaultValue) {
		return config.optString(name, devaultValue);
	}
	
	public int optIntProperty(String name, int devaultValue) {
		return config.optInt(name, devaultValue);
	}
	
	public JSONObject optJSONProperty(String name) {
		return config.optJSONObject(name);
	}
	
	public JSONArray optJSONProperties(String name) {
		return config.optJSONArray(name);
	}
		
	public JSONObject getConfig() {
		return config;
	}
	
	public String getUsedConfigPath() {
		return filePath;
	}
	
}
