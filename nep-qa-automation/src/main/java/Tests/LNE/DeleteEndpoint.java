package Tests.LNE;

import Actions.AgentActions;
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
    private AgentActions endpoint1, endpoint2;

    @Factory(dataProvider = "getData")
    public DeleteEndpoint(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "DeleteEndpoint" } )
    public void DeleteEndpoint()  {

        try {
            JLog.logger.info("Starting DeleteEndpoint test ...");

            manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            endpoint1 = new AgentActions(data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"), data.get("EP_Type_1"));
            endpoint2 = new AgentActions(data.get("EP_HostName_2"), data.get("EP_UserName_2"), data.get("EP_Password_2"), data.get("EP_Type_2"));

            // Set the endpoint name in the revoke configuration
            String deleteStandAloneWithEpNameConfigForEp1 = JsonUtil.ChangeTagConfiguration(data.get("deleteStandAlone"), "epName", endpoint1.getEpName());

            manager.delete(deleteStandAloneWithEpNameConfigForEp1);
            endpoint1.CheckDeleted(Integer.parseInt(general.get("EP Service Timeout")));
            endpoint2.CheckNotDeleted();

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
        if(endpoint1!=null){
            endpoint1.Close();
        }
        if(endpoint2!=null){
            endpoint2.Close();
        }
    }


}
