package Tests.LNE;

import Actions.BaseAgentActions;
import Actions.CheckUpdatesActions;
import Actions.DsMgmtActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.ConfigHandling;
import Utils.JsonUtil;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;

import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

//https://jira.trustwave.com/browse/NEP-1252
public class StandaloneEPconfigurationVerify extends GenericTest {

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
    private static final String configIdentifier = "Changed Configuration";
    
    SimulatedAgentActions simulatedAgent1 = null;
    SimulatedAgentActions simulatedAgent2 = null;
    
    
    @Factory(dataProvider = "getData")
    public StandaloneEPconfigurationVerify(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }

    @Test(groups = { "StandaloneEPconfiguration" },priority = 20 )//so that it will run after the send logs test, im portal env
    public void verifyStandaloneEPconfiguration()  {
    	try {
            JLog.logger.info("Starting StandaloneEPconfigurationVerify::verifyStandaloneEPconfiguration ...");
            
            String customerConf = ConfigHandling.getConfiguration(configIdentifier);
            customerConf = JsonUtil.ChangeTagConfiguration(customerConf, tag_to_update, customerLevelValue);
            
            //Prepare for verification
            JSONObject customerConfigSent = new JSONObject(customerConf);
            customerConfigSent.remove("centcom_meta");
            
            String epConf = ConfigHandling.getConfiguration(configIdentifier);
            epConf = JsonUtil.ChangeTagConfiguration(epConf, tag_to_update, endpointLevelValue);
            
            //Prepare for verification
            JSONObject epConfigSent = new JSONObject(epConf);
            epConfigSent.remove("centcom_meta");
	    	
            simulatedAgent1 = new SimulatedAgentActions(getGeneralData().get("DS Name"), customerId);
            simulatedAgent1.register(customerId, simulatedAgentIp1, simulatedAgentName1, 
            		simulatedAgentMac1, simulatedAgentOs);
            
            simulatedAgent2 = new SimulatedAgentActions(getGeneralData().get("DS Name"), customerId);
            simulatedAgent2.register(customerId, simulatedAgentIp2, simulatedAgentName2, 
            		simulatedAgentMac2, simulatedAgentOs);
            
            DsMgmtActions.SetCustomerConfiguration(customerId, customerConf);
            DsMgmtActions.setEndpointConfig(customerId, simulatedAgentName2, epConf);
            
            //agent1 expects to get the customer conf
            sendCheckUpdatesAndGetConfAndVerify(simulatedAgent1, simulatedAgentName1, customerConfigSent, CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName());
            //agent2 expects to get the standalone conf
            sendCheckUpdatesAndGetConfAndVerify(simulatedAgent2, simulatedAgentName2, epConfigSent, CheckUpdatesActions.CONFIGURATION_SWITCH.getActionName());
            
	       
            //Call revoke ep conf for agent 2
            DsMgmtActions.revokeEpConfiguration(customerId, simulatedAgentName2);
	        
	        //agent2 expects to get the customer conf now
	        sendCheckUpdatesAndGetConfAndVerify(simulatedAgent2, simulatedAgentName2, customerConfigSent, CheckUpdatesActions.CONFIGURATION_SWITCH.getActionName());
	        
	        
	        //Updates customer configuration again
	        customerConf = JsonUtil.ChangeTagConfiguration(customerConf, tag_to_update, secondCustomerLevelValue);
	        
	        //Prepare for verification
            customerConfigSent = new JSONObject(customerConf);
            customerConfigSent.remove("centcom_meta");
            
            //Set the new configuration for customer level
            DsMgmtActions.SetCustomerConfiguration(customerId, customerConf);
            
            //agent1 expects to get the customer conf
            sendCheckUpdatesAndGetConfAndVerify(simulatedAgent1, simulatedAgentName1, customerConfigSent, CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName());
            //agent2 expects to get the customer conf
            sendCheckUpdatesAndGetConfAndVerify(simulatedAgent2, simulatedAgentName2, customerConfigSent, CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName());
	        
	        JLog.logger.info("Finished StandaloneEPconfigurationVerify::verifyStandaloneEPconfiguration successfully.");

        } catch (Exception e) {
            org.testng.Assert.fail("StandaloneEPconfigurationVerify::verifyStandaloneEPconfiguration", e);
        }
        
    }
    
    private void sendCheckUpdatesAndGetConfAndVerify(SimulatedAgentActions simAgent, String epName, JSONObject expectedConf, String expectedAction) {    	
    	
    	String action = simAgent.sendCheckUpdatesAndGetAction(epName, GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
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
    		DsMgmtActions.deleteWithoutVerify(customerId, simulatedAgentName1);
    		
    		String action = simulatedAgent1.sendCheckUpdatesAndGetAction(simulatedAgentName1, GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
        	org.testng.Assert.assertEquals(action, CheckUpdatesActions.UNINSTALL.getActionName(), "check update result failure, got unexpected action: ");
           
    	}
        
    	if (simulatedAgent2 != null) {
    		DsMgmtActions.deleteWithoutVerify(customerId, simulatedAgentName2);
    	
    		String action = simulatedAgent2.sendCheckUpdatesAndGetAction(simulatedAgentName2, GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
    		org.testng.Assert.assertEquals(action, CheckUpdatesActions.UNINSTALL.getActionName(), "check update result failure, got unexpected action: ");
    	}
        
    }


}
