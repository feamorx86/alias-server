package ru.feamor.aliasserver.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import ru.feamor.aliasserver.commands.GameCommand;
import ru.feamor.aliasserver.components.NettyManager;

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
		pipeline.addLast("frame", new LengthFieldBasedFrameDecoder(GameCommand.MAX_COMMAND_SIZE, GameCommand.LENGTH_OFFSET, Integer.SIZE));
		pipeline.addLast("codec", new NettyCommandCodec(manager.getCommandAllocator()));
		pipeline.addLast("handler", new NettyCommandsHandler(client));			
	}		
}