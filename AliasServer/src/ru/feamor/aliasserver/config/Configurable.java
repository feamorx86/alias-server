package ru.feamor.aliasserver.config;

import org.json.JSONObject;

public interface Configurable {
	void configure(JSONObject config);
}
