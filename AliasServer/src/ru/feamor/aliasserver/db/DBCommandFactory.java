package ru.feamor.aliasserver.db;

import gnu.trove.map.hash.TIntObjectHashMap;

import org.json.JSONObject;

import ru.feamor.aliasserver.config.ConfigurationFactory;
import ru.feamor.aliasserver.db.requests.AuthorizationParser;
import ru.feamor.aliasserver.db.requests.Requests;
import ru.feamor.aliasserver.utils.Log;

public class DBCommandFactory {
	
	private TIntObjectHashMap<RequestParser> parsers;
	
	public DBCommandFactory() {
		parsers = new TIntObjectHashMap<>();
	}
	
	public RequestParser getRequestParser(int id) {
		RequestParser parser = parsers.get(id);
		return parser;
	}
	
	public void configure(JSONObject config) {
		Log.i(DBCommandFactory.class, "Start configuration");
		
		if (config != null) {
			//TODO: Add exclude configurations
			//TODO: Add include configurations
		}
		parsers.put(Requests.Authorization.EmailAndPassword.ID, new AuthorizationParser());	
		Log.i(DBCommandFactory.class, "Configuration complete");
	}
	
}
