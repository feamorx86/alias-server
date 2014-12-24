package ru.feamor.aliasserver.components;

import java.util.HashMap;

import ru.feamor.aliasserver.core.Component;
import ru.feamor.aliasserver.core.ComponentDefine;

public class Components {
	
	public static ComponentDefine dbManager;
	public static ComponentDefine nettyManager;
	public static ComponentDefine gameManager;
	public static ComponentDefine timeManager;
	
	public static void initialize() {
		componentsById = new HashMap<Integer, ComponentDefine>();
		componentsByAlias = new HashMap<String, ComponentDefine>();
		lastId = 0;
		
		dbManager = addCompomemt(DBManager.class, "db");
		nettyManager = addCompomemt(NettyManager.class, "netty");
		gameManager = addCompomemt(GameManager.class, "game");
		timeManager = addCompomemt(TimeManager.class, "time");
	}
			
	public static ComponentDefine getDefine(int id) {
		return componentsById.get(id);
	}
	
	public static ComponentDefine getDefine(String alias) {
		if (alias!=null) {
			return componentsByAlias.get(alias);
		} else {
			return null;
		}
	}
	
	public static Class<Component> getClass(int id) {
		ComponentDefine define = componentsById.get(id);
		if (define!=null) {
			return define.usedClass;
		} else {
			return null;
		}
	}
	
	public static Class<Component> getClass(String alias) {
		if (alias != null) {
			ComponentDefine define = componentsByAlias.get(alias);
			if (define!=null) {
				return define.usedClass;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	public static Component get(int id) {
		ComponentDefine define = componentsById.get(id);
		if (define!=null) {
			return define.compenent;
		} else {
			return null;
		}
	}
		
	public static Component get(String alias) {
		ComponentDefine define = componentsByAlias.get(alias);
		if (define!=null) {
			return define.compenent;
		} else {
			return null;
		}
	}

	private static HashMap<Integer, ComponentDefine> componentsById;
	private static HashMap<String, ComponentDefine> componentsByAlias;
	private static int lastId;
	
	private static ComponentDefine addCompomemt(Class<?> usedClass, String alias) {
		ComponentDefine define = new ComponentDefine();
		define.alias = alias;
		define.id = lastId;
		lastId++;
		define.usedClass = (Class<Component>) usedClass;
		componentsById.put(define.id, define);
		componentsByAlias.put(define.alias, define);
		return define;
	}
}
