package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


//https://jira.trustwave.com/browse/NEP-1214
public class SimulateLLMandVerify extends GenericTest {

    private BaseAgentActions agent;
    private LNEActions lennyActions;
    public static final String scp_path = "/work/services/siem/var/siem/data/nep/";
    public static final String syslog_path = "/work/services/siem/var/log/";
    static final String command_linuxSIEM = "cat /opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log | grep -e \".zip was sent successfully\"";
    static final String LLM_Syslog_path = "/opt/tw-endpoint/data/llm/monitor_dir/syslogs";
    static final String EP_Syslog_pattern = "LLM Test message #";
    static final String EP_LCA_SYSLOG_log_pattern = "Sent %s events.";
    static final String command_linuxLCA = "cat /opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log | grep -e \".txt was sent successfully\"";
    static final String command_linuxLCA_SYSLOG = "cat /opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log | grep -e \"Sent %s events.\"";
    private static final int schedule_report_timeout = 120000; //120 seconds
    private static int checkUPdatesInterval;
    String expectedNumberOfMessages;
    String lcaSyslogOnLenny;
    private static String customerId;
    
    private static final String host_value_to_update = "\\{lenny-ip\\}";
    
    @Factory(dataProvider = "getData")
    public SimulateLLMandVerify(Object dataToSet) {
        super(dataToSet);
    }
    
    @BeforeTest
    public void init() {
    	customerId = general.get("Customer Id");
        checkUPdatesInterval = Integer.parseInt(general.get("Check Updates Timeout")) * 1000; //35 seconds
    }

    @Test(groups = { "SimulateLLMandVerify" } )
    public void SimulateLLMandVerifyDelivery()  {
    	
    	try {
    		if (!data.get("EP_Type_1").equals("lnx")) {
    	    	JLog.logger.info("This test should not run for {} OS, skipping", data.get("EP_Type_1"));
    	    	return;
    	    }
		    String log_type = data.get("Log_Type");
  	        expectedNumberOfMessages = data.get("ExpectedResult");
  	        
  	        JLog.logger.info("Starting SimulateLLMandVerifyDelivery. Log type: {}.  Expected number of messages in log: {}.", log_type, expectedNumberOfMessages);

  	        lcaSyslogOnLenny = syslog_path + data.get("EP_HostName_1") + "/local0.log";
		    agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
		    lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
		
		    agent.clearFile(agent.getAgentLogPath()); 
            agent.clearFile(LLM_Syslog_path);  
            agent.clearFile(agent.getDbJsonPath());
            
		    //Read configuration and update the host tags
		    String confJson = data.get("Settings Json");		    
            confJson = confJson.replaceAll(host_value_to_update, PropertiesFile.readProperty("ClusterToTest"));
            		
		    lennyActions.SetCustomerConfiguration(customerId, confJson);
		    Thread.sleep(checkUPdatesInterval); //Waits until EP will get the new configuration
		    //TODO: compare json cofniguration on agent
	         
            if (log_type.equalsIgnoreCase("LCA_SYSLOG")) {
            	lennyActions.clearFile(lcaSyslogOnLenny);
            }
		    createLLM_input();
		    verifySyslogInAgent(LLM_Syslog_path, EP_Syslog_pattern);
		    
		    Thread.sleep(schedule_report_timeout);
	
	        if (log_type.equalsIgnoreCase( "SIEM")) {
	            verifyLogsSentToSiem(command_linuxSIEM);
	        } else if (log_type.equalsIgnoreCase("LCA")) {
	            verifyLogsSentToLca(command_linuxLCA);
	        } else if (log_type.equalsIgnoreCase("LCA_SYSLOG")) {
	            String command = String.format(command_linuxLCA_SYSLOG, expectedNumberOfMessages);
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

    	String res = agent.verifyExpectedOnCommandResult(command, ".zip was sent successfully");        
        org.testng.Assert.assertTrue(res != null);
                
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

        String res = agent.verifyExpectedOnCommandResult(command, ".txt was sent successfully");
        org.testng.Assert.assertTrue(res != null);
        
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
        String patt = String.format(EP_LCA_SYSLOG_log_pattern, expectedNumberOfMessages);
        String res = agent.verifyExpectedOnCommandResult(command, patt);
        
        // now check on LNE       
        JLog.logger.info("Going to verify logs received in Lenny according to logs on Lenny");
        res = lennyActions.numLinesinFile(lcaSyslogOnLenny, EP_Syslog_pattern);        
        org.testng.Assert.assertTrue(null != res , "Failed, response is null.");
        org.testng.Assert.assertTrue(res.contains(expectedNumberOfMessages), "Failed: number of messages is: " + res + ". Expected is: " + expectedNumberOfMessages);          
        JLog.logger.info("done");
    }
    

    private void verifySyslogInAgent(String filename, String pattern) {
        JLog.logger.info("Going to check that expected string '{}' exist in '{}' ", pattern, filename);
    	
        String res = agent.findInText(filename, pattern);
        org.testng.Assert.assertTrue(res!= null);
        int num_of_patterns = res.split(pattern,-1).length - 1;
        org.testng.Assert.assertEquals(Integer.parseInt(expectedNumberOfMessages),num_of_patterns);
        JLog.logger.info("done");
    }

    @AfterMethod
    public void Close(){
        if (agent!=null) {
            agent.close();
        }
        if(lennyActions!=null){
            lennyActions.Close();
        }
    }


}
