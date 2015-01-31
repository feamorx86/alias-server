package ru.feamor.aliasserver.games.chat;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.commands.CommandTypes;
import ru.feamor.aliasserver.commands.GameCommand;
import ru.feamor.aliasserver.components.NettyManager;
import ru.feamor.aliasserver.games.BaseGame;
import ru.feamor.aliasserver.users.GameClient;
import ru.feamor.aliasserver.utils.TextUtils;

public class SimpleChat extends BaseGame {

	private HashMap<Long, GameClient> players;
	private DoubleLinkedList playersForDisconect, newPlayers;
	private DoubleLinkedList playersForDisconectTemp, newPlayersTemp;
	
	public static final byte STATUS_NEW_PLAYER = 0;
	public static final byte STATUS_PLAYER_EXIT = 1;
	public static final byte STATUS_PLAYER_ENTER = 2;
	
	public SimpleChat() {
		players = new HashMap<Long, GameClient>();
		playersForDisconect = new DoubleLinkedList();
		newPlayers = new DoubleLinkedList();
		playersForDisconectTemp = new DoubleLinkedList();
		newPlayersTemp = new DoubleLinkedList();
	}
	
	@Override
	public byte getTypeId() {
		return CommandTypes.SIMPLE_CHAT.TYPE;
	}
	
	@Override
	public void onNewPlayer(GameClient player) {
		synchronized(newPlayers) {
			newPlayers.addLast(new DoubleLinkedListNode(player));
		}
	}

	private void sendListOfPlayersTo(GameClient player) {
		GameCommand command = new GameCommand(getTypeId(), CommandTypes.SIMPLE_CHAT.LIST_CLIENTS);
		ByteBuf buffer = NettyManager.get().getCommandAllocator().buffer();
		int count = players.size();
		buffer.writeInt(count);
		long playerId = player.getPlayer().getId();
		for (GameClient inPlayer : players.values()) {
			if (inPlayer.getPlayer().getId() != playerId) {
				inPlayer.getPlayer().write(buffer);
			}
		}
		player.getConnection().sendCommand(command.retain());
	}

	private void notifyPlayerStatusChanged(GameClient player, int status) {
		GameCommand command = new GameCommand(getTypeId(), CommandTypes.SIMPLE_CHAT.PLAYER_STATUS_CHANGED);
		ByteBuf buffer = NettyManager.get().getCommandAllocator().buffer();
		buffer.writeByte(STATUS_NEW_PLAYER);
		player.getPlayer().write(buffer);
		long playerId = player.getPlayer().getId();
		for (GameClient inPlayer : players.values()) {
			if (inPlayer.getPlayer().getId() != playerId) {
				inPlayer.getConnection().sendCommand(command.retain());
			}
		}
	}
	
	@Override
	public void update() throws InterruptedException {
		//TODO: add check of elapsed time
		
		if (needStopUpdate()) {
			return;
		}
		
		//Update new players
		synchronized (newPlayers) {
			DoubleLinkedList list = newPlayers;
			newPlayers = newPlayersTemp;
			newPlayersTemp = list;
			newPlayers.removeAll();
		}
		
		for(DoubleLinkedListNode i = newPlayersTemp.getFirst(); i!=null; i=i.next) {
			GameClient client = (GameClient) i.getPayload();
			notifyPlayerStatusChanged(client, STATUS_PLAYER_ENTER);
			players.put(Long.valueOf(client.getPlayer().getId()), client);
		}
		newPlayersTemp.removeAll();
		//Process Players
		for (GameClient client : players.values()) {
			updatePlayer(client);
		}
		//Update disconnecting players
		synchronized (playersForDisconect) {
			DoubleLinkedList list = playersForDisconect;
			playersForDisconect = playersForDisconectTemp;
			playersForDisconectTemp = list;
			playersForDisconect.removeAll();
		}
		
		for(DoubleLinkedListNode i = playersForDisconectTemp.getFirst(); i!=null; i=i.next) {
			GameClient client = (GameClient) i.getPayload();
			notifyPlayerStatusChanged(client, STATUS_PLAYER_EXIT);
			players.remove(Long.valueOf(client.getPlayer().getId()));
		}
		playersForDisconectTemp.removeAll();		
	}
	
	private void updatePlayer(GameClient player) {
		GameCommand command = player.getConnection().getFirstReceivedCommand(getTypeId());
		while(command != null) {
			processCommand(command, player);
			command = player.getConnection().getFirstReceivedCommand(getTypeId());
		}
	}
	
	private void processCommand(GameCommand command, GameClient player) {
		if (command == null || player == null) return;  
		switch(command.getId()) {
			case CommandTypes.SIMPLE_CHAT.BROADCAST_MESSAGE:
				try {
					String message = TextUtils.readFromBuf(command.getData());
					command.recycle();
					sendMessageBroadCast(message, player);						
				} catch (Exception ex) {
					//TODO: add  notify : command not processed! and correct recycle of command
				}
				break;
			case CommandTypes.SIMPLE_CHAT.LIST_CLIENTS:
				command.recycle();
				sendListOfPlayersTo(player);				
				break;
			case CommandTypes.SIMPLE_CHAT.PLAYER_STATUS_CHANGED:
				try {
					byte newStatus = command.getData().readByte();
					switch(newStatus) {
					case STATUS_PLAYER_EXIT:
						notifyPlayerStatusChanged(player, STATUS_PLAYER_EXIT);
						synchronized(playersForDisconect) {
							playersForDisconect.addLast(new DoubleLinkedListNode(player));
						}
						break;
					default:
						//TODO: add handle of incorrect Client Status
						break;
					}
					command.recycle();
				} catch (Exception ex) {
					//TODO: add  notify : command not processed! and correct recycle of command
				}
				break;
			case CommandTypes.SIMPLE_CHAT.SEND_MESSAGE_TO:
				try {
					long receiverId = command.getData().readLong();
					GameClient receiver = null;
					synchronized (players) {
						receiver = players.get(Long.valueOf(receiverId));
					}
					if (receiver != null) {
						String message = TextUtils.readFromBuf(command.getData());
						command.recycle();
						sendMessage(message, receiver, player);						
					} else {
						command.recycle();
						//TODO: add  notify sender : message not send, sender not found!
					}
				}catch(Exception ex) {
					//TODO: add  notify : command not processed! and correct recycle of command
				}
				break;
			default:
				command.recycle();
				//TODO: add notify player about unknown command  and correct recycle of command
				break;
		}
	}
			
	private void sendMessage(String message, GameClient toPlayer, GameClient fromPlayer) {
		GameCommand command = createCommand(CommandTypes.SIMPLE_CHAT.SEND_MESSAGE_TO);
		//SENDER ID
		command.getData().writeLong(fromPlayer.getPlayer().getId());
		//MESSAGE
		TextUtils.writeToBuf(message, command.getData());
		toPlayer.getConnection().sendCommand(command.retain());
	}
	
	private void sendMessageBroadCast(String message, GameClient fromPlayer ) {
		GameCommand command = createCommand(CommandTypes.SIMPLE_CHAT.BROADCAST_MESSAGE);
		//SENDER ID
		long senderId = fromPlayer.getPlayer().getId();
		command.getData().writeLong(senderId);
		//MESSAGE
		TextUtils.writeToBuf(message, command.getData());
		synchronized(players) {
			for (GameClient to : players.values()) {
				if (to.getPlayer().getId() != senderId) {
					to.getConnection().sendCommand(command.retain());
				}
			}
		}
	}
	
	

	@Override
	public void onPlayerDisconnect(GameClient player) {
		synchronized(playersForDisconect) {
			playersForDisconect.addLast(new DoubleLinkedListNode(player));
		}
	}
	
}
