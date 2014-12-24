package ru.feamor.aliasserver.game;

public class GameTags {
	public static final int TAG_GAME_PLAYER_LEVEL_GROUP_NODE;
	public static final int TAG_GAME_CLIENT_NEW_PLAYER_NODE;
	public static final int TAG_GAME_PLAYER_POLL_NODE;
	public static final int TAG_SIMPLE_CHAT_PLAYER_NODE;
	
	static {
		int startTag = 0;
		TAG_GAME_PLAYER_LEVEL_GROUP_NODE = startTag; startTag++; 
		TAG_GAME_CLIENT_NEW_PLAYER_NODE = startTag; startTag++;
		TAG_GAME_PLAYER_POLL_NODE = startTag; startTag++;
		TAG_SIMPLE_CHAT_PLAYER_NODE = startTag; startTag++;
	}
}
