package ru.feamor.aliasserver.game.connectors;

import java.util.Calendar;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.base.UpdateThreadController;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor.TypeGetGamesFor;
import ru.feamor.aliasserver.commands.SystemCommandsProcessor.TypeUserCanPlayGame;
import ru.feamor.aliasserver.components.DBManager;
import ru.feamor.aliasserver.db.DBRequest;
import ru.feamor.aliasserver.db.RunWithParamsDBRequest;
import ru.feamor.aliasserver.db.requests.Requests;
import ru.feamor.aliasserver.users.UsersPool.RequestedGameConnector;
import ru.feamor.aliasserver.users.UsersPool.UserQueue;
import ru.feamor.aliasserver.utils.RunWithParams;

public class ConnectorsLoader implements UpdateThreadController.ThreadPendingUpdated {

	private DoubleLinkedList newConnectors, newConnectorsTemp;
	private Object newConnectorsLocker;

	
	private DoubleLinkedListNode node;
	private boolean needStop;
	private boolean pending;
	private long lastUpdateTime;
	private long minUpdateTime = 300;
	private long nextUpdateTime = 0;
			
	public ConnectorsLoader() {
		lastUpdateTime = Calendar.getInstance().getTimeInMillis();
		pending = false;
		needStop = false;
	}
	
	public void loadGameType(UserQueue userQueue) {
		synchronized (newConnectorsLocker) {
			newConnectors.addFirst(new DoubleLinkedListNode(userQueue));
		}
	}
	
	@Override
	public void setUpdateNode(DoubleLinkedListNode node) {
		this.node = node;
	}

	@Override
	public DoubleLinkedListNode getUpdateNode() {
		return node;
	}

	@Override
	public boolean needStopUpdate() {
		return needStop;
	}
	
	private boolean checkIsItTimeToUpdate() {
		boolean needUpdate = true;
		
		pending = false;
		long now = Calendar.getInstance().getTimeInMillis();
		long delta = now - lastUpdateTime;
		lastUpdateTime = now;
		
		if (delta < minUpdateTime) {
			nextUpdateTime = now + delta;
			pending = true;
			needUpdate = false;
		}
		
		return needUpdate;
	}

	@Override
	public void update() throws InterruptedException {
		if (!needStop && checkIsItTimeToUpdate()) {
			checkNewRequests();
		}
	}

	private void checkNewRequests() {
		synchronized (newConnectorsLocker) {
			if (newConnectors.size() > 0) {
				newConnectorsTemp.removeAll();
				DoubleLinkedList temp = newConnectors;
				newConnectors = newConnectorsTemp;
				newConnectorsTemp = temp;
			}
		}
		
		for(DoubleLinkedListNode i = newConnectorsTemp.getFirst(); i!=null; i = i.next) {
			
//			- вот тут нестыковочка выходит - нужно перепроверить чтобы везде, в подобных местах был Temp.!!!
//			- дописать запроса данных
//			- создание коннектора 
//			- добавление и включение коннектора
//			- тест для коннектора
//			
//			тут всё описано
//			https://docs.google.com/document/d/1eaCsKgLZ64vl_egh0X5Mnq3lpxJJ6FH5g7rOoR5wjiw/edit?usp=sharing
				
			UserQueue userQueue = (UserQueue) i.getPayload();
			RequestedGameConnector requestedGameConnector = new RequestedGameConnector();  
			DBRequest request = DBManager.get().startRequest();
			request.setRequestParser(DBManager.commandFactory().getRequestParser(Requests.SystemCommands.CheckcGameAvalableForUser.ID));
			request.putParameter(TypeUserCanPlayGame.rq_pos_user_id, userId);
			request.putParameter(TypeUserCanPlayGame.rq_pos_game_type_id, selectedGameId);
			request.putParameter(TypeGetGamesFor.rq_request_id_position, client.nextRequestId());
			request.setSender(requestedGameConnector);
			request.setOnComplete(db_getGameTypeInfo);//TODO: add ability to make it possible to cancel if timeout.
			request.setOnExecuted(getExecutor());
			DBManager.get().executeAsync(request);					
		}
	}

	@Override
	public void onProblem(Integer problemType, Object problem) {
		
	}

	@Override
	public void wasError() {
		
	}

	@Override
	public boolean isPending() {
		return pending;
	}

	@Override
	public long getExecuteTime() {
		return nextUpdateTime;
	}
	
	private RunWithParams<DBRequest> db_getGameTypeInfo = new RunWithParamsDBRequest() {
		@Override
		public void run() {
			RequestedGameConnector request = (RequestedGameConnector) param.getSender();
			
		}
	};
}