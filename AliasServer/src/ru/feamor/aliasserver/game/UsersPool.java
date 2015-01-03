package ru.feamor.aliasserver.game;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Calendar;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;
import org.json.JSONArray;
import org.json.JSONObject;

import ru.feamor.aliasserver.base.DataObject;
import ru.feamor.aliasserver.base.ObjectCache;
import ru.feamor.aliasserver.base.UpdateThreadController.ThreadUpdated;
import ru.feamor.aliasserver.components.GameManager.ClientInUsersPool;
import ru.feamor.aliasserver.games.BaseGame;
import ru.feamor.aliasserver.utils.Log;

public class UsersPool {
	
	private TIntObjectHashMap<UserQueue> activeTypes;
	private DoubleLinkedList newUsers, newUsersTemp;
	private Object newUsersLocker;
	private DoubleLinkedList connectorsForLoading;
	
	public UsersPool() {
		activeTypes = new TIntObjectHashMap<>();
		newUsers = new DoubleLinkedList();
		newUsersTemp = new DoubleLinkedList();
		newUsersLocker = new Object();
		connectorsForLoading = new DoubleLinkedList();
	}
	
	public static class ClientInPool {
		private int gameType;
		private GameClient client;
		private DataObject data;
		
		public GameClient getClient() {
			return client;
		}
		
		public DataObject getData() {
			return data;
		}
		
		public int getGameType() {
			return gameType;
		}
	}
	
	private static class ClientGameTypePair extends ObjectCache.SimpleCachedObject {
		public int gameType;
		public GameClient client;
		public DataObject data;
		
		public void setup(int gameType, GameClient client, DataObject data) {
			this.gameType = gameType;
			this.client = client;
			this.data = data;
		}
		
		@Override
		public void beforeReturn() {
			client = null;
			data = null;
		}
	}
	
	private ObjectCache<ClientGameTypePair> clientGameTypePairCahce = new ObjectCache<ClientGameTypePair>(0, 100) {
		@Override
		public ClientGameTypePair create() {
			return new ClientGameTypePair();
		}
	};
	
//	public void startGame(GameClient client, int gameTypeId, DataObject data) {
//		synchronized (newUsersLocker) {
//			ClientGameTypePair pair = clientGameTypePairCahce.get();
//			pair.setup(gameTypeId, client, data);
//			DoubleLinkedListNode node = new DoubleLinkedListNode(pair);
//			newUsers.addLast(node);
//		}
//	}
	
	public void update() {
		updateLoadingConnectors();
		updateNewUsers();		
		updateConnectors();
		
	}
	
	private void updateConnectors() {
		// TODO Auto-generated method stub
		
	}

	private void updateNewUsers() {
		synchronized (newUsersLocker) {
			newUsersTemp.removeAll();
			DoubleLinkedList temp = newUsers;
			newUsers = newUsersTemp;
			newUsersTemp = temp;
		}
				
		for(DoubleLinkedListNode i = newUsersTemp.getFirst(); i!=null; i = i.next) {
			ClientGameTypePair pair = (ClientGameTypePair) i.getPayload();
			synchronized (activeTypes) {
				UserQueue queue = activeTypes.get(pair.gameType);
				if (queue == null) {
					queue = new UserQueue(pair.gameType);
					activeTypes.put(pair.gameType, queue);
					connectorsForLoading.addLast(new DoubleLinkedListNode(queue));
				}
				
				queue.addClient(pair.client);
			}
			clientGameTypePairCahce.back(pair);
		}
		newUsersTemp.removeAll();
	}

	private void updateLoadingConnectors() {
		
	}
		
	public static class SimpleCoutGameConnector extends GameConnector {
		public static final int STATE_NEW = 0;
		public static final int STATE_WAIT_USERS = 1;
		
		private int usersForGame;
		private int state;
		
		public SimpleCoutGameConnector(GameType gameType, UserQueue queue) {
			super(gameType, queue);
			state = STATE_NEW;
		}
		
		@Override
		public void update() throws InterruptedException {
			super.update();
			switch (state) {
				case STATE_NEW:
					firstStart();
					break;
				case STATE_WAIT_USERS:
					checkUsers();
					break;
			}
		}
				
		private void checkUsers() {
			DoubleLinkedList players = new DoubleLinkedList();
			synchronized (queue.users) {
				for (int i=0; i<usersForGame; i++) {
					DoubleLinkedListNode node = queue.users.getFirst();  
					queue.users.remove(node);
					players.addLast(node);
				}
			}
			
			if (queue.users.size() >= usersForGame) {
				BaseGame game = ((GameType.GameTypeWithFixedPlayersCount)gameType).createGameWithPlayers(players);
				
//				Добавить поведение сервера и клиента на ситуацию - Ошибка создания коннектора \ ошибка создания игры. 
//				добавить проверку на возможность пользователя играть в игру заданного типа.
//				Добавить для пользователя "подключение к игре отколонено"
			}
		}

		private void firstStart() {
			if (gameType instanceof GameType.GameTypeWithFixedPlayersCount) {
				usersForGame = ((GameType.GameTypeWithFixedPlayersCount) gameType).getPlayersCount();
				
				if (usersForGame <= 0) throw new RuntimeException("users For game can`t be <= 0!");
				state = STATE_WAIT_USERS;
			} else {
				throw new RuntimeException("Incorrect Game type, Game type must be inherior of GameTypeWithFixedPlayersCount");
			}
		}
	}
	
	
	
	public static class UserQueue {
		private int gameTypeId;
		private DoubleLinkedList users;
		private long createTime;
		private long lastCreatedGameTime;
		private GameConnector connector;
		private boolean isLoaded;
		
		public UserQueue(int gameTypeId) {
			this.gameTypeId = gameTypeId;
			users = new DoubleLinkedList();
			createTime = Calendar.getInstance().getTimeInMillis();
			connector = null;
			isLoaded = false;
		}
		
		public int getGameTypeId() {
			return gameTypeId;
		}
		
		public void addClient(GameClient client) {
			DoubleLinkedListNode node = new DoubleLinkedListNode(client);
			client.putTag(GameTags.TAG_GAME_CONNECTOR_QEUE_NODE, node);
			synchronized (users) {
				users.addLast(node);
			}
			
		}
		
	}

	public void startUpdate() {
		// TODO Auto-generated method stub
		
	}
}
