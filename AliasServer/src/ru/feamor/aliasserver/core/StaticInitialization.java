package ru.feamor.aliasserver.core;

import ru.feamor.aliasserver.commands.CommandTypes;
import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.config.ConfigurationFactory;
import ru.feamor.aliasserver.db.requests.Requests;
import ru.feamor.aliasserver.utils.Log;

public class StaticInitialization {
	private CommandTypes _commandTypes;
	private GameManager _gameManager;
	private ConfigurationFactory _configurationFactory;
	private Log _log;
	private Requests _requests;
		
	public StaticInitialization() {
		Log.initialize("config/log4j.xml");
		System.out.flush();//do nothing!
	}
}
