package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.CheckUpdatesActions;
import Actions.LNEActions;
import Actions.SimulatedAgentActions;
import Actions.LNEActions.CentcomMethods;
import Tests.GenericTest;
import Utils.JsonUtil;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;



public class SchemaVersionsAndUpgrade extends GenericTest {

	private String customerId;
    private LNEActions lennyActions;
    //private BaseAgentActions agent1, agent2;
    
    private static final String schema_tag = "schema_version";
    private static final String schemaVersionBaseValue = "1.1.1";
    private static final String schemaVersionNewValue = "1.1.2";
    
    private static final String simulatedAgentIp = "1.2.3.4";
    private static final String simulatedAgentOs = "Windows 10";
    private static final String simulatedAgentMac = "84-7B-EB-21-96";
    private static final String simulatedAgentName = "ep1";
    
    SimulatedAgentActions simulatedAgent;
    
    @Factory(dataProvider = "getData")
    public SchemaVersionsAndUpgrade(Object dataToSet) {
        super(dataToSet);
    }
    
    @BeforeTest
    public void init() {
    	
    	lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
    	customerId = "1010";
    	
    	simulatedAgent = new SimulatedAgentActions();
        simulatedAgent.register(customerId, simulatedAgentIp, simulatedAgentName, 
         		simulatedAgentMac, simulatedAgentOs);
         
        //TODO verify when this method is run - we want it one before all tests in this class +
        //TODO delete the endpoints once at the end of this class tests

    }

    @Test
    public void testCustomerEndpoint()  {

        try {
            JLog.logger.info("Starting SchemaVersionsAndUpgrade::testCustomerEndpoint ...");
            
            String confJson = data.get("Settings Json");
            confJson = JsonUtil.ChangeTagConfiguration(confJson, schema_tag, schemaVersionBaseValue);
            
            lennyActions.InitCustomerSettings(customerId, confJson);
            
            
            //In a real environment the following triggers the DS to send request for config upgrade to Centcom
            String action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, schemaVersionNewValue, customerId);
            org.testng.Assert.assertEquals(action, CheckUpdatesActions.NO_UPDATE.getActionName(), String.format("check update result assertion failure. Expected: 'no update', got '%s' ", action));
            lennyActions.verifyCallToCentcom(CentcomMethods.REQUEST_UPGRADE, customerId, simulatedAgentName);

            //In a real environment, the Centcom activates the following set config request, here we activate it instead
            confJson = JsonUtil.ChangeTagConfiguration(confJson, schema_tag, schemaVersionNewValue);            
            lennyActions.setCustomerConfig(customerId, confJson);
            
            JSONObject configSent = new JSONObject(confJson);
            configSent.remove("centcom_meta");
            
            action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, schemaVersionNewValue, customerId);
            org.testng.Assert.assertEquals(action, CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName(), String.format("check update result assertion failure. Expected: 'config update', got '%s' ", action));
            String newConf = simulatedAgent.getConf(customerId);
            JSONObject configReceived = new JSONObject(newConf );
            JSONAssert.assertEquals("Agent configuration is not as expected. See differences at the following lines:\n ", configSent.toString(), configReceived.toString(), JSONCompareMode.LENIENT);

            //TODO delete the endpoints
            
            JLog.logger.info("SchemaVersionsAndUpgrade::testCustomerEndpoint completed.");

        } catch (Exception e) {
            org.testng.Assert.fail("SchemaVersionsAndUpgrade::testCustomerEndpoint failed " + "\n" + e.toString());
        }
    }
    
    @Test
    public void testClusterEndpoint()  {
    	/*
    	 try {
             JLog.logger.info("Starting SchemaVersionsAndUpgrade::testClusterEndpoint ...");
             
             String confJson = data.get("Settings Json");
             confJson = JsonUtil.ChangeTagConfiguration(confJson, schema_tag, schemaVersionBaseValue);
             
             lennyActions.InitCustomerSettings(customerId, confJson);
             
             //TODO define cluster and add the simulated endpoint to it
             
             //In a real environment the following triggers the DS to send request for config upgrade to Centcom
             String action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, schemaVersionNewValue);            
             org.testng.Assert.assertEquals(action, CheckUpdatesActions.NO_UPDATE.getActionName(), String.format("check update result assertion failure. Expected: 'no update', got '%s' ", action));
             lennyActions.verifyCallToCentcom(CentcomMethods.REQUEST_UPGRADE, customerId, simulatedAgentName);

             //In a real environment, the Centcom activates the following set config request, here we activate it instead
             confJson = JsonUtil.ChangeTagConfiguration(confJson, schema_tag, schemaVersionNewValue);            
             lennyActions.setClusterConfig(customerId, confJson);
             
             JSONObject configSent = new JSONObject(confJson);
             configSent.remove("centcom_meta");
             
             action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, schemaVersionNewValue);
             org.testng.Assert.assertEquals(action, CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName(), String.format("check update result assertion failure. Expected: 'config update', got '%s' ", action));
             String newConf = simulatedAgent.getConf();            
             JSONObject configReceived = new JSONObject(newConf );
             JSONAssert.assertEquals("Agent configuration is not as expected. See differences at the following lines:\n ", configSent.toString(), configReceived.toString(), JSONCompareMode.LENIENT);

             //TODO remove the EP from the cluster
             
             String action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, schemaVersionNewValue);            
             org.testng.Assert.assertEquals(action, CheckUpdatesActions.NO_UPDATE.getActionName(), String.format("check update result assertion failure. Expected: 'no update', got '%s' ", action));
             lennyActions.verifyCallToCentcom(CentcomMethods.REQUEST_UPGRADE, customerId, simulatedAgentName);


             JLog.logger.info("SchemaVersionsAndUpgrade::testClusterEndpoint completed.");

         } catch (Exception e) {
             org.testng.Assert.fail("SchemaVersionsAndUpgrade::testClusterEndpoint failed " + "\n" + e.toString());
         }*/
    }
    
    @Test
    public void testStandaloneEndpoint()  {
    	
    	
    	 try {
             JLog.logger.info("Starting SchemaVersionsAndUpgrade::testStandaloneEndpoint ...");

             String confJson = data.get("Settings Json");
             //TODO: replace customer id and schema version 1.1.1 in the json
             lennyActions.InitCustomerSettings(customerId, confJson);
             
             SimulatedAgentActions simulatedAgent = new SimulatedAgentActions();
             simulatedAgent.register(customerId, simulatedAgentIp, simulatedAgentName, 
             		simulatedAgentMac, simulatedAgentOs);
             
             //TODO: et a standalone config for it, schema ver = 1.1.1
             
             //In a real environment the following triggers the DS to send request for config upgrade to Centcom
             String action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, "1.1.2", customerId);
             org.testng.Assert.assertEquals(action, "no update", String.format("check update result assertion failure. Expected: 'no update', got '%s' ", action));
             lennyActions.verifyCallToCentcom(CentcomMethods.REQUEST_UPGRADE, String.valueOf(customerId), simulatedAgentName);

             
             //TODO: Set the standalone config, schema ver = X.X.X+1
            // lennyActions.setc
             
             action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, "1.1.2", customerId);
             org.testng.Assert.assertEquals(action, "config update", String.format("check update result assertion failure. Expected: 'config update', got '%s' ", action));
             
             //TODO: Send a get-conf-update request with the simEP to verify the new configuration 
             
          //   lennyActions.revokeEpConfiguration(configJson);
             
             //TODO String action = simulatedAgent.checkUpdates(simulatedAgentName, "1.1.1", 3, 0, "1.1.2");
             //org.testng.Assert.assertEquals(action, "no update", String.format("check update result assertion failure. Expected: 'no update', got '%s' ", action));
             
             //TODO: Schema upgrade request (for standalone level config) from DS to CC triggered (verify in logs)

             JLog.logger.info("SchemaVersionsAndUpgrade::testStandaloneEndpoint completed.");

         } catch (Exception e) {
             org.testng.Assert.fail("SchemaVersionsAndUpgrade::testStandaloneEndpoint failed " + "\n" + e.toString());
         }
    	 
         
    }

    @AfterTest
    public void Close(){
        if(lennyActions!=null){
            lennyActions.Close();
        }
        
    }


}
