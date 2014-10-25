package ru.feamor.aliasserver.game.assignment;

import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.components.TimeManager;
import ru.feamor.aliasserver.game.GameClient;
import ru.feamor.aliasserver.game.GamePlayer;
import ru.feamor.aliasserver.game.GameTags;

public class PlayerInLevelGroup {
	private GameClient player;
	private DoubleLinkedListNode groupNode;
	private double addingTime;
	
	public PlayerInLevelGroup(GameClient player) {
		this.player = player;
		groupNode = new DoubleLinkedListNode(this);
		addingTime = TimeManager.get().getNow();
		player.putTag(GameTags.TAG_GAME_PLAYER_LEVEL_GROUP_NODE, this);
	}
	
	public DoubleLinkedListNode getGroupNode() {
		return groupNode;
	}
	
	public double getAddingTime() {
		return addingTime;
	}
	
	public GameClient getPlayer() {
		return player;
	}
}