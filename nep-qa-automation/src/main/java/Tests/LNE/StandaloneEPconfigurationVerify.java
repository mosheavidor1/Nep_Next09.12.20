package Tests.LNE;

import Actions.AgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class StandaloneEPconfigurationVerify extends GenericTest {

    private LNEActions manager;
    private AgentActions endpoint;
    static final String config_win = "c:\\ProgramData\\Trustwave\\NEPAgent\\config.json";
    static final String config_linux = "/opt/tw-endpoint/data/config.json";
    static final String settings_toVerify = "\"check_update_period\" : 53";
    @Factory(dataProvider = "getData")
    public StandaloneEPconfigurationVerify(Object dataToSet) {
        super(dataToSet);
    }

    @Test()
    public void ChangeStandaloneEPconfigurationVerify()  {
        try {
            String result;
            JLog.logger.info("Opening...");
            AgentActions.EP_OS epOs = data.get("EP_Type_1").contains("win") ? AgentActions.EP_OS.WINDOWS : AgentActions.EP_OS.LINUX;
            manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            endpoint = new AgentActions(data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
            // set stand alone config to Linux EP
            String StandAloneConfJson = data.get("StandAloneConfig");
            manager.SetCustomerConfiguration(StandAloneConfJson);
            endpoint.StopEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
            endpoint.StartEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
//        endpoint.CompareConfigurationToEPConfiguration(StandAloneConfJson, epOs);
            if (epOs == AgentActions.EP_OS.LINUX)
                result = endpoint.findInText(config_linux, settings_toVerify);
            else
                result = endpoint.findInText(config_win, settings_toVerify);
            if (null != result)
                JLog.logger.info("res: " + result);
        }
                catch (Exception e) {
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify ." + "\n" + e.toString());
            }
    }

    @AfterMethod
    public void Close(){
        JLog.logger.info("Closing...");
        if (endpoint!=null) {
            endpoint.Close();
        }
        if(manager!=null){
            manager.Close();
        }
    }


}
