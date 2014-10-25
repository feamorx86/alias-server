package ru.feamor.aliasserver.core;

import org.json.JSONObject;

import ru.feamor.aliasserver.config.Config;
import ru.feamor.aliasserver.config.ConfigurationFactory;

public class Component {
	private ComponentDefine define;
	
	public Component() {
		
	}
	
	public void init(ComponentDefine define) {
		if (define.compenent != null) {
			throw new RuntimeException("Fail to set component instance for class="+getClass().getSimpleName()+", Instance already set");
		} else {
			this.define = define;
			define.compenent = this;
		}
	}
	
	public int getUid() {
		return define.id;
	}
	
	public String getAlias() {
		return define.alias;
	}
	
	public void create() {
		
	}
	
	public void config(JSONObject config) {
		ConfigurationFactory.configure(this, config, getAlias());
	}
	
	public void onAdded() {
		
	}
	
	public void onError(Object ... args) {
		
	}
	
	public void onStart() {
		
	}
	
	public void onRemoved() {
		
	}
	
	public void onDestroy() {
		
	}
 
}
