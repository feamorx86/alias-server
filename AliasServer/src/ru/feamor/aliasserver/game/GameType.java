package ru.feamor.aliasserver.game;

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
	
	public abstract BaseGame createGame();
	
	public abstract String getAlias();
}
