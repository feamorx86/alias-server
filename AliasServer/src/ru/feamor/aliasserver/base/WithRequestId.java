package ru.feamor.aliasserver.base;

public interface WithRequestId {
	int nextRequestId();
	int currentRequestId();
}