package ru.feamor.aliasserver.config;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.feamor.aliasserver.game.assignment.GamePlayersLevelAssignmentGroup;
import ru.feamor.aliasserver.game.types.GameTypeCollector;
import ru.feamor.aliasserver.utils.Log;

public class GameTypeCollectorConfig implements Config<GameTypeCollector> {
	
	private HashMap<String,Class> groupsByName;
	
	public GameTypeCollectorConfig() {
		groupsByName = new HashMap<String, Class>();
		addGroup("Level.simple", GamePlayersLevelAssignmentGroup.class);
		addGroup("Level.normal", GamePlayersLevelAssignmentGroup.class);
		addGroup("Level.hard", GamePlayersLevelAssignmentGroup.class);
	}
	
	private void addGroup(String name, Class groupClass) {
		groupsByName.put(name, groupClass);		
	}
	
	@Override
	public void configure(JSONObject config, GameTypeCollector typeCollector) {
		JSONArray gameTypes =config.optJSONArray("gameTypes");
		int typesAdded = 0;
		if (gameTypes == null || gameTypes.length() == 0) {
			Log.e(GameTypeCollectorConfig.class, "There is no one game type!");
		} else {
			for (int i=0; i<gameTypes.length(); i++) {
				JSONObject gameType = (JSONObject) gameTypes.opt(i);
				String name = gameType.optString("name");
				if (name == null) {
					Log.e(GameTypeCollectorConfig.class, "Can`t get name of Assignment. Looks like incorrect config. JSON="+gameType.toString());							
				} else {
					Class groupClass = null; 
					if ("CustomClass".equalsIgnoreCase(name)) {
						String customClassName = config.optString("class", null);
						if (customClassName != null) {
							try {
								groupClass = Class.forName(name);
							} catch(Exception ex) {
								Log.e(GameTypeCollectorConfig.class, "Can`t get CustomClass for name = "+name, ex);
								groupClass = null;
							}
						} else {
							Log.e(GameTypeCollectorConfig.class, "CustomClass name is empty");
						}						
					} else {
						groupClass = groupsByName.get(name);
						if (groupClass == null) {
							Log.e(GameTypeCollectorConfig.class, "Can`t find class for name = "+name);
						}
					}
					
					if (groupClass == null) {
						Log.e(GameTypeCollectorConfig.class, "Fail to create GameType for json="+gameType);
					} else {
						Object testNewGroup = null;
						try {
							testNewGroup = groupClass.newInstance();
						}catch (Exception ex) {
							Log.e(GameTypeCollectorConfig.class, "fail to create { name = "+name+", class = "+groupClass.getSimpleName()+"}");
							testNewGroup = null;
						}
						if (testNewGroup != null) {
							if (testNewGroup instanceof GamePlayersLevelAssignmentGroup) {
								GamePlayersLevelAssignmentGroup newgroup = (GamePlayersLevelAssignmentGroup)testNewGroup;
								JSONObject typeConfig = gameType.optJSONObject("config");
								if (typeConfig == null) {
									Log.e(GameTypeCollectorConfig.class, "Threr is no Config for { name = "+name+", class = "+groupClass.getSimpleName()+"}, but class added!");
									typeCollector.addAssignment(newgroup);
									typesAdded++;
								} else {
									try {
										newgroup.configure(typeConfig);
										typeCollector.addAssignment(newgroup);
										typesAdded++;
									}catch(Exception ex) {
										Log.e(GameTypeCollectorConfig.class, "Can`t configure: { name = "+name+", class = "+groupClass.getSimpleName()+"}", ex);
									}
								}
							} else {
								Log.e(GameTypeCollectorConfig.class, "Created class for { name = "+name+", class = "+groupClass.getSimpleName()+"} is not subclass of GamePlayersLevelAssignmentGroup");
							}
						}							 
					}
				}
			}
			Log.i(GameTypeCollectorConfig.class, "Added: "+typesAdded+" game types");
		}
	}
}
