package ru.feamor.aliasserver;

import ru.feamor.aliasserver.components.Components;
import ru.feamor.aliasserver.config.ApplicationConfig;
import ru.feamor.aliasserver.config.ConfigurationFactory;
import ru.feamor.aliasserver.core.ComponentManager;
import ru.feamor.aliasserver.core.StaticInitialization;
import ru.feamor.aliasserver.utils.Log;

public class Application {
	
	public static final String CONFIG_PATH = "./config/config.json";
	public ApplicationConfig config;
	//TODO: getInstance()
	//TODO: application fatal
	
	private Application() {
		new StaticInitialization();
	}
	
	public void initialize() {
		Log.i("---------------------------------------------------------------------");
		Log.i("----------------         ALIAS       SERVER            --------------");
		Log.i("---------------------------------------------------------------------");
		Log.i("Server: Begin initialization");		
		ConfigurationFactory.initialize();
		Components.initialize();
		ComponentManager.initialize();
		Log.i("Server: Initialization complete");
	}
	
	public void config(String[] args) {
		Log.i("Server: begin configuration");
		config = new ApplicationConfig();
		config.fromFile(CONFIG_PATH);
		ConfigurationFactory.get().configure(config.getConfig());
		ComponentManager.get().configure(config.getConfig());
		Log.i("Server: Configuration complete");
	}
	
	public void launch() {
		Log.i("Server: Launch!");
		Log.i("Server: Launch components");
		ComponentManager.get().start();
		Log.i("Server: Launched!");
	}
	
	
	
	/**
	 * @param args
	 */
		
	public static void main(String[] args) {
		Application application = new Application();
		application.initialize();
		application.config(args);
		application.launch();
	}

}
