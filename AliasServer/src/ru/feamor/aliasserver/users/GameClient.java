package ru.feamor.aliasserver.users;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;

import ru.feamor.aliasserver.commands.GameCommand;
import ru.feamor.aliasserver.components.NettyManager;
import ru.feamor.aliasserver.game.GamePlayer;
import ru.feamor.aliasserver.netty.NettyClient;

public class GameClient {
	private NettyClient connection;
	private GamePlayer player;	
	private HashMap<Integer, Object> tags;
	
	public GameClient() {
		tags = new HashMap<Integer, Object>();
		connection = null;
		player = null;
	}
	
	public NettyClient getConnection() {
		return connection;
	}
	
	public void setConnection(NettyClient connection) {
		this.connection = connection;
	}
	
	public GamePlayer getPlayer() {
		return player;
	}
	
	public void setPlayer(GamePlayer player) {
		this.player = player;
	}
	
	public HashMap<Integer, Object> getTags() {
		return tags;
	}
	
	public Object getTag(int tag) {
		return tags.get(Integer.valueOf(tag));
	}
	
	public void putTag(int tag, Object value) {
		tags.put(Integer.valueOf(tag), value);
	}
	
	public void removeTag(int tag) {
		tags.remove(Integer.valueOf(tag));
	}
	
	public GameCommand createCommand(byte service, short commandId) {
		GameCommand command = new GameCommand(service, commandId);
		ByteBuf buf = NettyManager.get().getCommandAllocator().buffer();
		command.setData(buf);
		return command;
	}
	
}
