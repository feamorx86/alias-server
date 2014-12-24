package ru.feamor.aliasserver.netty;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import ru.feamor.aliasserver.commands.GameCommand;
import ru.feamor.aliasserver.components.DBManager;
import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.components.NettyManager;
import ru.feamor.aliasserver.core.ComponentManager;
import ru.feamor.aliasserver.utils.Log;

public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

	private NettyManager manager;
	
	public NettyChannelInitializer(NettyManager manager) {
		super();
		this.manager = manager;
	}	
	
	@Override
	protected void initChannel(SocketChannel ch)
			throws Exception {
		NettyClient client = new NettyClient(ch);
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("frame", new LengthFieldBasedFrameDecoder(GameCommand.MAX_COMMAND_SIZE, GameCommand.LENGTH_OFFSET, Integer.SIZE/8));
		pipeline.addLast("codec", new NettyCommandCodec(manager.getCommandAllocator()));
		pipeline.addLast("handler", new NettyCommandsHandler(client));		
		GameManager.get().addNewPlayer(client);
	}	
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		// TODO Add Handeler errors
		Log.e(NettyManager.class, "Netty error", cause);
		super.exceptionCaught(ctx, cause);		
	}
}