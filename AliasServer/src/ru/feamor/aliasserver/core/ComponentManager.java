package ru.feamor.aliasserver.core;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.feamor.aliasserver.components.Components;
import ru.feamor.aliasserver.utils.Log;


public class ComponentManager  {
	
	private HashMap<Integer, Component> components;
	
	
	private ComponentManager() {
		components = new HashMap<>();
	}
	
	public Component getComponent(int id) {
		return components.get(Integer.valueOf(id));
	}
	
	/***
	 * Singletone Implementation
	 */
	private static ComponentManager instance;
	
	public static ComponentManager get() {
		assert instance != null;
		return instance;
	}
	
	public static void initialize() {
		if (instance != null) {
			throw new RuntimeException("Can`t create, Singletone instance already created");
		} else {
			instance = new ComponentManager();
		}		
	}
	
	public void configure(JSONObject config) {
		Log.i(ComponentManager.class, "Start compomemts configuration");
		JSONArray usedComponents = config.getJSONArray("components");
		Log.i(ComponentManager.class, "Use next components:");
		if (usedComponents != null) {
			for (int i=0; i<usedComponents.length(); i++) {
				String alias = usedComponents.getString(i);
				ComponentDefine define = Components.getDefine(alias);
				if (define == null) {
					Log.e(ComponentManager.class, "incorrect alias = `"+alias+"`");
				} else {
					Component component = constructComponent(define);
					if (component != null) {
						Log.i(ComponentManager.class, "Added component. Alias = `"+alias+"`, class = "+define.usedClass.getSimpleName());
						components.put(define.id, define.compenent);
					}
				}
			}
		} else {
			Log.i(ComponentManager.class, "No Components to use!");
		}
		Log.i(ComponentManager.class, "Adding complete. "+components.size()+" components added!");
		
		Log.i(ComponentManager.class, "Start `Create` for components");
		ArrayList<Integer> toRemove = new ArrayList<>();
		for (Component component : components.values()) {
			try {
				component.create();
			} catch(Exception ex) {
				toRemove.add(component.getUid());
				Log.e(ComponentManager.class, "Fail to call `create` for component: { name="+component.getAlias()+", uid="+component.getUid()+"}", ex);
				//TODO: add error code / type
				component.onError(ex);
			}
		}
		for(int i=0; i<toRemove.size(); i++) {
			Component component = getComponent(toRemove.get(i).intValue());
			component.onRemoved();
			components.remove(toRemove.get(i).intValue());
			component.onDestroy();
		}
		toRemove.clear();
		
		Log.i(ComponentManager.class, "Components created, start configure");		
		for (Component component : components.values()) {
			try {
				component.config(config);
			} catch(Exception ex) {
				toRemove.add(component.getUid());
				Log.e(ComponentManager.class, "Fail to call `config` for component: { name="+component.getAlias()+", uid="+component.getUid()+"}", ex);
				//TODO: add error code / type
				component.onError(ex);
			}
		}
		
		for(int i=0; i<toRemove.size(); i++) {
			Component component = getComponent(toRemove.get(i).intValue());
			component.onRemoved();
			components.remove(toRemove.get(i).intValue());
			component.onDestroy();
		}
		toRemove.clear();
		
		for (Component component : components.values()) {
			try {
				component.onAdded();
			} catch(Exception ex) {
				toRemove.add(component.getUid());
				Log.e(ComponentManager.class, "Fail to call `onAdded` for component: { name="+component.getAlias()+", uid="+component.getUid()+"}", ex);
				//TODO: add error code / type
				component.onError(ex);
			}
		}
		
		for(int i=0; i<toRemove.size(); i++) {
			Component component = getComponent(toRemove.get(i).intValue());
			component.onRemoved();
			components.remove(toRemove.get(i).intValue());
			component.onDestroy();
		}
		toRemove.clear();
		
		Log.i(ComponentManager.class, "Components Configurated!");		
	}
	
	public void start() {
		Log.i(ComponentManager.class, "Start compomemts.");
		ArrayList<Integer> toRemove = new ArrayList<>();
		for (Component component : components.values()) {
			try {
				component.onStart();
			} catch(Exception ex) {
				toRemove.add(component.getUid());
				Log.e(ComponentManager.class, "Fail to call `onStart` for component: { name="+component.getAlias()+", uid="+component.getUid()+"}", ex);
			}
		}
		
		for(int i=0; i<toRemove.size(); i++)
			components.remove(toRemove.get(i).intValue());
		toRemove.clear();
		Log.i(ComponentManager.class, "Comonents started!");	
	}
	
	private Component constructComponent(ComponentDefine define) {
		Component component = null;
		try {
			component = define.usedClass.newInstance();
			component.init(define);
		} catch(Exception e) {
			Log.e(ComponentManager.class, "Fail to construct component, alias = `"+define.alias+"`", e);
			component = null;
		}
		return component;
	}
}
