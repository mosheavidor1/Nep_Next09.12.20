package Actions;

import Utils.Logs.JLog;

public class AgentActionsFactory {
	
	public static BaseAgentActions getAgentActions(String agentType, String epIp, String epUserName, String epPassword) {
		if (agentType.contains("win")) {
			return new WinAgentActions(epIp, epUserName, epPassword);
		}
		if (agentType.contains("linux") || agentType.contains("lnx")) {
			return new LinuxAgentActions(epIp, epUserName, epPassword);
		}
		JLog.logger.error("This type of endpoint is unsupported {}. Need to implement the specific AgentActions", agentType);
		throw new RuntimeException("This type of endpoint is unsupported: " + agentType);
	}

}
