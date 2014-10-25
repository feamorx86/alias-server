package ru.feamor.aliasserver.components;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import ru.feamor.aliasserver.core.Component;
import ru.feamor.aliasserver.netty.NettyChannelInitializer;
import ru.feamor.aliasserver.utils.Log;

public class NettyManager extends Component  {
	
	private int config_Port;
	
	private EventLoopGroup acceptEventLoopGroup;
	private EventLoopGroup clientEventLoopGroup;
	private ServerBootstrap bootstrap;
	
	public static NettyManager get() {
		return (NettyManager)Components.nettyManager.compenent;
	}
		
	@Override
	public void create() {
		super.create();
	}	
	
	@Override
	public void onAdded() {
		super.onAdded();
		//TODO: add normal properties for byte buffer allocatior
		commandAllocator = new PooledByteBufAllocator(true);
		acceptEventLoopGroup = new NioEventLoopGroup();
		clientEventLoopGroup = new NioEventLoopGroup();
		try {
			bootstrap = new ServerBootstrap();
			
			bootstrap.group(acceptEventLoopGroup, clientEventLoopGroup);
			bootstrap.channel(NioServerSocketChannel.class);
			
			bootstrap.childHandler(new NettyChannelInitializer(NettyManager.this));
			bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
		} catch(Throwable e) {
			
		}
	}
	
	public void onServerStarted() {
		Log.i(NettyManager.class, "Netty server Started!");
	}
	
	@Override
	public void onStart() {
		super.onStart();
		ChannelFuture bindFuture = null;
		try {
			bindFuture = bootstrap.bind(config_Port);
			bindFuture.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						onServerStarted();
					} else {
						if (future.isCancelled()) {
							Log.e(NettyManager.class, "Fail to bind server, port:"+config_Port+", Operation Canceled");
							//TODO: add error code / type
							onError();
						} else {
							Log.e(NettyManager.class, "Fail to bind server, port:"+config_Port, future.cause());
							//TODO: add error code / type
							onError();
						}
					}
				}
			});
		} catch(Throwable e) {
			
		}
	}
	
	@Override
	public void onRemoved() {
		// TODO Auto-generated method stub
		super.onRemoved();
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	@Override
	public void onError(Object... args) {
		// TODO Auto-generated method stub
		super.onError(args);
	}
	
	public void setConfig_Port(int config_Port) {
		this.config_Port = config_Port;
	}
	
	io.netty.buffer.ByteBufAllocator commandAllocator;

	public ByteBufAllocator getCommandAllocator() {
		return commandAllocator;
	}
}
