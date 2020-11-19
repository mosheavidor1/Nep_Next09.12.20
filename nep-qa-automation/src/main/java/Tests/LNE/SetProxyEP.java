package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.DsMgmtActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.ConfigHandling;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


//https://jira.trustwave.com/browse/NEP-1214
public class SetProxyEP extends GenericTest {

    private BaseAgentActions agent;

    private static String customerId;
    private static int agentInstallTimeout;
    private static int checkUPdatesInterval;
  
    @Factory(dataProvider = "getData")
    public SetProxyEP(Object dataToSet) {
        super(dataToSet);
    }
    
    @BeforeTest
    public void init() {
    	customerId = getGeneralData().get("Customer Id");
        agentInstallTimeout = Integer.parseInt(getGeneralData().get("EP Installation timeout"));
        checkUPdatesInterval = Integer.parseInt(getGeneralData().get("Check Updates Timeout")) * 1000; //35 seconds
        agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
	   
    }

    private String getProxyIP() {
        if (GlobalTools.isLennyEnv())
            return GlobalTools.getClusterToTest();
        else
            return null;
    }

    private String getProxyPort() {
        return "3128";
    }

    @Test(groups = { "SetProxyEP" }, priority=61  )
    public void SetProxyEP()  {
    	
    	try {
    	    String proxy_host = getProxyIP();
    	    String proxy_port =  getProxyPort();

            JLog.logger.info("Setting up proxy host {}, port {}", proxy_host,proxy_port);
            String set_proxy = agent.enableProxy(proxy_host,proxy_port);
            Thread.sleep(1000);
            String settings_json = agent.getSettingsPath();
            String res = agent.findInText(settings_json, proxy_host);
            org.testng.Assert.assertTrue(null != res , "Failed to find " + proxy_host + ", response is null.");
            res = agent.findInText(settings_json, proxy_port);
            org.testng.Assert.assertTrue(null != res , "Failed to find " + proxy_port + ", response is null.");

 //           agent.disableProxy();
	        JLog.logger.info("Completed test successfully\n");

    	} catch (Exception e) {
    		JLog.logger.error("Test failed", e);
            org.testng.Assert.fail("Test failed " + e.toString());
    	}
    }



    @AfterMethod
    public void Close(){
        if (agent!=null) {
            agent.close();
        }
    }
    

}
