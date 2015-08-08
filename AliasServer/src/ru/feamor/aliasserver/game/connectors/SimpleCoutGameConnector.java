package ru.feamor.aliasserver.game.connectors;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.game.GameType;
import ru.feamor.aliasserver.games.BaseGame;
import ru.feamor.aliasserver.users.UsersPool.UserQueue;

public class SimpleCoutGameConnector extends GameConnector {
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
		synchronized (queue.getUsers()) {
			for (int i=0; i<usersForGame; i++) {
				DoubleLinkedListNode node = queue.getUsers().getFirst();  
				queue.getUsers().remove(node);
				players.addLast(node);
			}
		}
		
		if (players.size() >= usersForGame) {
			BaseGame game = ((GameType.GameTypeWithFixedPlayersCount)gameType).createGame(players);
			добавить игру
//			ƒобавить поведение сервера и клиента на ситуацию - ќшибка создани€ коннектора \ ошибка создани€ игры. 
//			добавить проверку на возможность пользовател€ играть в игру заданного типа.
//			ƒобавить дл€ пользовател€ "подключение к игре отколонено"
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
