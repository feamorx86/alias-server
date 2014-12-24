package ru.feamor.aliasserver.game.types;

import org.json.JSONObject;

import ru.feamor.aliasserver.game.GameType;
import ru.feamor.aliasserver.games.Authorizator;
import ru.feamor.aliasserver.games.BaseGame;

public class AuthorizationGameType extends GameType{
	
	public static final String Alias = "authorization";
	public static final int DEFAULT_TYPE_ID = 1;
	public static int TYPE_ID = DEFAULT_TYPE_ID; 
	/**
	 * Time to receive first command - version of client
	 */
	public static final long DEFAULT_TIME_TO_RECEIVE_VERSION = 10 * 1000;
	public static final long DEFAULT_TIME_TO_RECEIVE_AUTHORIZATION = 10 * 1000;


	private long timeToReceiveVersion;
	private long timeToReceiveAuthorization;
	
	@Override
	public BaseGame createGame() {
		Authorizator auth = new Authorizator();
		auth.setType(this);
		return auth;
	}

	@Override
	public String getAlias() {
		return Alias;
	}
	
	@Override
	public void configure(JSONObject config) {
		super.configure(config);
		timeToReceiveVersion = config.optLong("timeToReceiveVersion", DEFAULT_TIME_TO_RECEIVE_VERSION);
		timeToReceiveAuthorization = config.optLong("timeToReceiveAuthorization", DEFAULT_TIME_TO_RECEIVE_AUTHORIZATION);
		TYPE_ID = config.optInt("id", DEFAULT_TYPE_ID);
	}
	
	public long getTimeToReceiveAuthorization() {
		return timeToReceiveAuthorization;
	}
	
	public long getTimeToReceiveVersion() {
		return timeToReceiveVersion;
	}
}
