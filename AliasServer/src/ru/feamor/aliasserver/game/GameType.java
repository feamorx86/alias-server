package ru.feamor.aliasserver.game;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.json.JSONObject;

import ru.feamor.aliasserver.games.BaseGame;

public abstract class GameType {
	
	protected int id;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void configure(JSONObject config) {
		
	}
	
	public abstract BaseGame createGame(Object params);
	
	public abstract String getAlias();
	
	public static abstract class GameTypeWithFixedPlayersCount extends GameType {
		public abstract int getPlayersCount();
		
		@Override
		public BaseGame createGame(Object params) {
			if (params == null || !(params instanceof DoubleLinkedList)) {
				throw new IllegalArgumentException("Create game: params not exist or params is not a DoubleLinkedList!");
			}
			return createGameWithPlayers((DoubleLinkedList)params);
		}
		
		protected abstract BaseGame createGameWithPlayers(DoubleLinkedList players);
	}
}
