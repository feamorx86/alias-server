package ru.feamor.aliasserver.game.assignment;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.json.JSONObject;

import ru.feamor.aliasserver.game.GamePlayer;
import ru.feamor.aliasserver.utils.Log;

public class GamePlayersLevelAssignmentGroup extends GamePlayersAssignmentGroup {
	private int minLevel, maxLevel, levelIntersection;
	
	public GamePlayersLevelAssignmentGroup(int groupId) {
		super(groupId);
		minLevel = -1;
		maxLevel = -1;
		levelIntersection = 0;
	}
	
	public void setLevels(int maxLevel, int minLevel) {
		this.minLevel = minLevel;
		this.maxLevel = maxLevel;
	}
	
	@Override
	public void configure(JSONObject config) {
		minLevel = config.optInt("min", -1);
		maxLevel = config.optInt("max", -1);
		levelIntersection= config.optInt("intersection", 0);
	}
	
	public int getMinLevel() {
		return minLevel;
	}
	
	public int getMaxLevel() {
		return maxLevel;
	}
	
	@Override
	public boolean isPlayerAssignable(GamePlayer player) {
		boolean result = (player.getLevel() >= minLevel && player.getLevel()<= maxLevel);
		return result;
	}
	
	@Override
	public int tryToAddPlayersToGroup(GamePlayersAssignmentGroup startGroup, int need,
			DoubleLinkedList collected) {
		int result = 0;
		if (players.size() > 0 ) {
			if (startGroup instanceof GamePlayersLevelAssignmentGroup) {
				GamePlayersLevelAssignmentGroup otherGroup = (GamePlayersLevelAssignmentGroup) startGroup;
				int veryMinLevel = minLevel - levelIntersection;
				int vertMaxLevel = maxLevel + levelIntersection;
				if ( (otherGroup.getMinLevel() >= veryMinLevel && otherGroup.getMinLevel() <= vertMaxLevel) ||
					 (otherGroup.getMaxLevel() >= veryMinLevel && otherGroup.getMaxLevel() <= vertMaxLevel)) {
					int howMany = Math.min(need, getPlayersCount());
					if (!tryToCollectGroup(howMany, collected)) {
						Log.e(GamePlayersLevelAssignmentGroup.class, "want to collect more items then have");
					}
					result = howMany;
				} 
			}
		}
		return result;
	}
}
