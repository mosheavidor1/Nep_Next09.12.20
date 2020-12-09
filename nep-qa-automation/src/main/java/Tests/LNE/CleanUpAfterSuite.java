package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class CleanUpAfterSuite extends GenericTest {
    BaseAgentActions endpoint = null;

    @Factory(dataProvider = "getData")
    public CleanUpAfterSuite(Object dataToSet) {
        super(dataToSet);
    }

    //must run at the end of suite
    @Test(groups = {"CleanupAfterSuite"}, priority = 9999)
    public void CleanUpAfterSuiteTest() {
        try {
            JLog.logger.info("Starting CleanUpAfterSuite test ...");
            endpoint = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
            endpoint.uninstallEndpoint(Integer.parseInt(data.get("Uninstall Timeout")));
            endpoint.close();

        } catch (Exception e) {
            org.testng.Assert.fail("Error during CleanUpAfterSuite test: "+ e.toString());

        }

    }


}


