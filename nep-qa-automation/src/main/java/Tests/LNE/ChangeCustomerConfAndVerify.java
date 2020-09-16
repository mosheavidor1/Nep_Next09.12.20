package Tests.LNE;

import Actions.CheckUpdatesActions;
import Actions.LNEActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class ChangeCustomerConfAndVerify extends GenericTest {

    private LNEActions lennyActions;
    private String customerId;
    
    private static String confJson;
    
    private SimulatedAgentActions simulatedAgent;
    
    private static boolean confWasSet = false;

    @Factory(dataProvider = "getData")
    public ChangeCustomerConfAndVerify(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
        lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
    }

    @Test(groups = { "ChangeCustomerConfAndVerify" } )
    public void changeCustomerConfAndVerify()  {

        JLog.logger.info("Starting ChangeCustomerConfAndVerify...");
        
        if (!confWasSet) { //Configuration will be set only once, and not for every EP
	        
	        confJson = data.get("Settings Json");
	        lennyActions.SetCustomerConfiguration(customerId, confJson);
	        
	        confWasSet = true;
        }
        //Verify that simulated agent gets this configuration

        simulatedAgent = new SimulatedAgentActions();
        simulatedAgent.register(customerId, "1.2.3.4", "epForTest", "84-7B-EB-21","Windows 10");
        String action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgent.getName(),"1.2.0.100", 0, 0, "1.1.1", customerId);
        
        org.testng.Assert.assertTrue(action.contains(CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName()), "ChangeCustomerConfAndVerify: simulated agent was expected to get conf change, but got a different action: " + action);
        

        String simulatedAgentConf = simulatedAgent.getConf(customerId);
        
        JSONObject configSent = new JSONObject(confJson);
        configSent.remove("centcom_meta");
        JSONObject configReceived = new JSONObject(simulatedAgentConf );
        JSONAssert.assertEquals("Agent configuration is not as expected. See differences at the following lines:\n ", configSent.toString(), configReceived.toString(), JSONCompareMode.LENIENT);

    }

    @AfterMethod
    public void Close(){
        if(lennyActions!=null){
            lennyActions.Close();
        }
        lennyActions.deleteWithoutVerify(customerId, "epForTest");
        simulatedAgent.sendCheckUpdatesAndGetResponse(simulatedAgent.getName(), "1.2.0.100", 0, 0, "1.1.1", customerId);
    }

}
