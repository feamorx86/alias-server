package ru.feamor.aliasserver.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;


public interface RequestParser {
	
	public abstract void setupRequest(PreparedStatement statement, DBRequest request);
	
	public abstract void parseResponce(DBRequest request, ResultSet result);

	public String getSql();
	
	public abstract int id();
}