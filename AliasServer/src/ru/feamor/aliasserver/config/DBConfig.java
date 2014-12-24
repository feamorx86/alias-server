package ru.feamor.aliasserver.config;

import org.json.JSONObject;

import ru.feamor.aliasserver.base.Config;
import ru.feamor.aliasserver.components.DBManager;

public class DBConfig implements Config<DBManager> {
	
	public static final int DEFAULT_PORT = 5432;
			
	@Override
	public void configure(JSONObject config, DBManager manager) {
		if (config != null) {
			String connectionString = config.optString("connectionUrl", null);
			String userName = config.optString("user", null);
			String password = config.optString("password", "");
			
			String server = config.optString("server", "localhost");
			String db = config.optString("db", "activity");
			
			int maxDbConnections = config.optInt("maxDbConnections", DBManager.DEFAULT_DB_MAX_CONNECTIONS);
			if (connectionString == null || userName == null) {
				throw new RuntimeException("Invalid db configuration, no connection string ot user name");
			} else {
				manager.setConfig_connectionString(connectionString);
				manager.setConfig_userName(userName);
				manager.setConfig_password(password);
				manager.setConfig_server(server);
				manager.setConfig_db(db);
				manager.setConfig_max_db_connections(maxDbConnections);
			}
		} else {
			manager.setConfig_connectionString(null);
			manager.setConfig_userName(null);
			manager.setConfig_password("");
		}
	}
}
