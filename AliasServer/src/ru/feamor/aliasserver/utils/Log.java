package ru.feamor.aliasserver.utils;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LoggerContext;

public class Log {
	
	private static Logger logger;
	private static final Marker MSG_MARKER = MarkerManager.getMarker("MSG");
	private static final Marker CLASS_MARKER = MarkerManager.getMarker("CLASS").setParents(MSG_MARKER);
		
	public static void initialize(String log4jConfigPath) {
		org.apache.logging.log4j.core.config.Configurator.initialize("Log", log4jConfigPath);
		logger = LogManager.getLogger("Log");
	}
	
	public static void i(String info) {
		logger.info(info);
	}
	
	public static void i(String tag, String info) {
		logger.info(CLASS_MARKER, "CLASS  {} | MSG | {}", tag, info);
	}
	
	public static void i(Class clazz, String info) {
		logger.info(CLASS_MARKER, "CLASS  {} | MSG | {}", clazz.getSimpleName(), info);
	}
	
	public static void e(String error) {
		logger.error(error);
	}
	
	public static void e(String tag, String error) {
		logger.error(CLASS_MARKER, "CLASS {} | MSG | {}", tag, error);
	}
	
	public static void e(Class clazz, String error) {
		logger.error(CLASS_MARKER, "CLASS {} | MSG | {}", clazz.getSimpleName(), error);
	}
	
	public static void e(Class clazz, String error, Throwable throwable) {
		logger.error(CLASS_MARKER, "CLASS {} | MSG | {}", clazz.getSimpleName(), error, throwable);
	}	
	
}
