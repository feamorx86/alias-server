package ru.feamor.aliasserver.config;

import org.json.JSONObject;

public interface Config<T> {
	void configure(JSONObject config, T value);
}
