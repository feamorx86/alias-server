package ru.feamor.aliasserver.base;

import org.json.JSONObject;

public interface Configurable {
	void configure(JSONObject config);
}
