package ru.feamor.aliasserver.db;

import ru.feamor.aliasserver.utils.RunWithParams;

public abstract class RunWithParamsDBRequest implements RunWithParams<DBRequest> {
		
	protected DBRequest param;
	
	@Override
	public void setParam(DBRequest value) {
		this.param = value;
	}
}