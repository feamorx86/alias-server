package ru.feamor.aliasserver.utils;

public interface RunWithParams<T> extends Runnable {
	void setParam(T value);
}
