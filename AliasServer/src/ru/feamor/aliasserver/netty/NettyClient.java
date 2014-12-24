package ru.feamor.aliasserver.netty;

import gnu.trove.map.hash.TByteObjectHashMap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.SocketChannel;

import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.commands.GameCommand;
import ru.feamor.aliasserver.commands.GameCommandHolder;
import ru.feamor.aliasserver.components.TimeManager;
import ru.feamor.aliasserver.utils.Log;

public class NettyClient {
	private SocketChannel channel;	
	private Object gameCommandsLocker;
	private boolean closed;
	
	private TByteObjectHashMap<GameCommandHolder> commands;	
	
	public NettyClient(SocketChannel socketChannel) {
		this.channel = socketChannel;
		closed = false;
		gameCommandsLocker = new Object();
		commands = new TByteObjectHashMap<GameCommandHolder>();
		channel.closeFuture().addListener(closeListener);
	}
	
	public void onCommandReceive(GameCommand command) {
		synchronized (gameCommandsLocker) {
			GameCommandHolder holder = commands.get(command.getType());
			if (holder == null) {
				holder = new GameCommandHolder(command.getType());
				commands.put(command.getType(), holder);
			}
			holder.addCommand(command);
			command.setReceiveTime(TimeManager.get().getNow());	
		}		
	}
	
	public GameCommandHolder getCommandHolder(byte type) {
		return commands.get(type);
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
	
	public synchronized boolean isClosed() {
		return closed;
	}
	
	private synchronized void setClosed(boolean closed) {
		this.closed = closed;
	}
	
	public boolean isConnected() {
		boolean result = channel != null && channel.isActive();
		return result;
	}
	
	//TODO: add cache for listeners
	private ChannelFutureListener sendCommandListener = new ChannelFutureListener() {
		
		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			// TODO: Add handler errors!!!
			Log.i("complete : "+future.isSuccess());
		}
	};
	//TODO: add cache for listeners	
	private ChannelFutureListener closeListener = new ChannelFutureListener() {
		
		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			//TODO: handler close
			setClosed(true);
		}
	};
	//TODO: add cache for listeners
	private ChannelFutureListener lastCommandListener = new ChannelFutureListener() {
		
		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			//TODO: handler close
			close();
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

	public void sendLastCommandAndClose(GameCommand lastCommand) {
		ChannelFuture future = channel.writeAndFlush(lastCommand);
		future.addListener(lastCommandListener);
	}
}