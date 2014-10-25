package ru.feamor.aliasserver.game;

import ru.feamor.aliasserver.utils.TextUtils;
import io.netty.buffer.ByteBuf;


public class GamePlayer {
	private long id;
	private String Name;
	private GameTeam team;
	private int level;	
		
	public GamePlayer() {
		
	}
			
	public long getId() {
		return id;
	}
	
	public int getLevel() {
		return level;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public void setName(String name) {
		Name = name;
	}
	
	public void setTeam(GameTeam team) {
		this.team = team;
	}
	
	public String getName() {
		return Name;
	}
	
	public GameTeam getTeam() {
		return team;
	}
	
	public void write(ByteBuf out) {
		out.writeLong(id);
		TextUtils.writeToBuf(Name, out);
		out.writeInt(level);
	}
	
	public void read(ByteBuf in) {
		id = in.readLong();
		Name = TextUtils.readFromBuf(in);
		level = in.readInt();
	}
}
