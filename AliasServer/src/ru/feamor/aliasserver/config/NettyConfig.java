package ru.feamor.aliasserver.config;

import org.json.JSONObject;

import ru.feamor.aliasserver.base.Config;
import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.components.NettyManager;

public class NettyConfig implements Config<NettyManager> {
	
	public static final int DEFAULT_PORT = 54651;
	
	@Override
	public void configure(JSONObject config, NettyManager manager) {
		JSONObject nettyConfig = config.optJSONObject("netty");
		if (nettyConfig != null) {
			int port = nettyConfig.optInt("port",DEFAULT_PORT);
			manager.setConfig_Port(port);
		} else {
			manager.setConfig_Port(DEFAULT_PORT);
		}
	}
}
