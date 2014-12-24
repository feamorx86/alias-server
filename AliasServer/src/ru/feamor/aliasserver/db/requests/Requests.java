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
	
	
}
