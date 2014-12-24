package ru.feamor.aliasserver.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import ru.feamor.aliasserver.commands.GameCommand;

public class NettyCommandCodec extends io.netty.handler.codec.ByteToMessageCodec<GameCommand> {

	ByteBufAllocator allocator;
	public NettyCommandCodec(ByteBufAllocator allocator) {
		super();
		this.allocator = allocator;
	}
	
	@Override
	protected void encode(ChannelHandlerContext ctx, GameCommand msg,
			ByteBuf out) throws Exception {
		out.writeByte(msg.getType());
		out.writeShort(msg.getId());
		out.writeInt(msg.getDataLength());
		out.writeBytes(msg.getData());
		msg.recycle();
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		GameCommand command = new GameCommand();
		byte type = in.readByte();
		short id = in.readShort();
		int length = in.readInt();
		ByteBuf data =  allocator.buffer(length, length);
		in.readBytes(data, length);
		command.setType(type);
		command.setId(id);
		command.setData(data);
		out.add(command);
	}
	
}