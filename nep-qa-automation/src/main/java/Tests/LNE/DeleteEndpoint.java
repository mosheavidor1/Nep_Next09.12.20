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


public class DeleteEndpoint extends GenericTest {

    private LNEActions manager;
    private BaseAgentActions agent;
    private SimulatedAgentActions simulatedAgent;

    @Factory(dataProvider = "getData")
    public DeleteEndpoint(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "DeleteEndpoint" } )
    public void deleteEndpoint()  {

        try {
            JLog.logger.info("Starting DeleteEndpoint test ...");

            manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));

            long customerId = JsonUtil.GetCustomerIDFromSentConfiguration(data.get("Settings Json"));
            simulatedAgent = new SimulatedAgentActions(customerId, "1.2.3.4", "SimulatedAgentForDeleteTest", "84-7B-EB-21-99-99","Windows 10");

            // Set the endpoint name in the delete configuration
            String deleteStandAloneWithEpNameConfig = JsonUtil.ChangeTagConfiguration(data.get("deleteStandAlone"), "epName", agent.getEpName());
            String originalEndpointId = agent.getEpIdFromDbJson();

            manager.delete(deleteStandAloneWithEpNameConfig);
            agent.checkDeleted(Integer.parseInt(general.get("EP Service Timeout")));

            String checkUpdatesResponse = simulatedAgent.checkUpdates("simulatedAgentName", "1.1.1", "3", 0, "1.1.2");
            String action = JsonUtil.GetCheckUpdatesAction(checkUpdatesResponse);

            agent.installEPIncludingRequisites(Integer.parseInt(general.get("EP Installation timeout")), Integer.parseInt(general.get("EP Service Timeout")), Integer.parseInt(general.get("From EP service start until logs show EP active timeout") ));
            String newEndpointId = agent.getEpIdFromDbJson();

            // Verify the agent got new unique id to be sure it is a new installation
            if(originalEndpointId.compareTo(newEndpointId) == 0){
                org.testng.Assert.fail("RevokeEndpoint test failed, the unique id is the same after the installation.");
            }

            // Assert after installing the agent back
            // Verify the other agent didn't get uninstall command
            org.testng.Assert.assertEquals(action, "no update", String.format("check update result assertion failure. Expected: 'no update', got '%s' ", action));

            JLog.logger.info("DeleteEndpoint test completed.");

        } catch (Exception e) {
            org.testng.Assert.fail("DeleteEndpoint test failed " + "\n" + e.toString());
        }
    }

    @AfterMethod
    public void Close(){
        JLog.logger.info("Closing...");
        if(manager!=null){
            manager.Close();
        }
        if(agent !=null){
            agent.close();
        }
    }


}
