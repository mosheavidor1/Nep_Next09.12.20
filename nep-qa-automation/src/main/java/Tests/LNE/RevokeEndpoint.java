package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.JsonUtil;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;



public class RevokeEndpoint extends GenericTest {

    private LNEActions lennyActions;
    private BaseAgentActions agent;
    private SimulatedAgentActions simulatedAgent;
    private String customerId;

    @Factory(dataProvider = "getData")
    public RevokeEndpoint(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
    }

    @Test(groups = { "RevokeEndpoint" } )
    public void revokeEndpoint()  {

        try {
            JLog.logger.info("Starting RevokeEndpoint test ...");

            lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            agent =  AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));

            simulatedAgent = new SimulatedAgentActions(customerId, "1.2.3.4", "SimulatedAgentForRevokeTest", "84-7B-EB-21-99-99","Windows 10");

            lennyActions.revoke(customerId, agent.getEpName());
            agent.checkRevoked(Integer.parseInt(general.get("EP Service Timeout")));

            String checkUpdatesResponse = simulatedAgent.checkUpdates("simulatedAgentName", "1.1.1", 3, 0, "1.1.2");
            String action = JsonUtil.GetCheckUpdatesAction(checkUpdatesResponse);

            agent.installEPIncludingRequisites(Integer.parseInt(general.get("EP Installation timeout")), Integer.parseInt(general.get("EP Service Timeout")), Integer.parseInt(general.get("From EP service start until logs show EP active timeout") ));

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
        JLog.logger.info("Closing...");
        if(lennyActions!=null){
            lennyActions.Close();
        }
        if(agent!=null){
            agent.close();
        }
    }


}
