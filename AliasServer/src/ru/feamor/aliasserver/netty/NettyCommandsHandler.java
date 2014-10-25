package ru.feamor.aliasserver.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.feamor.aliasserver.commands.GameCommand;

public class NettyCommandsHandler extends SimpleChannelInboundHandler<GameCommand> {
	
	private NettyClient client;
	
	public NettyCommandsHandler(NettyClient client) {
		super();
		this.client = client;						
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, GameCommand msg)
			throws Exception {
		client.onCommandReceive(msg);
	}
}