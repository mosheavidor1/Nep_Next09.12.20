package Tests.LNE;

import Actions.CheckUpdatesActions;
import Actions.LNEActions;
import Actions.SimulatedAgentActions;
import Actions.LNEActions.CentcomMethods;
import Tests.GenericTest;
import Utils.JsonUtil;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;



public class SchemaVersionsAndUpgrade extends GenericTest {

	private String customerId = "1010";
    private LNEActions lennyActions;
    SimulatedAgentActions simulatedAgent;
    
    private static final String schema_tag = "schema_version";
    private static final String schemaVersionBaseValue = "1.1.1";
    private static final String schemaVersionNewValue = "1.1.2";
    
    private static final String simulatedAgentIp = "1.2.3.4";
    private static final String simulatedAgentOs = "Windows 10";
    private static final String simulatedAgentMac = "84-7B-EB-21-96";
    private static final String simulatedAgentName = "ep1";
    
    private String confJson;
    
    private static final String clusterName = "ClusterForSchemaUpgradeTest";
    
    
    @Factory(dataProvider = "getData")
    public SchemaVersionsAndUpgrade(Object dataToSet) {
        super(dataToSet);
    }
    
    @BeforeTest
    public void init() {
    	lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
    	
    }
    
    @BeforeMethod
    public void beforeMethod() {
    	
    	
    	confJson = data.get("Settings Json");
    	confJson = JsonUtil.ChangeTagConfiguration(confJson, schema_tag, schemaVersionBaseValue);
    	lennyActions.InitCustomerSettings(customerId, confJson);
    	
    	simulatedAgent = new SimulatedAgentActions();
        simulatedAgent.register(customerId, simulatedAgentIp, simulatedAgentName, 
         		simulatedAgentMac, simulatedAgentOs);
         
    }

    @Test
    public void testCustomerEndpoint()  {

        try {
            JLog.logger.info("Starting SchemaVersionsAndUpgrade::testCustomerEndpoint ...");
            
                        
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
    	
    	 try {
             JLog.logger.info("Starting SchemaVersionsAndUpgrade::testClusterEndpoint ...");
             
             //Define cluster
             lennyActions.setClusterConfig(customerId, clusterName, confJson);

             //Add endpoint to cluster
             Map<String, List<String>> assignments = new HashMap<>();
             List<String> epsNames = new LinkedList<>();
             epsNames.add(simulatedAgent.getName());
             assignments.put(clusterName, epsNames);

             lennyActions.updateClusterMap(Long.valueOf(customerId), assignments);
             
             //In a real environment the following triggers the DS to send request for config upgrade to Centcom
             String action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, schemaVersionNewValue, customerId);            
             org.testng.Assert.assertEquals(action, CheckUpdatesActions.NO_UPDATE.getActionName(), String.format("check update result assertion failure. Expected: 'no update', got '%s' ", action));
             lennyActions.verifyCallToCentcom(CentcomMethods.REQUEST_UPGRADE, customerId, simulatedAgentName);

             //In a real environment, the Centcom activates the following set config request, here we activate it instead
             confJson = JsonUtil.ChangeTagConfiguration(confJson, schema_tag, schemaVersionNewValue);            
             lennyActions.setClusterConfig(customerId, clusterName, confJson);
             
             JSONObject configSent = new JSONObject(confJson);
             configSent.remove("centcom_meta");
             
             action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, schemaVersionNewValue, customerId);        
             org.testng.Assert.assertEquals(action, CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName(), String.format("check update result assertion failure. Expected: 'config update', got '%s' ", action));
             String newConf = simulatedAgent.getConf(customerId);       
             JSONObject configReceived = new JSONObject(newConf );
             JSONAssert.assertEquals("Agent configuration is not as expected. See differences at the following lines:\n ", configSent.toString(), configReceived.toString(), JSONCompareMode.LENIENT);

             //remove the EP from the cluster
             assignments.put(clusterName, new LinkedList<>());
             lennyActions.updateClusterMap(Long.valueOf(customerId), assignments);
             
             lennyActions.setCustomerConfig(customerId, confJson);
             
             action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, schemaVersionNewValue, customerId);
             org.testng.Assert.assertEquals(action, CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName(), String.format("check update result assertion failure. Expected: 'config update', got '%s' ", action));
             newConf = simulatedAgent.getConf(customerId);
             configReceived = new JSONObject(newConf );
             JSONAssert.assertEquals("Agent configuration is not as expected. See differences at the following lines:\n ", configSent.toString(), configReceived.toString(), JSONCompareMode.LENIENT);


             JLog.logger.info("SchemaVersionsAndUpgrade::testClusterEndpoint completed.");

         } catch (Exception e) {
             org.testng.Assert.fail("SchemaVersionsAndUpgrade::testClusterEndpoint failed " + "\n" + e.toString());
         }
    }
    
    @Test
    public void testStandaloneEndpoint()  {
    	
    	
    	 try {
             JLog.logger.info("Starting SchemaVersionsAndUpgrade::testStandaloneEndpoint ...");

             lennyActions.setEndpointConfig(customerId, simulatedAgent.getName(), confJson);           
             
             //In a real environment the following triggers the DS to send request for config upgrade to Centcom
             String action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, schemaVersionNewValue, customerId);
             org.testng.Assert.assertEquals(action, "no update", String.format("check update result assertion failure. Expected: 'no update', got '%s' ", action));
             lennyActions.verifyCallToCentcom(CentcomMethods.REQUEST_UPGRADE, String.valueOf(customerId), simulatedAgentName);

             
             //In a real environment, the Centcom activates the following set config request, here we activate it instead
             confJson = JsonUtil.ChangeTagConfiguration(confJson, schema_tag, schemaVersionNewValue);      
             lennyActions.setEndpointConfig(customerId, simulatedAgent.getName(), confJson);   
             
             JSONObject configSent = new JSONObject(confJson);
             configSent.remove("centcom_meta");
             
             action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, schemaVersionNewValue, customerId);
             org.testng.Assert.assertEquals(action, "config update", String.format("check update result assertion failure. Expected: 'config update', got '%s' ", action));
             String newConf = simulatedAgent.getConf(customerId);
             JSONObject configReceived = new JSONObject(newConf );
             JSONAssert.assertEquals("Agent configuration is not as expected. See differences at the following lines:\n ", configSent.toString(), configReceived.toString(), JSONCompareMode.LENIENT);

             
             lennyActions.revokeEpConfiguration(customerId, simulatedAgent.getName());             
             lennyActions.setCustomerConfig(customerId, confJson);
             
             action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, "1.1.1", 3, 0, schemaVersionNewValue, customerId);
             org.testng.Assert.assertEquals(action, CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName(), String.format("check update result assertion failure. Expected: 'config update', got '%s' ", action));
             newConf = simulatedAgent.getConf(customerId);
             configReceived = new JSONObject(newConf );
             JSONAssert.assertEquals("Agent configuration is not as expected. See differences at the following lines:\n ", configSent.toString(), configReceived.toString(), JSONCompareMode.LENIENT);

             JLog.logger.info("SchemaVersionsAndUpgrade::testStandaloneEndpoint completed.");

         } catch (Exception e) {
             org.testng.Assert.fail("SchemaVersionsAndUpgrade::testStandaloneEndpoint failed " + "\n" + e.toString());
         }
    	 
         
    }
    
    @AfterMethod
    public void cleanup(){
    	
    	lennyActions.deleteWithoutVerify(customerId, simulatedAgent.getName());
    	simulatedAgent.sendCheckUpdatesWithoutVerify(simulatedAgent.getName(), "1.2.0.100", 0, 0, schemaVersionNewValue, customerId);
        
    }

    @AfterTest
    public void Close(){
    	    	
        if(lennyActions!=null){
            lennyActions.Close();
        }
        
    }


}
