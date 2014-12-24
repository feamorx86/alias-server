package ru.feamor.aliasserver.components;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;
import org.json.JSONObject;
import org.postgresql.ds.PGPoolingDataSource;

import ru.feamor.aliasserver.core.Component;
import ru.feamor.aliasserver.db.DBCommandFactory;
import ru.feamor.aliasserver.db.DBRequest;
import ru.feamor.aliasserver.utils.Log;

public class DBManager extends Component {

	
	private static final int DEFAULT_DB_MAX_THREADS = 5;
	public static final int DEFAULT_DB_MAX_CONNECTIONS = 5;
	
	private String config_connectionString;
	private String config_server;
	private String config_db;
	private String config_userName;
	private String config_password;
	private int config_max_db_connections = DEFAULT_DB_MAX_CONNECTIONS;
	
	
	private int config_dbMaxThreads = DEFAULT_DB_MAX_THREADS; 
	
	private ExecutorService threadPool = Executors.newFixedThreadPool(config_dbMaxThreads);
	
	private DBCommandFactory commandFactory;
	
	private PGPoolingDataSource dataSources;
	
	public static DBManager get() {
		return (DBManager)Components.dbManager.compenent;
	}
	
	@Override
	public void create() {
		super.create();
		commandFactory = new DBCommandFactory();
	}	
	
	@Override
	public void onAdded() {
		super.onAdded();
		Log.i(NettyManager.class, "Start db manager for: "+config_connectionString+", user: "+config_userName+", password: "+config_password);
		try {
			dataSources = new PGPoolingDataSource();
			dataSources.setDataSourceName("DbSource");
			dataSources.setServerName(config_server);
			dataSources.setDatabaseName(config_db);
			dataSources.setUser(config_userName);
			dataSources.setPassword(config_password);
			dataSources.setMaxConnections(config_maxRequestPoolSize);
		} catch (Throwable ex) {
			Log.e(DBManager.class, "Fail to initialize connection", ex);
		}

	}
	
	
	public DBCommandFactory getCommandFactory() {
		return commandFactory;
	}
	
	public static DBCommandFactory commandFactory() {
		return get().getCommandFactory();
	}
	
	@Override
	public void config(JSONObject config) {
		super.config(config);
		JSONObject commandFactoryJson = config.optJSONObject("DBCommandFactory");
		commandFactory.configure(commandFactoryJson);
	}
	
		
	private DoubleLinkedList emptyRequestsPool = new DoubleLinkedList();
	public static final int MAX_REQUEST_POOL_SIZE = 1000 * 10; 
	private int config_maxRequestPoolSize =  MAX_REQUEST_POOL_SIZE;
	
	public DBRequest startRequest() {
		DBRequest request;
		synchronized(emptyRequestsPool) {			
			if (emptyRequestsPool.size() == 0) {
				request = new DBRequest();
			} else {
				request = (DBRequest) emptyRequestsPool.removeLast().getPayload();
			}
		}
		return request;
	}
	
	public void recycleRequest(DBRequest request) {
		request.recycle();
		synchronized(emptyRequestsPool) {
			if (emptyRequestsPool.size() < config_maxRequestPoolSize) {
				DoubleLinkedListNode node = new DoubleLinkedListNode(request);
				emptyRequestsPool.addLast(node);
			}
		}
	}
	
	private DoubleLinkedList executedRequests = new DoubleLinkedList();
	
	public void executeRequestNow(DBRequest request) {
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null; 
		try {
			connection = dataSources.getConnection();
			statement = connection.prepareStatement(request.getSql());
			request.setupRequest(statement);
			resultSet = statement.executeQuery();
			request.parseResponce(resultSet);
		} catch (SQLException ex) {
			request.setHasError(true);
			request.setError(ex);
		} catch (Exception ex) {
			request.setHasError(true);
			request.setError(ex);
		}
		finally {
			if (resultSet!=null)
				try {
					resultSet.close();
				} catch (SQLException e) {
					// TODO: Add handle errors
					e.printStackTrace();
				}
			if (statement!=null)
				try {
					statement.close();
				} catch (SQLException e) {
					// TODO: Add handle errors
					e.printStackTrace();
				}
			if (connection!=null)
				try {
					connection.close();
				} catch (SQLException e) {
					// TODO: Add handle errors
					e.printStackTrace();
				}
		}
		request.runOnComplete();
	}
	
	@Override
	public void onStart() {
		super.onStart();

	}
	
	@Override
	public void onRemoved() {
		// TODO Auto-generated method stub
		super.onRemoved();
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	@Override
	public void onError(Object... args) {
		// TODO Auto-generated method stub
		super.onError(args);
	}
	
	public void setConfig_connectionString(String config_connectionString) {
		this.config_connectionString = config_connectionString;
	}
	
	public void setConfig_password(String config_password) {
		this.config_password = config_password;
	}
	
	public void setConfig_userName(String config_userName) {
		this.config_userName = config_userName;
	}
	
	public void setConfig_db(String config_db) {
		this.config_db = config_db;
	}
	
	public void setConfig_server(String config_server) {
		this.config_server = config_server;
	}
	
	public void setConfig_max_db_connections(int config_max_db_connections) {
		this.config_max_db_connections = config_max_db_connections;
	}
	
	private Runnable runUpdateRequests = new  Runnable() {
		
		@Override
		public void run() {
			DBRequest request = null;
			synchronized (executedRequests) {
				if (executedRequests.size() > 0) {
					request = (DBRequest) executedRequests.getFirst().getPayload();
					executedRequests.remove(executedRequests.getFirst());
				}
			}
			if (request!=null) {
				executeRequestNow(request);
			}
			boolean haveRequests = false;
			synchronized (executedRequests) {
				if (executedRequests.size() > 0) {
					haveRequests = true;
				}
			}
			if (haveRequests) {
				threadPool.submit(runUpdateRequests);
			}
				
		}
	};
	
	public void runUpdateRequests() {
		threadPool.submit(runUpdateRequests);
	}

	public void executeAsync(DBRequest request) {
		synchronized (executedRequests) {
			executedRequests.addLast(new DoubleLinkedListNode(request));
		}		
		threadPool.submit(runUpdateRequests);
	}
}
