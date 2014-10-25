package ru.feamor.aliasserver.game.models;

import java.util.HashMap;

public class GameDictionary {
	private long id;
	private String name;
	private int type;
	private HashMap<Long, GameWord> words;
	
	public GameDictionary() {
		words = new HashMap<>();
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public HashMap<Long, GameWord> getWords() {
		return words;
	}
	
	public void setWords(HashMap<Long, GameWord> words) {
		this.words = words;
	}
	
	public GameWord getWord(long id) {
		return words.get(id);
	}
	
	public void addWord(GameWord word) {
		words.put(word.getId(), word);
	}
}
