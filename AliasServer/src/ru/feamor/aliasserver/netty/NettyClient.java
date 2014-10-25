package ru.feamor.aliasserver.netty;

import java.util.HashMap;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.commands.GameCommand;
import ru.feamor.aliasserver.commands.GameCommandHolder;
import ru.feamor.aliasserver.components.TimeManager;

public class NettyClient {
	private SocketChannel channel;	
	private Object gameCommandsLocker;
	
	private HashMap<Byte, GameCommandHolder> commands;	
	
	public NettyClient(SocketChannel socketChannel) {
		this.channel = socketChannel;
		gameCommandsLocker = new Object();
		commands = new HashMap<Byte, GameCommandHolder>();
	}
	
	public void onCommandReceive(GameCommand command) {
		synchronized (gameCommandsLocker) {
			GameCommandHolder holder = commands.get(Byte.valueOf(command.getType()));
			if (holder == null) {
				holder = new GameCommandHolder(command.getType());
				commands.put(Byte.valueOf(command.getType()), holder);
			}
			holder.addCommand(command);
			command.setReceiveTime(TimeManager.get().getNow());	
		}		
	}
	
	public GameCommandHolder getCommandHolder(byte type) {
		return commands.get(Byte.valueOf(type));
	}
		
	public GameCommand getFirstReceivedCommand(byte type) {
		GameCommand result = null;
		synchronized (gameCommandsLocker) {
			GameCommandHolder holder = getCommandHolder(type);
			if (holder!=null && holder.getCommands().size()>0) {
				DoubleLinkedListNode firstNode = holder.getCommands().getFirst();
				holder.getCommands().remove(firstNode);
				result = (GameCommand) firstNode.getPayload();
				result.setCommandNode(null);
			} 
		}
		return result;
	}
	
	
	private ChannelFutureListener sendCommandListener = new ChannelFutureListener() {
		
		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			// TODO: Add handler errors!!!
		}
	};
	
	private ChannelFutureListener closeListener = new ChannelFutureListener() {
		
		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			//TODO: handler close
			
		}
	};
	
	public void sendCommand(GameCommand command) {
		ChannelFuture future = channel.writeAndFlush(command);
		future.addListener(sendCommandListener);
	}
	
	public void close() {
		ChannelFuture future = channel.close();
		future.addListener(closeListener);
	}
	
	public Object getGameCommandsLocker() {
		return gameCommandsLocker;
	}
}