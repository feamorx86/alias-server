package ru.feamor.aliasserver.components;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import ru.feamor.aliasserver.game.GameType;
import ru.feamor.aliasserver.games.BaseGame;
import ru.feamor.aliasserver.utils.Log;

public class GamesFactory {
	private TIntObjectHashMap<GameType> gameTypes = new TIntObjectHashMap<GameType>();
	private GameClassificationNode classifications = null;
	private HashMap<String, Class<?>> gameTypeClasses = new HashMap<>();
	
	public GamesFactory() {
	
	}
	
	public void configure(JSONObject json) {
		registerAliases(json);
		loadGameTypesFromJson(json.optJSONArray("types"));
		JSONObject classificationJson = json.optJSONObject("classification");
		if (classificationJson != null) {
			loadClassificationFromJson(classificationJson);
		}
	}
	
	public synchronized void registerGameType(GameType gameType){
		gameTypes.put(gameType.getId(), gameType);
	}
	
	public GameType getType(int id) {
		return gameTypes.get(id);
	}
	
	public BaseGame createGame(int typeId, Object params) {
		GameType type = getType(typeId);
		BaseGame game = null;
		if(type != null) {
			try {
				game = type.createGame(params);
			} catch(Exception ex) {
				Log.e(GamesFactory.class, "Failt to create game for type, id="+typeId+", alias ="+type.getAlias(), ex);
				game = null;
			}
		}
		return game;
	}
		
	public synchronized GameType unregisterGameType(int gameTypeId){
		return gameTypes.remove(gameTypeId);
	}
	
	private void registerAliases(JSONObject config) {
//		gameTypeClasses.put(AuthorizationGameType.Alias, AuthorizationGameType.class);
	}
	
	public void loadGameTypesFromJson(JSONArray jsonTypes) {
		for (int i = 0; i < jsonTypes.length(); i++) {
			JSONObject json = jsonTypes.optJSONObject(i);
			String className = json.optString("alias");
			int id = json.optInt("id", 0);
			Class<?> typeClass = gameTypeClasses.get(className);
			if (typeClass == null) {
				Log.e(GamesFactory.class, "Can`t get class for game type = "+className);
			} else {
				try {
					GameType gameType = (GameType) typeClass.newInstance();
					gameType.setId(id);
					gameType.configure(json);
					gameTypes.put(gameType.getId(), gameType);					
				} catch (Exception ex) {
					Log.e(GamesFactory.class, "Fail to create instance for game type = "+className+", class = "+typeClass.getSimpleName(), ex);
				}
				
			}
		}
	}
	
	public void loadClassificationFromJson(JSONObject json) {
		Stack<ImmutablePair<JSONObject, GameClassificationNode>> stack = new Stack<>();
		classifications = new GameClassificationNode();
		stack.add(new ImmutablePair<JSONObject, GameClassificationNode>(json, classifications));		
		while (stack.size() > 0 ) {
			Pair<JSONObject, GameClassificationNode> curr = stack.pop();
			//load node data
			curr.getRight().id = curr.getLeft().optInt("id", -1);
			curr.getRight().name = curr.getLeft().optString("name", "");
			curr.getRight().description = curr.getLeft().optString("description", "");
			//supported game types
			JSONArray typesArray = curr.getLeft().optJSONArray("types");
			if (typesArray != null && typesArray.length() > 0) {
				curr.getRight().gameTypes = new ArrayList<>(typesArray.length());
				for (int i = 0; i < typesArray.length(); i++) {
					curr.getRight().gameTypes.add(typesArray.getInt(i));
				}
			} 
			
			JSONArray subNudes = curr.getLeft().optJSONArray("nodes");
			if (subNudes!=null&& subNudes.length() > 0) {
				for (int i = 0; i < subNudes.length(); i++) {
					GameClassificationNode childNode = new GameClassificationNode();
					childNode.setParent(curr.getRight());
					curr.getRight().getNodes().add(childNode);
					stack.push(new ImmutablePair<JSONObject, GameClassificationNode>(subNudes.getJSONObject(i), childNode));
				}
			}			
			
		}
	}
	
	public static class GameClassificationNode {
		private int id;
		private String name;
		private String description;
		
		private ArrayList<GameClassificationNode> nodes;
		private ArrayList<Integer> gameTypes;
		private GameClassificationNode parent;
		
		public GameClassificationNode() {
			id = -1;
			name = "";
			description = "";
			nodes = new ArrayList<GameClassificationNode>();
			gameTypes = new ArrayList<Integer>();
			parent = null;
		}		
		
		public GameClassificationNode getParent() {
			return parent;
		}
		
		public void setParent(GameClassificationNode parent) {
			this.parent = parent;
		}
		
		public int getId() {
			return id;
		}
		
		public void setId(int id) {
			this.id = id;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getDescription() {
			return description;
		}
		
		public void setDescription(String description) {
			this.description = description;
		}
		
		public ArrayList<GameClassificationNode> getNodes() {
			return nodes;
		}
		
		public void setNodes(ArrayList<GameClassificationNode> nodes) {
			this.nodes = nodes;
		}
		
		public ArrayList<Integer> getGameTypes() {
			return gameTypes;
		}
		
		public void setGameTypes(ArrayList<Integer> gameTypes) {
			this.gameTypes = gameTypes;
		}
	}
}
