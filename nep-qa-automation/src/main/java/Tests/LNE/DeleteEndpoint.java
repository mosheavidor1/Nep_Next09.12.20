package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.CheckUpdatesActions;
import Actions.LNEActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class DeleteEndpoint extends GenericTest {

    private LNEActions lennyActions;
    private BaseAgentActions agent;
    private SimulatedAgentActions simulatedAgent;
    private String SimulatedAgentName = "SimulatedAgentForDeleteTest";
    private String SimulatedAgentIp = "1.2.3.4";
    private String SimulatedAgentBinVer = "1.1.1";
    private String customerId;

    @Factory(dataProvider = "getData")
    public DeleteEndpoint(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
    }

    @Test(groups = { "DeleteEndpoint" } )
    public void deleteEndpoint()  {

        try {
            JLog.logger.info("Starting DeleteEndpoint test ...");

            lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));

            simulatedAgent = new SimulatedAgentActions();
            simulatedAgent.register(customerId, SimulatedAgentIp, SimulatedAgentName, "84-7B-EB-21-99-99","Windows 10");

            String originalEndpointId = agent.getEpIdFromDbJson();

            lennyActions.deleteAndVerifyResponse(customerId, agent.getEpName());
            agent.checkDeleted(Integer.parseInt(general.get("Check Updates Timeout")));

            String action = simulatedAgent.sendCheckUpdatesAndGetAction(SimulatedAgentName, SimulatedAgentBinVer, 3, 0, "1.1.2", customerId);


            agent.reinstallEndpoint(Integer.parseInt(general.get("EP Installation timeout")), Integer.parseInt(general.get("EP Service Timeout")), Integer.parseInt(general.get("From EP service start until logs show EP active timeout") ));
            String newEndpointId = agent.getEpIdFromDbJson();

            lennyActions.deleteWithoutVerify(customerId, SimulatedAgentName);
            simulatedAgent.sendCheckUpdatesAndGetResponse(SimulatedAgentName, SimulatedAgentBinVer, 3, 0, "1.1.2", customerId);

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
    	
        if(lennyActions!=null){
            lennyActions.Close();
        }
        if(agent !=null){
            agent.close();
        }
    }


}
