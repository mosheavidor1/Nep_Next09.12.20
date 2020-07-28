package Tests.LNE;

import Actions.LNEActions;
import Actions.AgentActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class ChangeCustomerConfiguration extends GenericTest {

    private LNEActions manager;
    private AgentActions endpoint;

    @Factory(dataProvider = "getData")
    public ChangeCustomerConfiguration(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "ChangeConfiguration" } )
    public void ChangeCustomerConfigurationAndVerify()  {

        JLog.logger.info("Opening...");

        manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
        endpoint = new AgentActions(data.get("EP_HostName_1"),data.get("EP_UserName_1"), data.get("EP_Password_1"));
        String confJson =data.get("Settings Json");
        manager.SetCustomerConfiguration(confJson);
        endpoint.StopEPService(Integer.parseInt(general.get("EP Service Timeout")));
        endpoint.StartEPService(Integer.parseInt(general.get("EP Service Timeout")));
        endpoint.CompareConfigurationToEPConfiguration(confJson);

    }

    @AfterMethod
    public void Close(){
        JLog.logger.info("Closing...");
        if (endpoint!=null) {
            endpoint.Close();
        }
    }


}
