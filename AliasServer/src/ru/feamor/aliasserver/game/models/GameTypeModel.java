package ru.feamor.aliasserver.game.models;

import io.netty.buffer.ByteBuf;

import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.base.ObjectCache;
import ru.feamor.aliasserver.utils.TextUtils;

public class GameTypeModel implements ObjectCache.CachedObject{
	
	private int id;
	private String name;
	private String description;
	private String iconUrl;
	private int classId;
	private String config;
	
	public GameTypeModel() {
		beforeReturn();
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getIconUrl() {
		return iconUrl;
	}
	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}
	public int getClassId() {
		return classId;
	}
	public void setClassId(int classId) {
		this.classId = classId;
	}
	public String getConfig() {
		return config;
	}
	public void setConfig(String config) {
		this.config = config;
	}
	
	private DoubleLinkedListNode cacheNode = new DoubleLinkedListNode(this);
	
	@Override
	public DoubleLinkedListNode getCacheNode() {
		return cacheNode;
	}
		
	@Override
	public void beforeReturn() {
		id = 0;
		name = "";
		description = "";
		iconUrl = "";
		classId = 0;
		config = null;
	}
	
	public void back() {
		returnModel(this);
	}
	
	private static ObjectCache<GameTypeModel> gameTypesCahce = new ObjectCache<GameTypeModel>(0, 100) {
		
		@Override
		public GameTypeModel create() {
			return new GameTypeModel();
		}
	};
	
	public static GameTypeModel getModel() {
		return gameTypesCahce.get();
	}
	
	public static void returnModel(GameTypeModel item) {
		gameTypesCahce.back(item);
	}
	
	public void writeClientGameTypeInfo(ByteBuf out) {
		out.writeInt(classId);
		TextUtils.writeToBuf(name, out);
		TextUtils.writeToBuf(description, out);
		TextUtils.writeToBuf(iconUrl, out);
	}
}
