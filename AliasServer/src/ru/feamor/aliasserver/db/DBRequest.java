package ru.feamor.aliasserver.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.base.RunnableExecutor;
import ru.feamor.aliasserver.components.GameManager;
import ru.feamor.aliasserver.utils.RunWithParams;
import gnu.trove.map.hash.TIntObjectHashMap;

public class DBRequest {	
	protected Object sender;
	protected RequestParser requestParser;
	protected TIntObjectHashMap<Object> parameters;
	protected TIntObjectHashMap<Object> results;
	protected DoubleLinkedListNode node;
	protected RunWithParams<DBRequest> onComplete;
	protected boolean hasError;
	protected boolean canceled;
	protected Object error;
	
	public DBRequest() {
		sender = null;
		requestParser = null;
		parameters = new TIntObjectHashMap<Object>();
		results = new TIntObjectHashMap<Object>();
		onComplete = null;
		canceled = false;
		hasError = false;
	}
	
	public void recycle() {
		sender = null;
		requestParser = null;
		parameters.clear();
		parameters = null;
		results.clear();
		results = null;
		onComplete = null;
		canceled = false;
		hasError = false;
	}
	
	public void putParameter(int paramId, Object value) {
		parameters.put(paramId, value);
	}
	
	public Object getParameter(int paramId) {
		return parameters.get(paramId);
	}
	
	public void putResult(int resultId, Object valaue) {
		results.put(resultId, valaue);
	}
	
	public Object getResult(int resultId) {
		return results.get(resultId);
	}
	
	public DoubleLinkedListNode getNode() {
		return node;
	}
	
	public void setNode(DoubleLinkedListNode node) {
		this.node = node;
	}
	
	public void clearNode() {
		node = null;
	}
	
	public void setRequestParser(RequestParser requestParser) {
		this.requestParser = requestParser;
	}
		
	public RequestParser getRequestParser() {
		return requestParser;
	}

	public void setSender(Object sender) {
		this.sender = sender;
	}
	
	public Object getSender() {
		return sender;
	}
	
	public String getSql() {
		return requestParser.getSql();
	}

	public boolean setupRequest(PreparedStatement statement) {
		return requestParser.setupRequest(statement, this);
	}
	
	public void parseResponce(ResultSet result) {
		requestParser.parseResponce(this, result);
	}
	
	public TIntObjectHashMap<Object> getParameters() {
		return parameters;
	}
	
	public void setParameters(TIntObjectHashMap<Object> parameters) {
		this.parameters = parameters;
	}
	
	public TIntObjectHashMap<Object> getResults() {
		return results;
	}
	
	public void setResults(TIntObjectHashMap<Object> results) {
		this.results = results;
	}
	
	public RunWithParams<DBRequest> getOnComplete() {
		return onComplete;
	}
	
	public void runOnComplete() {
		onComplete.setParam(this);
		if (runnableExecutor!=null) {			
			runnableExecutor.executeRunnable(onComplete);
		} else {
			onComplete.run();
		}
	}
	
	public void setOnComplete(RunWithParams<DBRequest> onComplete) {
		this.onComplete = onComplete;
	}
	
	public interface RequestExecutor {
		void execute(RunWithParams<DBRequest> onComplete, DBRequest dbRequest);
	}
	
	private RunnableExecutor runnableExecutor;
	
	public void setOnExecuted(RunnableExecutor runnableExecutor) {
		this.runnableExecutor = runnableExecutor;
	}
	
	public Object getError() {
		return error;
	}
	
	public void setError(Object error) {
		this.error = error;
	}
	
	public void setHasError(boolean hasError) {
		this.hasError = hasError;
	}
	
	public boolean hasError() {
		return this.hasError;
	}
	
	public boolean isCanceled() {
		return canceled;
	}
	
	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}
}
