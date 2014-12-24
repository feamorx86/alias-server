package ru.feamor.aliasserver.game.types;

import java.util.HashMap;

import org.json.JSONObject;

import ru.feamor.aliasserver.base.Config;
import ru.feamor.aliasserver.base.Configurable;
import ru.feamor.aliasserver.config.ConfigurationFactory;
import ru.feamor.aliasserver.config.GameTypeCollectorConfig;
import ru.feamor.aliasserver.game.GamePlayer;
import ru.feamor.aliasserver.game.assignment.GamePlayersAssignmentGroup;
import ru.feamor.aliasserver.game.assignment.GamePlayersLevelAssignmentGroup;


public class GameTypeCollector {
	private HashMap<Integer, GamePlayersAssignmentGroup> groups;
	
	public GameTypeCollector() {
		groups = new HashMap<Integer, GamePlayersAssignmentGroup>();
	}
	
	public void addAssignment(GamePlayersLevelAssignmentGroup group) {
		groups.put(Integer.valueOf(group.getGroupId()), group);
		
	}
	
	public GamePlayersAssignmentGroup getAssignment(int groupId) {
		GamePlayersAssignmentGroup assignmentGroup = groups.get(Integer.valueOf(groupId));
		return assignmentGroup;
	}
	
	public void configure(JSONObject config) {
		Config configurator = ConfigurationFactory.get(getClass());
		configurator.configure(config, this);
	}
	
	public void update(float delta) {
		
	}
	
	public void addPlayer(GamePlayer player) {
		
	}
}