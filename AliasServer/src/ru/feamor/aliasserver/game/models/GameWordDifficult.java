package ru.feamor.aliasserver.game.models;

public class GameWordDifficult {
	private long id;
	private int value;
	private String description;
	
	public GameWordDifficult() {
		
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public int getValue() {
		return value;
	}
	
	public void setValue(int value) {
		this.value = value;
	}
}
