package ru.feamor.aliasserver.config;

import java.util.HashMap;

import org.json.JSONObject;

import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.game.types.GameTypeCollector;
import ru.feamor.aliasserver.utils.Log;
import ru.feamor.aliasserver.utils.TextUtils;

public class ConfigurationFactory {
	
	private static ConfigurationFactory instance;
	
	private HashMap<Class, Config> configurations;
	
	private ConfigurationFactory() {
		configurations = new HashMap<Class, Config>();
	}
	
	public static ConfigurationFactory get() {
		assert instance != null;
		return instance;
	}
		
	public static void initialize() {
		Log.i(ConfigurationFactory.class, "Initialize configuration factory");
		if (instance != null) {
			throw new RuntimeException("Can`t create, Singletone instance already created");
		} else {
			instance = new ConfigurationFactory();
		}		
	}
	
	public Config<?> getConfig(Class objectClass) {
		Config result = configurations.get(objectClass);
		return result;
	}
	
	public static Config<?> get(Class objectClass) {
		return get().getConfig(objectClass);
	}
	
	public static void configure(Object objectToConfigure, JSONObject config) {
		configure(objectToConfigure, config, null);
	}
	
	public static void configure(Object objectToConfigure, JSONObject config, String propertyName) {
		if (TextUtils.isNotEmpty(propertyName)) {
			config = config.optJSONObject(propertyName);
		}
		if (objectToConfigure != null && config != null) {
			Class configableClass = objectToConfigure.getClass();
			Config configurator = get().getConfig(configableClass);
			if (configurator != null) {
				try {
					configurator.configure(config, objectToConfigure);
				} catch(Throwable e) {
					Log.e(ConfigurationFactory.class, "Can`t configure class="+configableClass.getSimpleName()+", json="+config.toString(), e);
				}
			} else {
				Log.e(ConfigurationFactory.class, "Can`t configure. There is no configuration for class="+configableClass.getSimpleName());
			}
		} else {
			Log.e(ConfigurationFactory.class, "Can`t configure. Object or Config is null");
		}
	}
	
	public void configure(JSONObject config) {
		Log.i(ConfigurationFactory.class, "Start configuration");
		
		JSONObject setup = config.optJSONObject("ConfigurationFactory");
		if (setup != null) {
			//TODO: Add exclude configurations
			//TODO: Add include configurations
		}
		configurations.put(GameTypeCollector.class, new GameTypeCollectorConfig());
		configurations.put(GameManager.class, new GameManagerConfig());
		configurations.put(NettyConfig.class, new NettyConfig());
		
		Log.i(ConfigurationFactory.class, "Configuration complete");
	}
	
}
