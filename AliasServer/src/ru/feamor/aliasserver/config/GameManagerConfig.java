package ru.feamor.aliasserver.config;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.feamor.aliasserver.base.Config;
import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.game.types.AuthorizationGameType;
import ru.feamor.aliasserver.game.types.GameTypeCollector;
import ru.feamor.aliasserver.games.BaseGame;
import ru.feamor.aliasserver.utils.Log;

public class GameManagerConfig implements Config<GameManager> {
	@Override
	public void configure(JSONObject config, GameManager gameManager) {
		JSONArray gameTypes = config.optJSONArray("gameTypes");
		if (gameTypes!= null) {
			Config gameTypesConfig = ConfigurationFactory.get(GameTypeCollector.class);
			gameTypesConfig.configure(config, gameManager.getTypeController());
		} else {
			Log.e(GameManagerConfig.class, "There is no game types in configuration");
		}
		JSONObject gameLogic = config.optJSONObject("gameLogic");
		if (gameLogic!=null) {
			gameManager.setConfig_maxGameLogicThreads(gameLogic.optInt("maxThreads", GameManager.DEFAULT_MAX_GAME_LOGIC_THREADS));
		}
		
		gameManager.getGamesFactory().configure(config.optJSONObject("games"));
		
	}
}