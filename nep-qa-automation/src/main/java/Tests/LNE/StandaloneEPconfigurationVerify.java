package Tests.LNE;

import Actions.AgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.JsonUtil;
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
            String ep_name = endpoint.getEPName();
            if (null == ep_name)
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify ep_name is invalid: " + ep_name);
            String updatedConfig = JsonUtil.ChangeEpNameConfiguration(StandAloneConfJson, ep_name);
            if (null == updatedConfig)
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify updatedConfig is invalid: " + updatedConfig);
            manager.SetCustomerConfiguration(updatedConfig);
            endpoint.StopEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
            endpoint.StartEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
//            endpoint.CompareConfigurationToEPConfiguration(StandAloneConfJson, epOs);
            if (epOs == AgentActions.EP_OS.LINUX)
                result = endpoint.findInText(config_linux, settings_toVerify);
            else
                result = endpoint.findInText(config_win, settings_toVerify);
            if (null != result)
                JLog.logger.info("res: " + result);

            Thread.sleep(10000);
            String RevokeAloneConfJson = data.get("revokeStandAlone");
            manager.revokeEpConfiguration(RevokeAloneConfJson);
            Thread.sleep(10000);
            endpoint.StopEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
            endpoint.StartEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);

            Thread.sleep(10000);
            String StandAloneSet = data.get("StandAloneSet");
            manager.SetCustomerConfiguration(StandAloneSet);
            Thread.sleep(10000);
            endpoint.StopEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
            endpoint.StartEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
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
