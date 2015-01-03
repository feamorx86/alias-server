package ru.feamor.aliasserver.commands;


public class CommandTypes {
	
	public static class UNKNOWN {
		public static final byte TYPE					= 0;
		public static final short UNKNOWN						= 0;
	}
	
	public static class SYSTEM {
		public static final byte TYPE					= 1;
		public static final short VERSION						= 0; 
		public static final short LOGIN							= 1;
		public static final short LOGOUT						= 2;
		
		public static final short GET_GAME_TYPES	 			= 3;
		public static final short SELECT_GAME_TYPE 				= 4;
		public static final short DISCONNECT_BY					= 5;
		public static final short CANCEL_GET_GAME_TYPES	 		= 6;
	}

	public static class NEW_PLAYER {
		public static final byte TYPE					= 2;
		public static final short VERSION	 					= 1;
		public static final short AUTHORIZE 					= 2;
		public static final short REGISTER	 					= 3;
		public static final short ACTION_TIMEOUT				= 4;
		public static final short INVALID_COMMAND				= 5;
		
		public static class P_AUTH {
			public static final int AUTH_TYPE_INVALID = 0;
			public static final int AUTH_TYPE_LOGIN_AND_PASSWORD = 1;
			public static final int AUTH_TYPE_COOKIE = 2;
			public static final int AUTH_TYPE_VKONTAKTE_ID = 3;
			public static final int AUTH_TYPE_FACEBOOK_ID = 4;
			
			public static final int AUTH_RESULT_SUCCESS = 0;
			public static final int AUTH_RESULT_FAIL = 1;	
			public static final int AUTH_RESULT_ERROR = 2;
			
			public static final int AUTH_ERROR_INCORRECT_MESSAGE_FORMAT = 0;
			public static final int AUTH_ERROR_INCORRECT_AUTHORIZATION_TYPE= 1;
			public static final int AUTH_ERROR_INCORRECT_AUTHORIZATION_DATA= 2;
			
			public static final byte VERSION_LAST = 0;
			public static final byte VERSION_CAN_UPDATED = 1;
			public static final byte VERSION_NOT_SUPPORTED = 2;
		}
	}
	
	public static class SIMPLE_CHAT {
		public static final byte TYPE					= 10;
		public static final short PLAYER_STATUS_CHANGED			= 0;
		public static final short SYSTEM_MESSAGE				= 1;
		public static final short BROADCAST_MESSAGE				= 2;
		public static final short SEND_MESSAGE_TO				= 3;
		public static final short RECEIVE_MESSAGE_FROM			= 4;
		public static final short LIST_CLIENTS					= 5;		
	}
}
