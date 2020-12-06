package Actions;

import Utils.Logs.JLog;

public class AgentActionsFactory {
	
	public static BaseAgentActions getAgentActions(String agentType, String epIp, String epUserName, String epPassword) {
		try {
			BaseAgentActions baseAction = null;
			if (agentType.contains("win") || agentType.contains("msi")) {
				baseAction = new WinAgentActions(epIp, epUserName, epPassword);
			} else if (agentType.contains("linux") || agentType.contains("lnx") || agentType.contains("ubu")) {
				baseAction = new LinuxAgentActions(epIp, epUserName, epPassword);
			} else {
				JLog.logger.error("This type of endpoint is unsupported {}. Need to implement the specific AgentActions", agentType);
				org.testng.Assert.fail("This type of endpoint is unsupported: " + agentType);
			}
			baseAction.osName = agentType;
			return baseAction;

		}
		catch (Exception e){
			org.testng.Assert.fail("Error in creating agent actions. ip: " +  epIp + " type: " +agentType +"\n" + e.toString());
			return null;
		}
	}

}
