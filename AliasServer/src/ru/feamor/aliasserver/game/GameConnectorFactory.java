package ru.feamor.aliasserver.game;

import gnu.trove.map.hash.TIntObjectHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.feamor.aliasserver.game.UsersPool.SimpleCoutGameConnector;
import ru.feamor.aliasserver.utils.Log;

public class GameConnectorFactory {		
	private TIntObjectHashMap<GameConnectorCreator> connectorCreators= new TIntObjectHashMap<GameConnectorCreator>();
	private TIntObjectHashMap<Class<?>> connectorCreatorClasses = new TIntObjectHashMap<Class<?>>();
	
	public GameConnector createConnector(int id, Object params) {
		GameConnectorCreator creator = connectorCreators.get(id);
		GameConnector result = null;
		if (creator!=null) {
			result = creator.createConnector(params);
		} else {
			if (connectorCreatorClasses.contains(id)) {
				Log.i(GameConnectorFactory.class, "Connector class for id = "+id+", exist but ConnectorCreater not exist in config");
			}
		}
		return result;
	}
	
	
	public GameConnectorFactory () {
		
	}
	
	public static final int INVALID_CONNECTOR_CREATOR_ID = -1;
	
	public void configure(JSONObject config) {
		putConnectorClasses();
		//TODO: add 'unregister' some unused classes.
		JSONArray creatorsJson = config.optJSONArray("creators");
		if (creatorsJson != null) {
			for (int i=0; i<creatorsJson.length(); i++) {
				JSONObject creatorJson = creatorsJson.optJSONObject(i);
				if (creatorJson!=null) {
					int id = creatorJson.optInt("id", INVALID_CONNECTOR_CREATOR_ID);
					if (id != INVALID_CONNECTOR_CREATOR_ID) {
						Class<?> clazz = null;
						try {
							clazz = connectorCreatorClasses.get(id);
							if (clazz!=null) {
								GameConnectorCreator creator = (GameConnectorCreator) clazz.newInstance();
								creator.configure(creatorJson);
								connectorCreators.put(creator.getId(), creator);
							} else {
								Log.e(GameConnectorFactory.class, "Configure: requested class not register, id ="+id);
							}
						} catch (Throwable e) {
							Log.e(GameConnectorFactory.class, "Configure: Can`t create Creator for id ="+id+", class ="+clazz.getSimpleName());
						}
					} else {
						Log.e(GameConnectorFactory.class, "Configure: thre is no `id` field in json ="+creatorJson.toString());
					}
				}
			}
		}
	}
	
	private void putConnectorClasses() {
		registerCreatorClass(SimpleCoutGameConnector.class, 0);
	}
		
	public void registerCreatorClass(Class<?> connectorCreatorClass, int id) {
		connectorCreatorClasses.put(id, connectorCreatorClass);
	}
	
	public Class<?> unregisterCreatorClass(int id) {
		return connectorCreatorClasses.remove(id);
	}
	
	public static abstract class GameConnectorCreator {	
		public abstract void configure(JSONObject json);
		public abstract GameConnector createConnector(Object params);
		public abstract int getId();
	}
}
