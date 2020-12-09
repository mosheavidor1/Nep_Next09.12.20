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
public class SimulateLLMandVerify extends GenericTest {

    private BaseAgentActions agent;
    private static final LNEActions lennyActions = GlobalTools.getLneActions();
    public static final String scp_path = "/work/services/siem/var/siem/data/nep/";
    public static final String syslog_path = "/work/services/siem/var/log/";
    static final String command_linuxSIEM = "cat /opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log | grep -e \".zip was sent successfully\"";
    static final String LLM_Syslog_path = "/opt/tw-endpoint/data/llm/monitor_dir/syslogs";
    static final String EP_Syslog_pattern = "LLM Test message #";
    static final String EP_LCA_SYSLOG_log_pattern = "Sent %s events.";
    static final String command_linuxLCA = "cat /opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log | grep -e \".txt was sent successfully\"";
    static final String command_linuxLCA_SYSLOG = "cat /opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log | grep -e \"Sent %s events.\"";
    String expectedNumberOfMessages;
    String lcaSyslogOnLenny;
    private static String customerId;
    private static int agentInstallTimeout;
    private static int verifyLogsTimeout = 120;//120 seconds
    private static int checkUPdatesInterval;
    private static int testResultTimeout;
    private static boolean is_using_proxy = false;
    private static boolean is_proxy_valid = false;
    private static String  proxy_IP;
    private static String proxy_Port;

    @Factory(dataProvider = "getData")
    public SimulateLLMandVerify(Object dataToSet) {
        super(dataToSet);
    }
    
    @BeforeTest
    public void init() {
    	customerId = getGeneralData().get("Customer Id");
        agentInstallTimeout = Integer.parseInt(getGeneralData().get("EP Installation timeout"));
        checkUPdatesInterval = Integer.parseInt(getGeneralData().get("Check Updates Timeout")) * 1000; //35 seconds
    }
    private void initProxy(String proxy_ip) {
        JLog.logger.debug("Starting initProxy of SimulateLLMandVerify test...");


        if (proxy_ip.equalsIgnoreCase("LNE"))
            proxy_IP = GlobalTools.getClusterToTest();
        else
            proxy_IP = proxy_ip;

        proxy_Port = data.get("Proxy_Port");
        String v = data.get("Proxy_Valid");
        is_proxy_valid = v.equalsIgnoreCase("yes");
    }


    @Test(groups = { "SimulateLLMandVerify" }, priority=61  )
    public void SimulateLLMandVerifyDelivery()  {
    	
    	try {

            JLog.logger.debug("Starting SimulateLLMandVerify test...");

            if (!data.get("EP_Type_1").equals("lnx")) {
    	    	JLog.logger.info("This test should not run for {} OS, skipping", data.get("EP_Type_1"));
    	    	return;
    	    }
		    String log_type = data.get("Log_Type");
  	        expectedNumberOfMessages = data.get("ExpectedResult");
            testResultTimeout = Integer.parseInt(data.get("Result_timeout")) * 1000; // seconds
            String proxy_ip = data.get("Proxy_IP");
            is_using_proxy = !proxy_ip.isEmpty();
            if (is_using_proxy) {
                initProxy(proxy_ip);
            }
  	        JLog.logger.info("Starting SimulateLLMandVerifyDelivery. Log type: {}.  Expected number of messages in log: {}.", log_type, expectedNumberOfMessages);
            JLog.logger.info("Using proxy: {}",is_using_proxy);
            if (is_using_proxy)
                JLog.logger.info("Proxy IP: {}; Proxy Port: {}, Proxy is Valid: {}", proxy_IP, proxy_Port, is_proxy_valid);
  	        JLog.logger.info("Going to set configuration with LLM disabled, and waits until agent updates");
  		    DsMgmtActions.SetCustomerConfiguration(customerId, ConfigHandling.getConfiguration("LLM LFM Disabled"));
  		    Thread.sleep(checkUPdatesInterval); //Waits until EP will get the new configuration
  	        
  	        //Read configuration and update the host tags
            String confJsonName = data.get("Configuration Name");
		    String confJson = ConfigHandling.getConfiguration(confJsonName);	  	    
            confJson = confJson.replaceAll(ConfigHandling.lennyIpPlaceholder, GlobalTools.getClusterToTest()); 	        

  	        lcaSyslogOnLenny = syslog_path + data.get("EP_HostName_1") + "/local0.log";
		    agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
            if (is_using_proxy) {
                agent.enableProxy(proxy_IP, proxy_Port);
                Thread.sleep(5000);
            }
		    JLog.logger.info("Going to stop the agent service, clean agent logs/files, set the needed configuration for the customer and finally start the agent service.");		    
		    agent.stopEPService(agentInstallTimeout);
		    
		    //Set the conf here, so that the endpoint will get it right upon starting
            DsMgmtActions.SetCustomerConfiguration(customerId, confJson);		    
		    //TODO: compare json cofniguration on agent
		
		    agent.clearFile(agent.getAgentLogPath()); 
            agent.clearFile(LLM_Syslog_path);  
            agent.clearFile(agent.getDbJsonPath());
            
            if (log_type.equalsIgnoreCase("LCA_SYSLOG")) {
            	lennyActions.clearFile(lcaSyslogOnLenny);
            }
                       
            agent.startEPService(agentInstallTimeout);
            
            JLog.logger.info("Going to sleep {} seconds before generating LLM input", testResultTimeout/1000);
            Thread.sleep(testResultTimeout);//seconds
		    createLLM_input();
		    JLog.logger.info("Going to sleep 1 minute until events/files are collected");
		    Thread.sleep(60000);//60 seconds
		    verifySyslogInAgent(LLM_Syslog_path, EP_Syslog_pattern);
		    
	        if (log_type.equalsIgnoreCase( "SIEM")) {
	            verifyLogsSentToSiem(command_linuxSIEM);
	        } else if (log_type.equalsIgnoreCase("LCA")) {
	            verifyLogsSentToLca(command_linuxLCA);
	        } else if (log_type.equalsIgnoreCase("LCA_SYSLOG")) {
	            String command = String.format(command_linuxLCA_SYSLOG, data.get("ExpectedResultRE"));//Expected result in Regular expression, tolerant to 30/31
	            verifyLogsSentToLcaSyslog(command);
	        }else {
	            org.testng.Assert.fail("Unknown server log_type: " +  log_type);
	        }
	        
	        JLog.logger.info("Completed test successfully\n");

    	} catch (Exception e) {
    		JLog.logger.error("Test failed", e);
            org.testng.Assert.fail("Test failed " + e.toString());
    	}
    }

     private void createLLM_input() {
    	 JLog.logger.info("Going to run script to create LLM input");
         String script;
         script = data.get("LLMScript");
         agent.writeAndExecute(script, "llmTestCreateInput");
         JLog.logger.info("done");
     }

    private void verifyLogsSentToSiem(String command) {
    	JLog.logger.info("Going to verify logs sent to SIEM");

    	String res = agent.verifyExpectedOnCommandResult(command, ".zip was sent successfully", verifyLogsTimeout);        
    	 org.testng.Assert.assertTrue(null != res, "Failed to find expected messages in log");
                
        int start = res.lastIndexOf("dla_");
        int stop = res.lastIndexOf(".zip");
        org.testng.Assert.assertTrue(start != -1 && stop != -1, "Failed to find zip file name in result");
        
        String zipFileName = res.substring(start, stop + 4);
        JLog.logger.info("Going to verify number of lines in {}", scp_path + zipFileName);
       
        res = lennyActions.numLinesinFile(scp_path + zipFileName, EP_Syslog_pattern);
        org.testng.Assert.assertTrue(null != res , "Failed, response is null.");
        org.testng.Assert.assertTrue(res.contains(expectedNumberOfMessages), "Failed: number of messages is: " + res + ". Expected is: " + expectedNumberOfMessages);        
        JLog.logger.info("done");
    }

    private void verifyLogsSentToLca(String command) {
    	JLog.logger.info("Going to verify logs sent to LCA");

        String res = agent.verifyExpectedOnCommandResult(command, ".txt was sent successfully", verifyLogsTimeout);
        org.testng.Assert.assertTrue(null != res, "Failed to find expected messages in log");
        
        int start = res.lastIndexOf("dla_");
        int stop = res.lastIndexOf(".txt");
        org.testng.Assert.assertTrue(start != -1 && stop != -1, "Failed to find txt file name in result");
        
        String txtFileName = res.substring(start, stop + 4);
        JLog.logger.info("Going to verify number of lines in {}", scp_path + txtFileName);
        
        res = lennyActions.numLinesinFile(scp_path + txtFileName, EP_Syslog_pattern);
        org.testng.Assert.assertTrue(null != res , "Failed, response is null.");
        org.testng.Assert.assertTrue(res.contains(expectedNumberOfMessages), "Failed: number of messages is: " + res + ". Expected is: " + expectedNumberOfMessages);  
        JLog.logger.info("done");
    }

    private void verifyLogsSentToLcaSyslog(String command) {
    	JLog.logger.info("Going to verify logs sent to LCA SYSLOG according to logs on agent");
        String res = agent.verifyExpectedOnCommandResult(command, null, verifyLogsTimeout);
        org.testng.Assert.assertTrue(null != res, "Failed to find expected messages in log");
        
        // now check on LNE       
        JLog.logger.info("Result: {}. Going to verify logs received in Lenny according to logs there", res);
        res = lennyActions.numLinesinFile(lcaSyslogOnLenny, EP_Syslog_pattern);        
        org.testng.Assert.assertTrue(null != res , "Failed, response is null.");
        org.testng.Assert.assertTrue((res.contains(expectedNumberOfMessages) || res.contains(expectedNumberOfMessages + 1)), "Failed: number of messages is: " + res + ". Expected is: " + expectedNumberOfMessages + " or + 1");          
        JLog.logger.info("done");
    }
    

    private void verifySyslogInAgent(String filename, String pattern) {
        JLog.logger.info("Going to check that expected string '{}' exist in '{}' ", pattern, filename);
    	
        String res = agent.findInText(filename, pattern);
        org.testng.Assert.assertTrue(res!= null, "Response is null!");
        JLog.logger.info("Response is: '{}'. \nChecking occurences of '{}' and expect '{}'", res, pattern, expectedNumberOfMessages);
        int num_of_patterns = res.split(pattern,-1).length - 1;
        org.testng.Assert.assertEquals(Integer.parseInt(expectedNumberOfMessages),num_of_patterns);
        JLog.logger.info("done");
    }

    @AfterMethod
    public void Close(){
        if (agent!=null) {
            if (is_using_proxy) {
                agent.disableProxy();
            }
            agent.close();
        }
    }
    
    /*
    @AfterTest
    public void clean(){
    	if (destLogFiles != null) {
    		for (int i = 0; i < destLogFiles.size(); i++) {
    			//TODO: delete files from destination
    		}
    	}
    }*/


}
