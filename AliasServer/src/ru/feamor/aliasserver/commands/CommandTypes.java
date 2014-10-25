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
	}

	public static class NEW_PLAYER {
		public static final byte TYPE					= 2;
		public static final short START_GAME 					= 0;
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
