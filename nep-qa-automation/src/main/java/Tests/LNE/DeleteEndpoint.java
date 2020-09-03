package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.JsonUtil;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class DeleteEndpoint extends GenericTest {

    private LNEActions manager;
    private BaseAgentActions agent1, agent2;

    @Factory(dataProvider = "getData")
    public DeleteEndpoint(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "DeleteEndpoint" } )
    public void deleteEndpoint()  {

        try {
            JLog.logger.info("Starting DeleteEndpoint test ...");

            manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            agent1 = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
            agent2 = AgentActionsFactory.getAgentActions(data.get("EP_Type_2"), data.get("EP_HostName_2"), data.get("EP_UserName_2"), data.get("EP_Password_2"));

            // Set the endpoint name in the revoke configuration
            String deleteStandAloneWithEpNameConfigForEp1 = JsonUtil.ChangeTagConfiguration(data.get("deleteStandAlone"), "epName", agent1.getEpName());
            String originalEndpointId = agent1.getEpIdFromDbJson();

            manager.delete(deleteStandAloneWithEpNameConfigForEp1);
            agent1.checkDeleted(Integer.parseInt(general.get("EP Service Timeout")));
            agent2.checkNotDeleted();

            agent1.installEPIncludingRequisites(Integer.parseInt(general.get("EP Installation timeout")), Integer.parseInt(general.get("EP Service Timeout")), Integer.parseInt(general.get("From EP service start until logs show EP active timeout") ));
            String newEndpointId = agent1.getEpIdFromDbJson();

            if(originalEndpointId.compareTo(newEndpointId) == 0){
                org.testng.Assert.fail("RevokeEndpoint test failed, the unique id is the same after the installation.");
            }

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
        if(agent1!=null){
            agent1.close();
        }
        if(agent2!=null){
            agent2.close();
        }
    }


}
