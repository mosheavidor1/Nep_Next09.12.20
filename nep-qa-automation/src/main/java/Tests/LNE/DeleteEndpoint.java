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


public class DeleteEndpoint extends GenericTest {

    private BaseAgentActions agent;
    private SimulatedAgentActions simulatedAgent;
    private String SimulatedAgentName = "SimulatedAgentForDeleteTest";
    private String SimulatedAgentIp = "1.2.3.4";
    private String SimulatedAgentBinVer = "1.1.1";
    private String customerId;

    @Factory(dataProvider = "getData")
    public DeleteEndpoint(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }

    @Test(groups = { "DeleteEndpoint" }, priority = 100 )
    public void deleteEndpoint()  {

        try {
            JLog.logger.info("Starting DeleteEndpoint test ...");

            agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));

            simulatedAgent = new SimulatedAgentActions(getGeneralData().get("DS Name"), customerId);
            simulatedAgent.register(customerId, SimulatedAgentIp, SimulatedAgentName, "84-7B-EB-21-99-99","Windows 10");

            String originalEndpointId = agent.getEpIdFromDbJson();

            DsMgmtActions.deleteAndVerifyResponse(customerId, agent.getEpName());
            agent.checkDeleted(Integer.parseInt(getGeneralData().get("EP Installation timeout")));

            String action = simulatedAgent.sendCheckUpdatesAndGetAction(SimulatedAgentName, SimulatedAgentBinVer, 3, 0, "1.1.2", customerId);


            agent.reinstallEndpoint(Integer.parseInt(getGeneralData().get("EP Installation timeout")), Integer.parseInt(getGeneralData().get("EP Service Timeout")), Integer.parseInt(getGeneralData().get("From EP service start until logs show EP active timeout") ));
            String newEndpointId = agent.getEpIdFromDbJson();

            DsMgmtActions.deleteWithoutVerify(customerId, SimulatedAgentName);
            simulatedAgent.sendCheckUpdatesAndGetResponse(SimulatedAgentName, SimulatedAgentBinVer, 3, 0, "1.1.2", customerId);

            // Verify the agent got new unique id to be sure it is a new installation
            if(originalEndpointId.compareTo(newEndpointId) == 0){
                org.testng.Assert.fail("Delete endpoint test failed, the unique id is the same after the installation. Original EP IP: "+ originalEndpointId + " New EP ID: " + newEndpointId + " Machine: " + data.get("EP_HostName_1") );
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
    	
        if(agent !=null){
            agent.close();
        }
    }


}
