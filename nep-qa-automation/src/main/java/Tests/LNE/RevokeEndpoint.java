package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.DsMgmtActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;



public class RevokeEndpoint extends GenericTest {

    private BaseAgentActions agent;
    private SimulatedAgentActions simulatedAgent;
    private String SimulatedAgentName = "SimulatedAgentForRevokeTest";
    private String SimulatedAgentIp = "1.2.3.4";
    private String SimulatedAgentBinVer = "1.1.1";
    private String customerId;

    @Factory(dataProvider = "getData")
    public RevokeEndpoint(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }

    @Test(groups = { "RevokeEndpoint" } , priority = 101)
    public void revokeEndpoint()  {

        try {
            JLog.logger.info("Starting RevokeEndpoint test ...");

            agent =  AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));

            simulatedAgent = new SimulatedAgentActions(customerId);
            simulatedAgent.register(customerId, SimulatedAgentIp, SimulatedAgentName, "84-7B-EB-21-99-99","Windows 10");

            DsMgmtActions.revoke(customerId, agent.getEpName());
            agent.checkDeleted(Integer.parseInt(getGeneralData().get("EP Installation timeout")));

            String action = simulatedAgent.sendCheckUpdatesAndGetAction(SimulatedAgentName, SimulatedAgentBinVer, 3, 0, "1.1.2", customerId);


            agent.reinstallEndpoint(Integer.parseInt(getGeneralData().get("EP Installation timeout")), Integer.parseInt(getGeneralData().get("EP Service Timeout")), Integer.parseInt(getGeneralData().get("From EP service start until logs show EP active timeout") ));

            DsMgmtActions.deleteWithoutVerify(customerId, SimulatedAgentName);
            simulatedAgent.sendCheckUpdatesAndGetResponse(SimulatedAgentName, SimulatedAgentBinVer, 3, 0, "1.1.2", customerId);

            // Assert after installing the agent back
            // Verify the other agent didn't get uninstall command
            org.testng.Assert.assertEquals(action, "no update", String.format("check update result assertion failure. Expected: 'no update', got '%s' ", action));

            JLog.logger.info("RevokeEndpoint test completed.");

        } catch (Exception e) {
            org.testng.Assert.fail("RevokeEndpoint test failed " + "\n" + e.toString());
        }
    }

    @AfterMethod
    public void Close(){
    	
        if(agent!=null){
            agent.close();
        }
    }


}
