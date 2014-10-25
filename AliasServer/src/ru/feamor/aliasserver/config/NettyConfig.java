package ru.feamor.aliasserver.config;

import org.json.JSONObject;

import ru.feamor.aliasserver.components.NettyManager;

public class NettyConfig implements Config<NettyManager> {
	
	public static final int DEFAULT_PORT = 54651;
	
	@Override
	public void configure(JSONObject config, NettyManager manager) {
		int port = config.optInt("port",DEFAULT_PORT);
		manager.setConfig_Port(port);
	}
}
