package ru.feamor.aliasserver.db.requests;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import ru.feamor.aliasserver.base.WithRequestId;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor.TypeGetGamesFor;
import ru.feamor.aliasserver.components.DBManager;
import ru.feamor.aliasserver.db.DBRequest;
import ru.feamor.aliasserver.db.RequestParser;
import ru.feamor.aliasserver.db.requests.Requests.SystemCommands.GetAllUserGamesWithStatus;
import ru.feamor.aliasserver.game.models.GameTypeModel;
import ru.feamor.aliasserver.games.Authorizator.TypeEmailPassword;
import ru.feamor.aliasserver.utils.Log;

public class GetGamesRequestParser implements RequestParser {

	@Override
	public boolean setupRequest(PreparedStatement statement, DBRequest request) {
		int requestId = (Integer)request.getParameter(TypeGetGamesFor.rq_request_id_position);
		WithRequestId objectWithRequestId = (WithRequestId) request.getSender();
		boolean executeRequest = false;
		if (requestId == objectWithRequestId.currentRequestId()) {
			executeRequest = true; 
			try {
				long userId = (Long)request.getParameter(TypeGetGamesFor.rq_pos_user_id);
				int gameStatus = (Integer)request.getParameter(TypeGetGamesFor.rq_pos_game_type_status);
				statement.setLong(GetAllUserGamesWithStatus.rq_user_id, userId);
				statement.setInt(GetAllUserGamesWithStatus.rq_game_status, gameStatus);
			} catch (SQLException exception) {
				Log.e(GetGamesRequestParser.class, "Can`t setup request, error", exception);
			}
		} else {
			request.setCanceled(true);
			DBManager.get().recycleRequest(request);
		}
		return executeRequest;
	}
	
	@Override
	public void parseResponce(DBRequest request, ResultSet result) {
		try {
			ArrayList<GameTypeModel> games = new ArrayList<GameTypeModel>();
			while (result.next()) {
				int gameTypeId = (int)result.getLong(GetAllUserGamesWithStatus.rs_game_type_id);
				String name = result.getString(GetAllUserGamesWithStatus.rs_game_type_name);
				String description = result.getString(GetAllUserGamesWithStatus.rs_game_type_descrition);
				String iconUrl = result.getString(GetAllUserGamesWithStatus.rs_game_type_icon_url);
				GameTypeModel gameType = GameTypeModel.getModel();
				gameType.setId(gameTypeId);
				gameType.setName(name);
				gameType.setDescription(description);
				gameType.setIconUrl(iconUrl);
				games.add(gameType);
			}
			request.putResult(TypeGetGamesFor.rs_pos_result, TypeEmailPassword.RESULT_SUCCESS);
			request.putResult(TypeGetGamesFor.rs_pos_game_types, games);
		} catch (Exception ex) {
			request.putResult(TypeGetGamesFor.rs_pos_result, TypeGetGamesFor.RESULT_ERROR);
			request.putResult(TypeGetGamesFor.rs_pos_error, ex);
		}
	}

	@Override
	public String getSql() {
		return GetAllUserGamesWithStatus.SQL;
	}

	@Override
	public int id() {
		return GetAllUserGamesWithStatus.ID;
	}		
}
