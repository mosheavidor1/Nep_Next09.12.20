package Tests.LNE;

import Actions.BaseAgentActions;
import Actions.CheckUpdatesActions;
import Actions.LNEActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.JsonUtil;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

//https://jira.trustwave.com/browse/NEP-1252
public class StandaloneEPconfigurationVerify extends GenericTest {

    private LNEActions lennyActions;
    
    private static final String tag_to_update = "ds_max_off_perios";
    private static final String customerLevelValue = "10";
    private static final String endpointLevelValue = "15";
    
    private static final String secondCustomerLevelValue = "20";
    
    private String customerId;
    
    private static final String simulatedAgentIp1 = "1.2.3.4";
    private static final String simulatedAgentIp2 = "1.2.3.5";
    private static final String simulatedAgentOs = "Windows 10";
    private static final String simulatedAgentMac1 = "84-7B-EB-21-22";
    private static final String simulatedAgentMac2 = "84-7B-EB-21-23";
    private static final String simulatedAgentName1 = "ep1";
    private static final String simulatedAgentName2 = "ep2";
    
    SimulatedAgentActions simulatedAgent1 = null;
    SimulatedAgentActions simulatedAgent2 = null;
    
    
    
    @Factory(dataProvider = "getData")
    public StandaloneEPconfigurationVerify(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
        lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
    }

    @Test(groups = { "StandaloneEPconfiguration" })
    public void verifyStandaloneEPconfiguration()  {
    	try {
            JLog.logger.info("Starting StandaloneEPconfigurationVerify::verifyStandaloneEPconfiguration ...");
            
            String customerConf = data.get("config");
            customerConf = JsonUtil.ChangeTagConfiguration(customerConf, tag_to_update, customerLevelValue);
            
            //Prepare for verification
            JSONObject customerConfigSent = new JSONObject(customerConf);
            customerConfigSent.remove("centcom_meta");
            
            String epConf = data.get("config");
            epConf = JsonUtil.ChangeTagConfiguration(epConf, tag_to_update, endpointLevelValue);
            
            //Prepare for verification
            JSONObject epConfigSent = new JSONObject(epConf);
            epConfigSent.remove("centcom_meta");
	    	
            simulatedAgent1 = new SimulatedAgentActions();
            simulatedAgent1.register(customerId, simulatedAgentIp1, simulatedAgentName1, 
            		simulatedAgentMac1, simulatedAgentOs);
            
            simulatedAgent2 = new SimulatedAgentActions();
            simulatedAgent2.register(customerId, simulatedAgentIp2, simulatedAgentName2, 
            		simulatedAgentMac2, simulatedAgentOs);
            
            lennyActions.SetCustomerConfiguration(customerId, customerConf);
            lennyActions.setEndpointConfig(customerId, simulatedAgentName2, epConf);
            
            //agent1 expects to get the customer conf
            sendCheckUpdatesAndGetConfAndVerify(simulatedAgent1, simulatedAgentName1, customerConfigSent, CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName());
            //agent2 expects to get the standalone conf
            sendCheckUpdatesAndGetConfAndVerify(simulatedAgent2, simulatedAgentName2, epConfigSent, CheckUpdatesActions.CONFIGURATION_SWITCH.getActionName());
            
	       
            //Call revoke ep conf for agent 2
	        lennyActions.revokeEpConfiguration(customerId, simulatedAgentName2);
	        
	        //agent2 expects to get the customer conf now
	        sendCheckUpdatesAndGetConfAndVerify(simulatedAgent2, simulatedAgentName2, customerConfigSent, CheckUpdatesActions.CONFIGURATION_SWITCH.getActionName());
	        
	        
	        //Updates customer configuration again
	        customerConf = JsonUtil.ChangeTagConfiguration(customerConf, tag_to_update, secondCustomerLevelValue);
	        
	        //Prepare for verification
            customerConfigSent = new JSONObject(customerConf);
            customerConfigSent.remove("centcom_meta");
            
            //Set the new configuration for customer level
            lennyActions.SetCustomerConfiguration(customerId, customerConf);
            
            //agent1 expects to get the customer conf
            sendCheckUpdatesAndGetConfAndVerify(simulatedAgent1, simulatedAgentName1, customerConfigSent, CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName());
            //agent2 expects to get the customer conf
            sendCheckUpdatesAndGetConfAndVerify(simulatedAgent2, simulatedAgentName2, customerConfigSent, CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName());
	        
	        JLog.logger.info("StandaloneEPconfigurationVerify::verifyStandaloneEPconfiguration completed.");

        } catch (Exception e) {
            org.testng.Assert.fail("StandaloneEPconfigurationVerify::verifyStandaloneEPconfiguration", e);
        }
        
    }
    
    private void sendCheckUpdatesAndGetConfAndVerify(SimulatedAgentActions simAgent, String epName, JSONObject expectedConf, String expectedAction) {    	
    	
    	String action = simAgent.sendCheckUpdatesAndGetAction(epName, "1.1.1", 0, 0, "1.1.1", customerId);
        org.testng.Assert.assertEquals(action, expectedAction, "check update result failure, got unexpected action: ");
        
        String receivedConf = simAgent.getConf(customerId);
        JSONObject receivedConfObj = new JSONObject(receivedConf );
        JSONAssert.assertEquals("Agent configuration is not as expected. See differences at the following lines:\n ", expectedConf.toString(), receivedConfObj.toString(), JSONCompareMode.LENIENT);

    }

    String verifyPatternInConfig(BaseAgentActions agent, String pattern) {
        String result;
        String conf_path = agent.getConfigPath(true);
        result = agent.findInText(conf_path, pattern);
        return result;
    }

    @AfterMethod
    public void Close(){
        
    	if (simulatedAgent1 != null) {
    		lennyActions.deleteWithoutVerify(customerId, simulatedAgentName1);
    		
    		String action = simulatedAgent1.sendCheckUpdatesAndGetAction(simulatedAgentName1, "1.2.0.100", 0, 0, "1.1.1", customerId);
        	org.testng.Assert.assertEquals(action, CheckUpdatesActions.UNINSTALL.getActionName(), "check update result failure, got unexpected action: ");
           
    	}
        
    	if (simulatedAgent2 != null) {
    		lennyActions.deleteWithoutVerify(customerId, simulatedAgentName2);
    	
    		String action = simulatedAgent2.sendCheckUpdatesAndGetAction(simulatedAgentName2, "1.2.0.100", 0, 0, "1.1.1", customerId);
    		org.testng.Assert.assertEquals(action, CheckUpdatesActions.UNINSTALL.getActionName(), "check update result failure, got unexpected action: ");
    	}
        
        if(lennyActions!=null){
            lennyActions.Close();
        }
    }


}
