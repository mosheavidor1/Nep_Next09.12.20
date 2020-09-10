package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

//https://jira.trustwave.com/browse/NEP-1252
public class StandaloneEPconfigurationVerify extends GenericTest {

    private LNEActions lennyActions;
    static final String settings_toVerify_Alone = "\"check_update_period\":53";
    static final String settings_toVerify_Set = "\"check_update_period\":311";
    private String customerId;
    
    private static final String simulatedAgentIp1 = "1.2.3.4";
    private static final String simulatedAgentIp2 = "1.2.3.5";
    private static final String simulatedAgentOs = "Windows 10";
    private static final String simulatedAgentMac1 = "84-7B-EB-21-99-100";
    private static final String simulatedAgentMac2 = "84-7B-EB-21-99-101";
    private static final String simulatedAgentName1 = "ep1";
    private static final String simulatedAgentName2 = "ep2";
    
    
    
    @Factory(dataProvider = "getData")
    public StandaloneEPconfigurationVerify(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
    }

    @Test()
    public void verifyStandaloneEPconfiguration()  {
    	try {
            JLog.logger.info("Starting StandaloneEPconfigurationVerify::verifyStandaloneEPconfiguration ...");
	    	
            SimulatedAgentActions simulatedAgent1 = new SimulatedAgentActions(customerId, simulatedAgentIp1, simulatedAgentName1, 
            		simulatedAgentMac1, simulatedAgentOs);
            
            SimulatedAgentActions simulatedAgent2 = new SimulatedAgentActions(customerId, simulatedAgentIp2, simulatedAgentName2, 
            		simulatedAgentMac2, simulatedAgentOs);
            
          //  lennyActions.SetCustomerConfiguration(customerId, configuration);
           // lennyActions.setEndpointConfig(customerId, simulatedAgentName2, configuration);
	        
	        //In a real environment the following triggers the DS to send request for config upgrade to Centcom
	        String action = simulatedAgent1.checkUpdates(simulatedAgentName1, "1.1.1", 0, 0, "1.1.1");
	        org.testng.Assert.assertEquals(action, "config update", String.format("check update result assertion failure. Expected: 'config update', got '%s' ", action));
            
	        action = simulatedAgent2.checkUpdates(simulatedAgentName1, "1.1.1", 0, 0, "1.1.1");
	        org.testng.Assert.assertEquals(action, "config update", String.format("check update result assertion failure. Expected: 'config update', got '%s' ", action));
            
	        
	      //TODO delete the endpoints
	        
	        JLog.logger.info("SchemaVersionsAndUpgrade::testCustomerEndpoint completed.");

        } catch (Exception e) {
            org.testng.Assert.fail("StandaloneEPconfigurationVerify::verifyStandaloneEPconfiguration", e);
        }
        
    }

    String verifyPatternInConfig(BaseAgentActions agent, String pattern) {
        String result;
        String conf_path = agent.getConfigPath(true);
        result = agent.findInText(conf_path, pattern);
        return result;
    }

    @AfterMethod
    public void Close(){
        
        if(lennyActions!=null){
            lennyActions.Close();
        }
    }


}
