package ru.feamor.aliasserver.db.requests;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.feamor.aliasserver.base.WithRequestId;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor.TypeGetGamesFor;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor.TypeUserCanPlayGame;
import ru.feamor.aliasserver.components.DBManager;
import ru.feamor.aliasserver.db.DBRequest;
import ru.feamor.aliasserver.db.RequestParser;
import ru.feamor.aliasserver.db.requests.Requests.SystemCommands.CheckcGameAvalableForUser;
import ru.feamor.aliasserver.db.requests.Requests.SystemCommands.GetAllUserGamesWithStatus;
import ru.feamor.aliasserver.utils.Log;

public class CheckGameAvailableForUserRequestParser implements RequestParser {

	@Override
	public boolean setupRequest(PreparedStatement statement, DBRequest request) {
		int requestId = (Integer)request.getParameter(TypeUserCanPlayGame.rq_request_id_position);
		WithRequestId objectWithRequestId = (WithRequestId) request.getSender();
		boolean executeRequest = false;
		if (requestId == objectWithRequestId.currentRequestId()) {
			executeRequest = true; 
			try {
				long userId = (Long)request.getParameter(TypeUserCanPlayGame.rq_pos_user_id);
				int gameTypeId = (Integer)request.getParameter(TypeUserCanPlayGame.rq_pos_game_type_id);
				statement.setLong(Requests.SystemCommands.CheckcGameAvalableForUser.rq_user_id, userId);
				statement.setInt(Requests.SystemCommands.CheckcGameAvalableForUser.rq_game_type_id, gameTypeId);
			} catch (SQLException exception) {
				Log.e(CheckGameAvailableForUserRequestParser.class, "Can`t setup request, error", exception);
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
			int gameResult = TypeUserCanPlayGame.RESULT_NO_SUCH_GAME;
			while (result.next()) {
//				long userGameTypeId = result.getLong(CheckcGameAvalableForUser.rs_user_game_type_id);
				int userGameTypeStatus = result.getInt(CheckcGameAvalableForUser.rs_user_game_type_status);
				
				if (userGameTypeStatus == TypeGetGamesFor.GAME_STATUS_AVALABLE) {
					gameResult = TypeUserCanPlayGame.RESULT_SUCCESS;
				} else {
					gameResult = TypeUserCanPlayGame.RESULT_GAME_NOT_AVALABLE;
					request.putResult(TypeUserCanPlayGame.rs_pos_game_type_status, userGameTypeStatus);
				}
			}
			request.putResult(TypeUserCanPlayGame.rs_pos_result, gameResult);
		} catch (Exception ex) {
			request.putResult(TypeUserCanPlayGame.rs_pos_result, TypeUserCanPlayGame.RESULT_ERROR);
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
