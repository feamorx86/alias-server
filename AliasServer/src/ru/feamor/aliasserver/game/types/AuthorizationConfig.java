package ru.feamor.aliasserver.game.types;

import org.json.JSONObject;

import ru.feamor.aliasserver.base.Config;
import ru.feamor.aliasserver.games.Authorizator;

public class AuthorizationConfig implements Config<Authorizator> {
		
	public static final long DEFAULT_TIME_TO_RECEIVE_VERSION = 10 * 1000;
	public static final long DEFAULT_TIME_TO_RECEIVE_AUTHORIZATION = 10 * 1000;

	@Override
	public void configure(JSONObject config, Authorizator authorizator) {
		authorizator.setTimeToReceiveVersion(config.optLong("timeToReceiveVersion", DEFAULT_TIME_TO_RECEIVE_VERSION));
		authorizator.setTimeToReceiveAuthorization(config.optLong("timeToReceiveAuthorization", DEFAULT_TIME_TO_RECEIVE_AUTHORIZATION));
		Authorizator.TYPE_ID = config.optInt("id", Authorizator.DEFAULT_TYPE_ID);
	}
}
