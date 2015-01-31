package ru.feamor.aliasserver.game.assignment;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;
import org.json.JSONObject;

import ru.feamor.aliasserver.components.TimeManager;
import ru.feamor.aliasserver.game.GamePlayer;
import ru.feamor.aliasserver.game.GameTags;
import ru.feamor.aliasserver.users.GameClient;
import ru.feamor.aliasserver.utils.Log;

public class GamePlayersAssignmentGroup {
	private int groupId;
	protected DoubleLinkedList players;
	
	public GamePlayersAssignmentGroup(int groupId) {
		this.groupId = groupId;
		players = new DoubleLinkedList();
	}
	
	public synchronized void addPlayer(GameClient player) {
		PlayerInLevelGroup playerInLevelGroup = new PlayerInLevelGroup(player);
		players.addLast(playerInLevelGroup.getGroupNode());
	}
	
	public synchronized void removePlayer(GameClient player) {
		PlayerInLevelGroup group = (PlayerInLevelGroup) player.getTag(GameTags.TAG_GAME_PLAYER_LEVEL_GROUP_NODE);
		if (group == null) {
			Log.e(GamePlayersAssignmentGroup.class, "can`t get PlayerInGroup from tags for player id="+player.getPlayer().getId());
		} else {
			players.remove(group.getGroupNode());
		}		
	}
	
	public int getGroupId() {
		return groupId;
	}
	
	public DoubleLinkedList getPlayers() {
		return players;
	}
	
	public int getPlayersCount() {
		return players.size();
	}
	
	public void configure(JSONObject config) {
		
	}
	
	public boolean isPlayerAssignable(GamePlayer player) {
		return true;
	}
	
	public boolean tryToCollectGroup(int maxSize, DoubleLinkedList group) {
			if (maxSize < players.size()) {
				return false;
			} else {
				for(int i=0; i<maxSize; i++) {
					PlayerInLevelGroup playerInGroup = (PlayerInLevelGroup) players.getFirst().getPayload();
					players.remove(players.getFirst());
					group.addLast(playerInGroup.getGroupNode());
				}
				return true;
			}		
	}
	
	public String getName() {
		return "default";
	}
	
	public int tryToAddPlayersToGroup(GamePlayersAssignmentGroup startGroup, int need, DoubleLinkedList collected) {
		return 0;
	}
}
