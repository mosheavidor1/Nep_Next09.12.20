package Actions;

public enum CheckUpdatesActions {
	
	CONFIGURATION_UPDATE("configuration update"),
	CONFIGURATION_SWITCH("configuration switch"), //conf source switched
	NO_UPDATE("no update"),
	UNINSTALL("uninstall");
	
	private String actionName;
	
	private CheckUpdatesActions(String actionName){
		this.actionName = actionName;
	}

	public String getActionName() {
		return actionName;
	}
	

}
