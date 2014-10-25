package ru.feamor.aliasserver.game;

import java.util.HashMap;

public class GameTeam {
	private long id;
	private int Color;
	private String Name;
	private GameSession sessing;
	private HashMap<Long, GamePlayer> players;
	private GameWordsSequence wordsSequence;
}
