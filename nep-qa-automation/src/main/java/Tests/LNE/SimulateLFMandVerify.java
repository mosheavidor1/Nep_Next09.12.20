package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.DsMgmtActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.ConfigHandling;
import Utils.Utils;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.Vector;


public class SimulateLFMandVerify extends GenericTest {

	private static final LNEActions lennyActions = GlobalTools.getLneActions();
    private BaseAgentActions agent;
    private static String customerId;
    
    private static final String scp_path = "/work/services/siem/var/siem/data/nep/";
    private static final String wasSentSuccessString = " was sent successfully";
    private static final String filePrefixFormat = "%s-src.";    
    private static final String fileSuffixFormat = ".%s-tag.log";
    private static int agentInstallTimeout;
    private static int verifyLogsTimeout = 120;//120 seconds
    private static int checkUPdatesInterval;
    private static int testResultTimeout;
    private static boolean is_using_proxy = false;
    private static boolean is_proxy_valid = false;
    private static String  proxy_IP;
    private static String proxy_Port;

    private Vector<String> destLogFiles;
    
    
    String expectedResult1, expectedResult2;
    @Factory(dataProvider = "getData")
    public SimulateLFMandVerify(Object dataToSet) {
        super(dataToSet);
    }
    
    @BeforeTest
    public void init() {
    	customerId = getGeneralData().get("Customer Id");
        destLogFiles = null;
        agentInstallTimeout = Integer.parseInt(getGeneralData().get("EP Installation timeout"));
        checkUPdatesInterval = Integer.parseInt(getGeneralData().get("Check Updates Timeout")) * 1000; //35 seconds
    }
    private void initProxy(String proxy_ip) {
        if (proxy_ip.equalsIgnoreCase("LNE"))
            proxy_IP = GlobalTools.getClusterToTest();
        else
            proxy_IP = proxy_ip;

        proxy_Port = data.get("Proxy_Port");
        String v = data.get("Proxy_Valid");
        is_proxy_valid = v.equalsIgnoreCase("yes");
    }
    
    @Test(groups = { "SimulateLFMandVerify" }, priority=60 )
    public void SimulateLFMandVerifyDelivery()  {
    	try {
    	    JLog.logger.info("Starting Simulate LFM and Verify test...");


            String log_type = data.get("Log_Type");
		    int expectedResult1 = Integer.parseInt(data.get("ExpectedResult1"));
		    int expectedResult2 = Integer.parseInt(data.get("ExpectedResult2"));
            testResultTimeout = Integer.parseInt(data.get("Result_timeout")) * 1000; // seconds
            String proxy_ip = data.get("Proxy_IP");
            is_using_proxy = !proxy_ip.isEmpty();
            if (is_using_proxy) {
                initProxy(proxy_ip);
            }
		    String agentType = data.get("EP_Type_1");
		   		   		   		
		    JLog.logger.info("Starting SimulateLFMandVerifyDelivery. Agent type: {}. Log type: {}. Expected results: {} and {}.", data.get("EP_Type_1"), log_type, expectedResult1, expectedResult2);
            JLog.logger.info("Using proxy: {}",is_using_proxy);
            if (is_using_proxy)
                JLog.logger.info("Proxy IP: {}; Proxy Port: {}, Proxy is Valid: {}", proxy_IP, proxy_Port, is_proxy_valid);

            JLog.logger.info("Going to set configuration with LFM disabled, and waits until agent updates");
		    DsMgmtActions.SetCustomerConfiguration(customerId, ConfigHandling.getConfiguration("LLM LFM Disabled"));
		    Thread.sleep(checkUPdatesInterval); //Waits until EP will get the new configuration
		    
		    
		    //Read configuration and update the host tags
		    String confJsonName = data.get("Configuration Name");
		    String confJson = ConfigHandling.getConfiguration(confJsonName);	    
            confJson = confJson.replaceAll(ConfigHandling.lennyIpPlaceholder, GlobalTools.getClusterToTest());
            
		    agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
		    
		    JLog.logger.info("Going to stop the agent service, clean agent logs/files, set the needed configuration for the customer and finally start the agent service.");
            if (is_using_proxy) {
                agent.enableProxy(proxy_IP, proxy_Port);
                Thread.sleep(5000);
            }
		    agent.stopEPService(agentInstallTimeout);
		    
		    //Set the conf here, so that the endpoint will get it right upon starting
		    DsMgmtActions.SetCustomerConfiguration(customerId, confJson);
		    //TODO: compare json cofniguration on agent
		    
		    resetAgentAndPrepareDirs(agentType);   
            
            agent.startEPService(agentInstallTimeout);		
		    
            JLog.logger.info("Going to sleep {} seconds before generating LFM input", testResultTimeout/1000);
		    Thread.sleep(testResultTimeout);// seconds
		    createLogs();
		    JLog.logger.info("Going to sleep 1 minute until events/files are collected");
		    Thread.sleep(60000);//60 seconds
	
	        if (log_type.equalsIgnoreCase( "SIEM")) {
	            verifyLogsSentToSiem(agent.getVerifySiemCommand(), expectedResult1 + expectedResult2);
	        } else if (log_type.equalsIgnoreCase("LCA")) {
	        	verifyLogsSentToLca(agent.getVerifyLFMLca2Command(), agent.getVerifyLca2Command(), expectedResult1 + expectedResult2);
	        } else {
	            org.testng.Assert.fail("Unknown server log_type: " +  log_type);
	        }
	        
	        JLog.logger.info("Completed test successfully\n");

    	} catch (Exception e) {
    		JLog.logger.error("Test failed", e);
            org.testng.Assert.fail("Test failed " + e.toString());
    	}
    }
    
    /**
     * This function resets the agent between runs; cleans log files and monitored folders.
     * For windows we must stop the agent in order to clear the db.json
     */
    private void resetAgentAndPrepareDirs(String agentType) {
    	
	    prepareDirectories();
	    agent.clearFile(agent.getDbJsonPath());
	    agent.clearFile(agent.getAgentLogPath()); 
	    
    }

     private void prepareDirectories() {
    	 JLog.logger.info("Going to prepareDirectories");
         String script_name = agent.getScriptName("LFM_Create_Dir");
         org.testng.Assert.assertTrue(script_name!= null,"Can't find script : LFM_Create_Dir");
         String script_text = data.get(script_name);
         agent.writeAndExecute(script_text, "lfmTestPrepareDirs");
         JLog.logger.info("done");
     }


     private void createLogs() {
    	 JLog.logger.info("Going to run script to create LFM input");
         String script_name = agent.getScriptName("LFM_Create_Log");
         org.testng.Assert.assertTrue(script_name!= null,"Can't find script : LFM_Create_Log");
         String script_text = data.get(script_name);
         agent.writeAndExecute(script_text, "lfmTestCreateInput");
         JLog.logger.info("done");
     }

    public void verifyLogsSentToSiem(String command, int totalExpectedRows) {
    	JLog.logger.info("Going to verify logs sent to SIEM");
        String patt = ".zip was sent successfully";
        String res = agent.verifyExpectedOnCommandResult(command, patt, verifyLogsTimeout);       
        org.testng.Assert.assertTrue(null != res, "Failed to find expected files in log.");
        
        destLogFiles = Utils.extractFileNames(res, "dla_", ".zip");
        org.testng.Assert.assertTrue(destLogFiles.size() > 1, "Failed to find at least 2 log files in agent log.");
        int totalSentRows = getTotalRows(destLogFiles);
        org.testng.Assert.assertEquals(totalSentRows, totalExpectedRows, "Total of sent rows doesn't equal." );
        JLog.logger.info("done");
    }

    private void verifyLogsSentToLca(String commandLCA, String commandLCA2, int totalExpectedRows) {
    	JLog.logger.info("Going to verify logs sent to LCA");
    	
    	verifyExpectedInLogAndExtractFileNames(commandLCA, "log");
    	int totalSentRows = getTotalRows(destLogFiles);   
    	verifyExpectedInLogAndExtractFileNames(commandLCA2, "txt");
    	totalSentRows += getTotalRows(destLogFiles);         
             
        org.testng.Assert.assertEquals(totalSentRows, totalExpectedRows, "Total of sent rows doesn't equal." );
        JLog.logger.info("done");
    }
    
    private void verifyExpectedInLogAndExtractFileNames(String command, String fileName) {
    	String expectedString = String.format(fileSuffixFormat, fileName) + wasSentSuccessString;
        String res = agent.verifyExpectedOnCommandResult(command, expectedString, verifyLogsTimeout);
        org.testng.Assert.assertTrue(null != res, "Failed to find expected files in log");
        destLogFiles = Utils.extractFileNames(res, String.format(filePrefixFormat, fileName), String.format(fileSuffixFormat, fileName));
        
    }
    
    private int getTotalRows(Vector<String> logFiles) {
    	int sumOfRows = 0;
        for (int i = 0; i < logFiles.size(); i++) {
            String res = lennyActions.numLinesinFile(scp_path + logFiles.elementAt(i), null);
            org.testng.Assert.assertTrue(null != res , "Failed, num of lines is null in file" + logFiles.elementAt(i));
            int rowsCount = Integer.parseInt(res.trim());
            JLog.logger.info("Found '{}' rows in file '{}'", rowsCount, logFiles.elementAt(i));
            sumOfRows += rowsCount;
        }
        return sumOfRows;
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

    @AfterMethod
    public void Close(){
        if (agent!=null) {
            if (is_using_proxy) {
                agent.disableProxy();
            }
            agent.close();
        }
       	    
    }


}
