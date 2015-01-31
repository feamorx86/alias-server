package ru.feamor.aliasserver.commands;

import java.util.ArrayList;
import java.util.Calendar;

import ru.feamor.aliasserver.base.DataObject;
import ru.feamor.aliasserver.base.RunnableExecutor;
import ru.feamor.aliasserver.commands.CommandTypes.SYSTEM;
import ru.feamor.aliasserver.components.DBManager;
import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.core.ClientInProcessor;
import ru.feamor.aliasserver.core.ClientsProcessor;
import ru.feamor.aliasserver.db.DBRequest;
import ru.feamor.aliasserver.db.RunWithParamsDBRequest;
import ru.feamor.aliasserver.db.requests.Requests;
import ru.feamor.aliasserver.game.models.GameTypeModel;
import ru.feamor.aliasserver.utils.Log;
import ru.feamor.aliasserver.utils.RunWithParams;
import ru.feamor.aliasserver.utils.TextUtils;

public class SystemCommandsProcessor extends ClientsProcessor {
	
	public static class TypeGetGamesFor {
		public static final int rq_pos_user_id = 0;
		public static final int rq_pos_game_type_status = 1;
		public static final int rq_request_id_position = 2;
		
		public static final int rs_pos_result = 0;
		
		public static final byte RESULT_SUCCESS = 0;
		public static final byte RESULT_ERROR = 2;
		
		public static final int GAME_STATUS_UNAVALABLE = 0;
		public static final int GAME_STATUS_AVALABLE = 1;
		public static final int GAME_STATUS_AVALABLE_NEXT = 2;			
		
		public static final int rs_pos_game_types = 1;			
		public static final int rs_pos_error = 1;
	}
	
	public static class TypeUserCanPlayGame {
		public static final int rq_pos_user_id = 0;
		public static final int rq_pos_game_type_id = 1;
		public static final int rq_request_id_position = 2;
		
		public static final int rs_pos_result = 0;
		public static final int rs_pos_game_type_status = 1;
		
		public static final byte RESULT_SUCCESS = 0;
		public static final byte RESULT_NO_SUCH_GAME = 1;
		public static final byte RESULT_GAME_NOT_AVALABLE = 2;
		public static final byte RESULT_ERROR = 3;
			
		public static final int rs_pos_error = 1;
	}
	
	public static class States {
		public static final int START = 0;
		public static final int FINISHED = 1;
		public static final int WAIT_FIRST_COMMAND = 2;		
		public static final int WAIT_GET_ALL_GAMES_RESULT = 3;
		public static final int WAIT_CHECK_USER_HAVE_GAME_IN_DB = 4;
		public static final int WAIT_CREATE_GAME = 5;
		
		public static final short DISCONNECT_REASON_FIRST_COMMAND_TIMEOUT = 0;
	}
	
	/**
	 * Time before user send any command. 5 minutes, then disconnect.
	 */
	public static final long MAX_TIME_BEFORE_FIRST_COMMAND = 5 * 60 * 1000;
	
	@Override
	public void addNewClient(ClientInProcessor client) {
		client.setState(States.START);
		super.addNewClient(client);
	}
	
	@Override
	public void addResuedClient(ClientInProcessor client) {
		client.setState(States.START);
		super.addResuedClient(client);
	}
		
	@Override
	public void removeClient(ClientInProcessor client) {
		super.removeClient(client);
		client.setState(States.FINISHED);
	}
	
	@Override
	public void processClient(ClientInProcessor client) {
		int state = client.getState();
		switch(state) {
		case States.START:
			{
				SystemCommandsClient systemClient = (SystemCommandsClient) client;
				systemClient.setLastCommandTime(Calendar.getInstance().getTimeInMillis());
				client.setState(States.WAIT_FIRST_COMMAND);
			}
			break;
		case States.WAIT_FIRST_COMMAND:
		{
			GameCommand command = client.getGameClient().getConnection().getFirstReceivedCommand(CommandTypes.SYSTEM.TYPE);
			if (command!=null) {
				startProcessCommand(command, (SystemCommandsClient) client);
			} else {
				long now = Calendar.getInstance().getTimeInMillis();
				SystemCommandsClient systemClient = (SystemCommandsClient) client;
				long delta = now - systemClient.getLastCommandTime(); 
				if (delta > MAX_TIME_BEFORE_FIRST_COMMAND) {
					systemClient.disconnectBy(States.DISCONNECT_REASON_FIRST_COMMAND_TIMEOUT, 
							new DataObject().setAsArraList(2)
								.addToArrayList(new DataObject().setLong(delta))
								.addToArrayList(new DataObject().setString("too long wait first command"))
					);
					removeClient(systemClient);
				}
			}
		}
		break;		
		case States.WAIT_GET_ALL_GAMES_RESULT:
		{
			GameCommand command = client.getGameClient().getConnection().getFirstReceivedCommand(CommandTypes.SYSTEM.TYPE);
			if (command!=null) {
				int cId = command.getId(); 
				if (cId == SYSTEM.CANCEL_GET_GAME_TYPES /* || cId == SYSTEM.EXET || client.Disconnnected()*/) {
					startProcessCommand(command, (SystemCommandsClient) client);	
				} else {
					//TODO: 
				}
				
			} else {
				//TODO: add timeout					
			}
		}
		break;
		case States.WAIT_CHECK_USER_HAVE_GAME_IN_DB:
			//TODO: add timeout			
		break;
		case States.WAIT_CREATE_GAME:
			//TODO: add timeout
			break;
		case States.FINISHED:
			//do nothing
			break;
		default: 
			break;
		}
	}
		
	private void startProcessCommand(GameCommand command, SystemCommandsClient client) {
		switch(command.getId()) {
		case CommandTypes.SYSTEM.GET_GAME_TYPES:
			{
				command.recycle();
				long userId = client.getGameClient().getPlayer().getId();
				
				DBRequest request = DBManager.get().startRequest();
				request.setRequestParser(DBManager.commandFactory().getRequestParser(Requests.SystemCommands.GetAllUserGamesWithStatus.ID));
				request.putParameter(TypeGetGamesFor.rq_pos_user_id, userId);
				request.putParameter(TypeGetGamesFor.rq_pos_game_type_status, TypeGetGamesFor.GAME_STATUS_AVALABLE);
				request.putParameter(TypeGetGamesFor.rq_request_id_position, client.nextRequestId());
				request.setSender(client);
				request.setOnComplete(db_getAvalableGames);//TODO: add ability to make it possible to cancel if timeout.
				request.setOnExecuted(getExecutor());
				DBManager.get().executeAsync(request);					
				client.setState(States.WAIT_GET_ALL_GAMES_RESULT);
				client.setLastCommandTime(Calendar.getInstance().getTimeInMillis());
			}
			break;
		case CommandTypes.SYSTEM.CANCEL_GET_GAME_TYPES : 
			{
				command.recycle();
				
				client.setLastCommandTime(Calendar.getInstance().getTimeInMillis());
				client.nextRequestId();
				client.setState(States.WAIT_FIRST_COMMAND);
			}
			break;
		case CommandTypes.SYSTEM.LOGOUT:
			{
				command.recycle();
				
				client.nextRequestId();
				client.getGameClient().getConnection().close();
				removeClient(client);
			}
			break;
		case CommandTypes.SYSTEM.SELECT_GAME_TYPE:
			{
				int selectedGameId = command.getData().readInt();
				long userId = client.getGameClient().getPlayer().getId();
				command.recycle();
				
				DBRequest request = DBManager.get().startRequest();
				request.setRequestParser(DBManager.commandFactory().getRequestParser(Requests.SystemCommands.CheckcGameAvalableForUser.ID));
				request.putParameter(TypeUserCanPlayGame.rq_pos_user_id, userId);
				request.putParameter(TypeUserCanPlayGame.rq_pos_game_type_id, selectedGameId);
				request.putParameter(TypeGetGamesFor.rq_request_id_position, client.nextRequestId());
				request.setSender(client);
				request.setOnComplete(db_checkUserCanStartGame);//TODO: add ability to make it possible to cancel if timeout.
				request.setOnExecuted(getExecutor());
				DBManager.get().executeAsync(request);					
				client.setState(States.WAIT_CHECK_USER_HAVE_GAME_IN_DB);
				client.setLastCommandTime(Calendar.getInstance().getTimeInMillis());
			}
			break;
			default:
				break;
		}
	}

	@Override
	public RunnableExecutor getExecutor() {
		return null;
	}
	
	private RunWithParams<DBRequest> db_checkUserCanStartGame = new RunWithParamsDBRequest() {
		@Override
		public void run() {
			SystemCommandsClient client = (SystemCommandsClient) param.getSender();
			int requestId = (Integer)param.getParameter(TypeGetGamesFor.rq_request_id_position);
			if (requestId == client.currentRequestId()) {
				int result = (Integer)param.getResult(TypeUserCanPlayGame.rs_pos_result);
				switch(result) {
				case TypeUserCanPlayGame.RESULT_SUCCESS:
					{
						int gameId = (Integer)param.getParameter(TypeUserCanPlayGame.rq_pos_game_type_id);
						GameManager.get().getUsersPool().startGame(client, gameId);
						client.setState(States.WAIT_CREATE_GAME);
					}
					break;
				case TypeUserCanPlayGame.RESULT_NO_SUCH_GAME:
					{
						GameCommand command = client.getGameClient().createCommand(CommandTypes.SYSTEM.TYPE, CommandTypes.SYSTEM.SELECT_GAME_TYPE);
						int gameId = (Integer)param.getParameter(TypeUserCanPlayGame.rq_pos_game_type_id);
						command.getData().writeByte(TypeUserCanPlayGame.RESULT_NO_SUCH_GAME).writeInt(gameId);
						client.getGameClient().getConnection().sendCommand(command);
						client.setState(States.WAIT_FIRST_COMMAND);
					}
					break;
				case TypeUserCanPlayGame.RESULT_GAME_NOT_AVALABLE:
					{
						GameCommand command = client.getGameClient().createCommand(CommandTypes.SYSTEM.TYPE, CommandTypes.SYSTEM.SELECT_GAME_TYPE);
						int gameId = (Integer)param.getParameter(TypeUserCanPlayGame.rq_pos_game_type_id);
						int gameStatus = (Integer)param.getResult(TypeUserCanPlayGame.rs_pos_game_type_status);
						command.getData()
							.writeByte(TypeUserCanPlayGame.RESULT_GAME_NOT_AVALABLE)
							.writeInt(gameId)
							.writeInt(gameStatus);
						client.getGameClient().getConnection().sendCommand(command);
						client.setState(States.WAIT_FIRST_COMMAND);
					}
					break;
				case TypeUserCanPlayGame.RESULT_ERROR:
					{
						Exception ex = (Exception) param.getResult(TypeUserCanPlayGame.rs_pos_error);
						Log.e(SystemCommandsProcessor.class, "DB: Check user can play game, have error", ex);
						GameCommand command = client.getGameClient().createCommand(CommandTypes.SYSTEM.TYPE, CommandTypes.SYSTEM.SELECT_GAME_TYPE);
						command.getData().writeByte(result);
						TextUtils.writeToBuf("Problems, while check is game avalable for user", command.getData());
						client.getGameClient().getConnection().sendCommand(command);
						client.setState(States.WAIT_FIRST_COMMAND);
					}
					break;
				}
			} else {
				Log.i(SystemCommandsProcessor.class, "DB: Check user can start game, client send another request.");
			}
		}
	};
	
	private RunWithParams<DBRequest> db_getAvalableGames = new RunWithParamsDBRequest() {
		@Override
		public void run() {
			SystemCommandsClient client = (SystemCommandsClient) param.getSender();
			int requestId = (Integer)param.getParameter(TypeGetGamesFor.rq_request_id_position);
			if (requestId == client.currentRequestId()) {
				int result = (Integer)param.getResult(TypeGetGamesFor.rs_pos_result);
				
				switch(result) {
				case TypeGetGamesFor.RESULT_SUCCESS:
				{
					ArrayList<GameTypeModel> avalableGames = (ArrayList<GameTypeModel>)param.getResult(TypeGetGamesFor.rs_pos_result);
					GameCommand command = client.getGameClient().createCommand(CommandTypes.SYSTEM.TYPE, CommandTypes.SYSTEM.GET_GAME_TYPES);
					command.getData().writeByte(result).writeInt(avalableGames.size());
					for (GameTypeModel gameTypeModel : avalableGames) {
						gameTypeModel.writeClientGameTypeInfo(command.getData());
					}
					client.getGameClient().getConnection().sendCommand(command);
					client.setState(States.WAIT_FIRST_COMMAND);
				}
				break;
				case TypeGetGamesFor.RESULT_ERROR:
				{
					Exception ex = (Exception) param.getResult(TypeGetGamesFor.rs_pos_error);
					Log.e(SystemCommandsProcessor.class, "DB: Get avalable games, can`t get list, have error", ex);
					GameCommand command = client.getGameClient().createCommand(CommandTypes.SYSTEM.TYPE, CommandTypes.SYSTEM.GET_GAME_TYPES);
					command.getData().writeByte(result);
					TextUtils.writeToBuf("Problems, while get avalable games list", command.getData());
					client.getGameClient().getConnection().sendCommand(command);
					client.setState(States.WAIT_FIRST_COMMAND);
				}
				break;
				default:
					Log.e(SystemCommandsProcessor.class, "DB: Get avalable games, problem, unexpected result = "+result+", for client = "+client);
					break;
				}
			} else {
				Log.i(SystemCommandsProcessor.class, "DB: Get avalable games, client send another request.");
			}
		}
	};
}