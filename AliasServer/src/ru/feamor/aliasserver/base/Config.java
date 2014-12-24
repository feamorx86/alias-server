package ru.feamor.aliasserver.base;

import org.json.JSONObject;

public interface Config<T> {
	void configure(JSONObject config, T value);
}
