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

//https://jira.trustwave.com/browse/NEP-1252
public class StandaloneEPconfigurationVerify extends GenericTest {

    private LNEActions manager;
    private AgentActions endpoint, endpoint2;
    static final String config_win = "/C:/ProgramData/Trustwave/NEPAgent/config.json";
    static final String config_linux = "/opt/tw-endpoint/data/config.json";
    static final String settings_toVerify_Alone = "\"check_update_period\":53";
    static final String settings_toVerify_Set = "\"check_update_period\":311";
    @Factory(dataProvider = "getData")
    public StandaloneEPconfigurationVerify(Object dataToSet) {
        super(dataToSet);
    }

    @Test()
    public void ChangeStandaloneEPconfigurationVerify()  {
        try {
            endpoint2 = new AgentActions(data.get("AnotherEP_IP"), data.get("AnotherEP_User"), data.get("AnotherEP_Password"));
        } catch (Exception e) {
            org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify: This test requires usage of more than 1 EP. Need to configure another EP" + "\n" + e.toString());
            org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify Failed");
        }
        try {
            String result;
            JLog.logger.info("Opening...");
            AgentActions.EP_OS epOs = data.get("EP_Type_1").contains("win") ? AgentActions.EP_OS.WINDOWS : AgentActions.EP_OS.LINUX;
            manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            endpoint = new AgentActions(data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
            AgentActions.EP_OS ep2Os = data.get("AnotherEP_Type").contains("win") ? AgentActions.EP_OS.WINDOWS : AgentActions.EP_OS.LINUX;
            // 1.set stand alone config to endpoint
            String StandAloneConfJson = data.get("StandAloneConfig");
            String ep_name = endpoint.getEPName();
            if (null == ep_name)
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify ep_name is invalid: " + ep_name);


            ep_name = ep_name.replaceAll("^\n+", "");
            String updatedConfig = JsonUtil.ChangeTagConfiguration(StandAloneConfJson, "name", ep_name);
            if (null == updatedConfig)
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify updatedConfig is invalid: " + updatedConfig);
            manager.SetCustomerConfiguration(updatedConfig);
            endpoint.StopEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
            endpoint.StartEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
            Thread.sleep(5000);

            // Verify on Stand Alone EP
             if (null == verifyPatternInConfig(endpoint, epOs, settings_toVerify_Alone))
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify configuration was not updated? - " + settings_toVerify_Alone + " was not found");
            // Verify that configuration on second EP was not updated
            if (null != verifyPatternInConfig(endpoint2, ep2Os, settings_toVerify_Alone))
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify configuration was update on second EP - Error - " + settings_toVerify_Alone + " was found");


            Thread.sleep(10000);
            // 2.set revoke/ uninstall config to endpoint
            String RevokeAloneConfJson = data.get("revokeStandAlone");
            updatedConfig = JsonUtil.ChangeTagConfiguration(RevokeAloneConfJson, "epName", ep_name);
            if (null == updatedConfig)
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify updatedConfig is invalid: " + updatedConfig);
            manager.revokeEpConfiguration(updatedConfig);
            Thread.sleep(10000);
            endpoint.StopEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
            endpoint.StartEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
            Thread.sleep(5000);
            // Verify that configuration on second EP was not updated
            if (null != verifyPatternInConfig(endpoint2, ep2Os, settings_toVerify_Alone))
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify configuration was update on second EP - Error - " + settings_toVerify_Alone + " was found");

            // 3.set config to both endpoints
            String StandAloneSet = data.get("StandAloneSet");
            manager.SetCustomerConfiguration(StandAloneSet);
            Thread.sleep(10000);
            endpoint.StopEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
            endpoint.StartEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
            endpoint2.StopEPService(Integer.parseInt(general.get("EP Service Timeout")), ep2Os);
            endpoint2.StartEPService(Integer.parseInt(general.get("EP Service Timeout")), ep2Os);
            Thread.sleep(5000);
            // Verify that configuration on stand alone EP was updated
            if (null == verifyPatternInConfig(endpoint, epOs, settings_toVerify_Set))
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify configuration on stand alone EP was not updated - " + settings_toVerify_Set + " was not found");
          // Verify that configuration on second EP was updated
            if (null == verifyPatternInConfig(endpoint2, ep2Os, settings_toVerify_Set))
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify configuration on second EP was not updated - " + settings_toVerify_Set + " was not found");
        }
                catch (Exception e) {
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify failed " + "\n" + e.toString());
            }
    }

    String verifyPatternInConfig(AgentActions endpoint, AgentActions.EP_OS epOs, String pattern) {
        String result;
        if (epOs == AgentActions.EP_OS.LINUX)
            result = endpoint.findInText(config_linux, pattern);
        else
            result = endpoint.findInText(config_win, pattern);
        return result;
    }

    @AfterMethod
    public void Close(){
        JLog.logger.info("Closing...");
        if (endpoint!=null) {
            endpoint.Close();
        }
        if (endpoint2!=null) {
            endpoint2.Close();
        }
        if(manager!=null){
            manager.Close();
        }
    }


}
