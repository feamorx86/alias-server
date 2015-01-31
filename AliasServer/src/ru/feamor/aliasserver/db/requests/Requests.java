package ru.feamor.aliasserver.db.requests;

public class Requests {
	
	public static class Authorization {
		
		public static class AuthorizationTypes {
			public static final int TYPE_EMAIL = 1;
			public static final int TYPE_COOKIES = 2;
		}
		
		public static class EmailAndPassword {
			public static final int ID = 10;
			public static final String SQL = "SELECT user_ids.fk_user, users.alias "
					+ "FROM user_ids, users "
					+ "WHERE user_ids.fk_id_type = "+AuthorizationTypes.TYPE_EMAIL+" AND user_ids.field_1 = ? AND user_ids.field_2 = ? "
					+ "AND user_ids.fk_user = users.id;";
			
			public static final int rq_email = 1;
			public static final int rq_password = 2;
			
			public static final int rs_userId = 1;
			public static final int rs_userAlias = 2;
			
			
		}		
	}
	
	public static class SystemCommands {
		
		public static class GetAllUserGamesWithStatus {
			public static final int ID = 20;
			public static final String SQL = "SELECT game_type.id, game_type.NAME, game_type.description, game_type.icon_url "
					+ "FROM game_type "
					+ "LEFT OUTER JOIN user_game_types ON user_game_types.fk_game_type = game_type.id "
					+ "WHERE user_game_types.fk_user = ? "
					+ "AND user_game_types.status = ?; ";
			public static final int rq_user_id = 1;
			public static final int rq_game_status = 2;
			
			public static final int rs_game_type_id = 1;
			public static final int rs_game_type_name = 2;
			public static final int rs_game_type_descrition = 3;
			public static final int rs_game_type_icon_url = 4;
		}
		
		public static class CheckcGameAvalableForUser {
			public static final int ID = 30;
			public static final String SQL = "SELECT user_game_types.id, user_game_types.status "
					+ "FROM user_game_types"
					+ "WHERE user_game_types.fk_user = ? "
					+ "AND user_game_types.fk_game_type = ?;";
			public static final int rq_user_id = 1;
			public static final int rq_game_type_id = 2;
			
			public static final int rs_user_game_type_id = 1;
			public static final int rs_user_game_type_status = 2;
		}
		
		static class TODO_GetAllUserGamesWithStatus_MD5 {
			//TODO: add md5
		}
	}
	
	
}
