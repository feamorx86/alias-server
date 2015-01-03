package ru.feamor.aliasserver.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;


public interface RequestParser {
	
	public boolean setupRequest(PreparedStatement statement, DBRequest request);
	
	public void parseResponce(DBRequest request, ResultSet result);

	public String getSql();
	
	public int id();
}