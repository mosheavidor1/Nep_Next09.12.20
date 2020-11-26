package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.DsMgmtActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.ConfigHandling;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


//https://jira.trustwave.com/browse/NEP-1257
public class RevokeEndpoint extends GenericTest {

    private BaseAgentActions agent;
    private SimulatedAgentActions simulatedAgent;
    private String SimulatedAgentName = "SimulatedAgentForRevokeTest";
    private String SimulatedAgentIp = "1.2.3.4";
    private String customerId;

    @Factory(dataProvider = "getData")
    public RevokeEndpoint(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }

    @Test(groups = { "RevokeEndpoint" } , priority = 11)
    public void revokeEndpoint()  {

        try {
            JLog.logger.info("Starting RevokeEndpoint test with {} agent", data.get("EP_Type_1"));
            
            JLog.logger.info("Going to set default configuration for this customer. Check update period - 30 seconds");
            DsMgmtActions.setCustomerConfig(getGeneralData().get("Customer Id"), ConfigHandling.getDefaultConfiguration());
            

            agent =  AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
            String originalEndpointId = agent.getEpIdFromDbJson();

            JLog.logger.info("Going to register another simulated agent with name {}", SimulatedAgentName);
            simulatedAgent = new SimulatedAgentActions(getGeneralData().get("DS Name"), customerId);
            simulatedAgent.register(customerId, SimulatedAgentIp, SimulatedAgentName, "84-7B-EB-21-99-99","Windows 10");

            JLog.logger.info("Going to revoke the real agent and make sure the service is uninstalled");
            DsMgmtActions.revoke(customerId, agent.getEpName());
            boolean deleted = agent.checkDeleted(Integer.parseInt(getGeneralData().get("EP Installation timeout")));
            if(!deleted){
                org.testng.Assert.fail("Endpoint deleted verification failed, the endpoint service still running.");
            }

            JLog.logger.info("Going to make sure that the simulated agent keeps getting updates and not 'uninstall'");
            String action = simulatedAgent.sendCheckUpdatesAndGetAction(SimulatedAgentName, GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
            org.testng.Assert.assertNotEquals(action, "uninstall", "check update result assertion failure.");

            JLog.logger.info("Going to install the origin agent back and make sure it gets the same uuid as before");
            agent.installEndpoint(Integer.parseInt(getGeneralData().get("EP Installation timeout")), Integer.parseInt(getGeneralData().get("EP Service Timeout")));
            String newEndpointId = agent.getEpIdFromDbJson();
            org.testng.Assert.assertEquals(originalEndpointId, newEndpointId, "New uuid should equals the one before revoking.");

            JLog.logger.info("Finished RevokeEndpoint successfully.");

        } catch (Exception e) {
            org.testng.Assert.fail("RevokeEndpoint test failed " + "\n" + e.toString());
        }
    }

    @AfterMethod
    public void Close(){
    	
        if(agent!=null){
            agent.close();
        }
        if (simulatedAgent != null) {
        	JLog.logger.info("Going to remove the simulated agent");
            DsMgmtActions.deleteWithoutVerify(customerId, SimulatedAgentName);
            simulatedAgent.sendCheckUpdatesAndGetResponse(SimulatedAgentName, GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);

        }
    }


}
