package Tests.LNE;

import Actions.AgentActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class VerifyConfiguration extends GenericTest {

    private AgentActions endpoint;

    @Factory(dataProvider = "getData")
    public VerifyConfiguration(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "VerifyConfiguration" } )
    public void VerifyEndpointConfiguration()  {

        JLog.logger.info("Opening...");

        endpoint = new AgentActions(data.get("EP_HostName_1"),data.get("EP_UserName_1"), data.get("EP_Password_1"));
        AgentActions.EP_OS epOs = data.get("EP_Type_1").contains("win") ? AgentActions.EP_OS.WINDOWS : AgentActions.EP_OS.LINUX;
        endpoint.StopEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
        endpoint.StartEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
        endpoint.CompareConfigurationToEPConfiguration(epOs);

    }

    @AfterMethod
    public void Close(){
        JLog.logger.info("Closing...");
        if (endpoint!=null) {
            endpoint.Close();
        }
    }


}
