package ru.feamor.aliasserver.game;

import java.util.HashMap;

public class GameWordsSequence {
	private HashMap<Long, Integer> usedWords;
	
	public GameWordsSequence() {
		usedWords = new HashMap<Long, Integer>();
	}
	
	public int addWord(long id) {
		Integer count = usedWords.get(id);
		int used = 1;
		if (count != null) {
			used = count +1;
		}
		usedWords.put(id, used);
		return used;
	}
	
	public int getCount(long id) {
		Integer count = usedWords.get(id);
		int result = 0;
		if (count != null) {
			result = count;
		}
		return result;
	}
}
