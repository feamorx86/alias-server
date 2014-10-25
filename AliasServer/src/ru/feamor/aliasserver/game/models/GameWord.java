package ru.feamor.aliasserver.game.models;

public class GameWord {
	
	private long id;
	private GameDictionary dictionary;
	private String word;
	private String hint;
	private GameWordDifficult dificult;
	
	public GameWord() {
		
	}
	
	public GameDictionary getDictionary() {
		return dictionary;
	}
	
	public void setDictionary(GameDictionary dictionary) {
		this.dictionary = dictionary;
	}
	
	public GameWordDifficult getDificult() {
		return dificult;
	}
	
	public void setDificult(GameWordDifficult dificult) {
		this.dificult = dificult;
	}
	
	public String getHint() {
		return hint;
	}
	
	public void setHint(String hint) {
		this.hint = hint;
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getWord() {
		return word;
	}
	
	public void setWord(String word) {
		this.word = word;
	}	
}
