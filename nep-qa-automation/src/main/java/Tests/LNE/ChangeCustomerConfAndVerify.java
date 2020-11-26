package Tests.LNE;

import Actions.CheckUpdatesActions;
import Actions.DsMgmtActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.ConfigHandling;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;

import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class ChangeCustomerConfAndVerify extends GenericTest {

    private String customerId;
    
    private static String confJson;
    
    private SimulatedAgentActions simulatedAgent;
    

    @Factory(dataProvider = "getData")
    public ChangeCustomerConfAndVerify(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }

    @Test(groups = { "ChangeCustomerConfAndVerify" }, priority = 20 )//so that it will run after the send logs test, im portal env
    public void changeCustomerConfAndVerify()  {

        JLog.logger.info("Starting ChangeCustomerConfAndVerify...");
       
    	confJson = ConfigHandling.getConfiguration("Changed Configuration");
        DsMgmtActions.SetCustomerConfiguration(customerId, confJson);
	        
        //Verify that simulated agent gets this configuration

        simulatedAgent = new SimulatedAgentActions(getGeneralData().get("DS Name"), customerId);
        simulatedAgent.register(customerId, "1.2.3.4", "epForTest", "84-7B-EB-21","Windows 10");
        String action = simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgent.getName(),GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
        
        org.testng.Assert.assertTrue(action.contains(CheckUpdatesActions.CONFIGURATION_UPDATE.getActionName()), "ChangeCustomerConfAndVerify: simulated agent was expected to get conf change, but got a different action: " + action);
        

        String simulatedAgentConf = simulatedAgent.getConf(customerId);
        
        JSONObject configSent = new JSONObject(confJson);
        configSent.remove("centcom_meta");
        JSONObject configReceived = new JSONObject(simulatedAgentConf );
        JSONAssert.assertEquals("Agent configuration is not as expected. See differences at the following lines:\n ", configSent.toString(), configReceived.toString(), JSONCompareMode.LENIENT);
        
        JLog.logger.info("ChangeCustomerConfAndVerify finished successfully.");

    }

    @AfterMethod
    public void Close(){
        DsMgmtActions.deleteWithoutVerify(customerId, "epForTest");
        simulatedAgent.sendCheckUpdatesAndGetResponse(simulatedAgent.getName(), GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
        
    }

}
