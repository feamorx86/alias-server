package ru.feamor.aliasserver.db.requests;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.feamor.aliasserver.db.DBRequest;
import ru.feamor.aliasserver.db.RequestParser;
import ru.feamor.aliasserver.db.requests.Requests.Authorization.EmailAndPassword;
import ru.feamor.aliasserver.games.Authorizator.TypeEmailPassword;
import ru.feamor.aliasserver.utils.Log;

public class AuthorizationParser implements RequestParser {

	@Override
	public boolean setupRequest(PreparedStatement statement, DBRequest request) {
		try {
			String email = (String)request.getParameter(TypeEmailPassword.rq_pos_email);
			String password = (String)request.getParameter(TypeEmailPassword.rq_pos_password);
			statement.setString(EmailAndPassword.rq_email, email);
			statement.setString(EmailAndPassword.rq_password, password);
		} catch (SQLException exception) {
			Log.e(AuthorizationParser.class, "Can`t setup request, error", exception);
		}
		return true;
	}

	@Override
	public void parseResponce(DBRequest request, ResultSet result) {
		try {
			if (result.next()) {
				long userId = result.getLong(EmailAndPassword.rs_userId);
				String userAlias = result.getString(EmailAndPassword.rs_userAlias);
				
				request.putResult(TypeEmailPassword.rs_pos_result, TypeEmailPassword.RESULT_SUCCESS);
				request.putResult(TypeEmailPassword.rs_pos_user_id, userId);
				request.putResult(TypeEmailPassword.rs_pos_user_alias, userAlias);
			} else {
				request.putResult(TypeEmailPassword.rs_pos_result, TypeEmailPassword.RESULT_NOT_FOUND);
			}
		} catch (Exception ex) {
			request.putResult(TypeEmailPassword.rs_pos_result, TypeEmailPassword.RESULT_ERROR);
			request.putResult(TypeEmailPassword.rs_pos_error, ex);
		}
	}

	@Override
	public String getSql() {
		return EmailAndPassword.SQL;
	}

	@Override
	public int id() {
		// TODO Auto-generated method stub
		return EmailAndPassword.ID;
	}
		
}
