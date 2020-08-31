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

import java.time.Duration;
import java.time.LocalDateTime;


public class RevokeEndpoint extends GenericTest {

    private LNEActions manager;
    private AgentActions endpoint1, endpoint2;

    @Factory(dataProvider = "getData")
    public RevokeEndpoint(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "RevokeEndpoint" } )
    public void RevokeEndpoint()  {

        try {
            JLog.logger.info("Starting RevokeEndpoint test ...");

            manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            endpoint1 = new AgentActions(data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"), data.get("EP_Type_1"));
            endpoint2 = new AgentActions(data.get("EP_HostName_2"), data.get("EP_UserName_2"), data.get("EP_Password_2"), data.get("EP_Type_2"));

            // Set the endpoint name in the revoke configuration
            String revokeStandAloneWithEpNameConfigForEp1 = JsonUtil.ChangeTagConfiguration(data.get("revokeStandAlone"), "epName", endpoint1.getEpName());

            manager.revoke(revokeStandAloneWithEpNameConfigForEp1);
            endpoint1.CheckRevoked(Integer.parseInt(general.get("EP Service Timeout")));
            endpoint2.CheckNotRevoked();

            JLog.logger.info("RevokeEndpoint test completed.");

        } catch (Exception e) {
            org.testng.Assert.fail("RevokeEndpoint test failed " + "\n" + e.toString());
        }
    }

    @AfterMethod
    public void Close(){
        JLog.logger.info("Closing...");
        if(manager!=null){
            manager.Close();
        }
    }


}
