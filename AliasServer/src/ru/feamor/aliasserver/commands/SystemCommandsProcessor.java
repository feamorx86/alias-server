package ru.feamor.aliasserver.commands;

import java.util.ArrayList;
import java.util.Calendar;

import ru.feamor.aliasserver.base.DataObject;
import ru.feamor.aliasserver.base.RunnableExecutor;
import ru.feamor.aliasserver.commands.CommandTypes.SYSTEM;
import ru.feamor.aliasserver.components.DBManager;
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
	
	public static class States {
		public static final int START = 0;
		public static final int FINISHED = 1;
		public static final int WAIT_FIRST_COMMAND = 2;		
		public static final int WAIT_GET_ALL_GAMES_RESULT = 3;
		
		public static final short DISCONNECT_REASON_FIRST_COMMAND_TIMEOUT = 0;
	}
	
	/**
	 * Time before user send any command. 5 minutes, then disconnect.
	 */
	public static final long MAX_TIME_BEFORE_FIRST_COMMAND = 5 * 60 * 1000;
	
	@Override
	public void addClient(ClientInProcessor client) {
		client.setState(States.START);
		super.addClient(client);
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
			client.setState(States.WAIT_FIRST_COMMAND);
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
		case States.FINISHED:
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
				
				DBRequest request = DBManager.get().startRequest();
				request.setRequestParser(DBManager.commandFactory().getRequestParser(Requests.SystemCommands.GetAllUserGamesWithStatus.ID));
				request.putParameter(TypeGetGamesFor.rq_pos_user_id, client.getGameClient().getPlayer().getId());
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
				client.nextRequestId();
				client.setState(States.WAIT_FIRST_COMMAND);
			}
			break;
		}
	}

	@Override
	public RunnableExecutor getExecutor() {
		return null;
	}
	
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