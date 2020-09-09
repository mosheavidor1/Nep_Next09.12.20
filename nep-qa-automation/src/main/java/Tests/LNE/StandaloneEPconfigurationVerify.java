package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

//https://jira.trustwave.com/browse/NEP-1252
public class StandaloneEPconfigurationVerify extends GenericTest {

    private LNEActions lennyActions;
    private BaseAgentActions agent, agent2;
    static final String settings_toVerify_Alone = "\"check_update_period\":53";
    static final String settings_toVerify_Set = "\"check_update_period\":311";
    private String customerId;
    
    @Factory(dataProvider = "getData")
    public StandaloneEPconfigurationVerify(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
    }

    @Test()
    public void ChangeStandaloneEPconfigurationVerify()  {
    	/*
    	 JLog.logger.info("Starting ChangeStandaloneEPconfigurationVerify test ...");
    	
        try {

         agent2 =  AgentActionsFactory.getAgentActions(data.get("EP_Type_2"), data.get("EP_HostName_2"), data.get("EP_UserName_2"), data.get("EP_Password_2"));
        } catch (Exception e) {
            org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify: This test requires usage of more than 1 EP. Need to configure another EP" + "\n" + e.toString());
            org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify Failed");
        }
        try {
           
            lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
            // 1.set stand alone config to endpoint
            String StandAloneConfJson = data.get("StandAloneSet");
           // String ep_name = agent.getEpName();
           // if (null == ep_name)
           //     org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify ep_name is invalid: " + ep_name);


           // ep_name = ep_name.replaceAll("^\n+", "");
           // lennyActions.SetCustomerConfiguration(customerId, updatedConfig);
            //TODO: use setEndpointConfig
            agent.stopEPService(Integer.parseInt(general.get("EP Service Timeout")));
            agent.startEPService(Integer.parseInt(general.get("EP Service Timeout")));
            Thread.sleep(5000);

            // Verify on Stand Alone EP
             if (null == verifyPatternInConfig(agent, settings_toVerify_Alone))
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify configuration was not updated? - " + settings_toVerify_Alone + " was not found");
            // Verify that configuration on second EP was not updated
            if (null != verifyPatternInConfig(agent2, settings_toVerify_Alone))
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify configuration was update on second EP - Error - " + settings_toVerify_Alone + " was found");


            Thread.sleep(10000);
            // 2.set revoke/ uninstall config to endpoint
           
            lennyActions.revokeEpConfiguration(customerId, ep_name);
            Thread.sleep(10000);
            agent.stopEPService(Integer.parseInt(general.get("EP Service Timeout")));
            agent.startEPService(Integer.parseInt(general.get("EP Service Timeout")));
            Thread.sleep(10000);
            // Verify that configuration on second EP was not updated
            if (null != verifyPatternInConfig(agent2, settings_toVerify_Alone))
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify configuration was update on second EP - Error - " + settings_toVerify_Alone + " was found");

            // 3.set config to both endpoints
            String StandAloneSet = data.get("StandAloneSet");
            lennyActions.SetCustomerConfiguration(customerId, StandAloneSet);
            Thread.sleep(10000);
            agent.stopEPService(Integer.parseInt(general.get("EP Service Timeout")));
            agent.startEPService(Integer.parseInt(general.get("EP Service Timeout")));
            agent2.stopEPService(Integer.parseInt(general.get("EP Service Timeout")));
            agent2.startEPService(Integer.parseInt(general.get("EP Service Timeout")));
            Thread.sleep(10000);
            // Verify that configuration on stand alone EP was updated
            if (null == verifyPatternInConfig(agent, settings_toVerify_Set))
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify configuration on stand alone EP was not updated - " + settings_toVerify_Set + " was not found");
          // Verify that configuration on second EP was updated
            if (null == verifyPatternInConfig(agent2, settings_toVerify_Set))
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify configuration on second EP was not updated - " + settings_toVerify_Set + " was not found");
        }
                catch (Exception e) {
                org.testng.Assert.fail("ChangeStandaloneEPconfigurationVerify failed " + "\n" + e.toString());
            }*/
    }

    String verifyPatternInConfig(BaseAgentActions agent, String pattern) {
        String result;
        String conf_path = agent.getConfigPath(true);
        result = agent.findInText(conf_path, pattern);
        return result;
    }

    @AfterMethod
    public void Close(){
        JLog.logger.info("Closing...");
        if (agent!=null) {
            agent.close();
        }
        if (agent2!=null) {
            agent2.close();
        }
        if(lennyActions!=null){
            lennyActions.Close();
        }
    }


}
