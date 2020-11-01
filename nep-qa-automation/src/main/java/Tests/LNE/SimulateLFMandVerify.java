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
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.Vector;


public class SimulateLFMandVerify extends GenericTest {

	private static final LNEActions lennyActions = GlobalTools.getLneActions();
    private BaseAgentActions agent;
    private static String customerId;
    
    private static final String scp_path = "/work/services/siem/var/siem/data/nep/";
    private static final int schedule_report_timeout = 120000; //120 seconds
    private static final String wasSentSuccessString = " was sent successfully";
    private static int checkUPdatesInterval;
    private static final String filePrefixFormat = "%s-src.";    
    private static final String fileSuffixFormat = ".%s-tag.log";
    
    private Vector<String> destLogFiles;
    
   
    
    
    String expectedResult1, expectedResult2;
    @Factory(dataProvider = "getData")
    public SimulateLFMandVerify(Object dataToSet) {
        super(dataToSet);
    }
    
    @BeforeTest
    public void init() {
    	customerId = getGeneralData().get("Customer Id");
        checkUPdatesInterval = Integer.parseInt(getGeneralData().get("Check Updates Timeout")) * 1000; //35 seconds
        destLogFiles = null;
    }

    
    @Test(groups = { "SimulateLFMandVerify" } )
    public void SimulateLFMandVerifyDelivery()  {
    	try {
		    String log_type = data.get("Log_Type");
		    int expectedResult1 = Integer.parseInt(data.get("ExpectedResult1"));
		    int expectedResult2 = Integer.parseInt(data.get("ExpectedResult2"));
		    
		    String agentType = data.get("EP_Type_1");
		   		   		   		
		    JLog.logger.info("Starting SimulateLFMandVerifyDelivery. Agent type: {}. Log type: {}. Expected results: {} and {}.", data.get("EP_Type_1"), log_type, expectedResult1, expectedResult2);
		    agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
		    
		    //Set Basic configuration with LFM false
		    DsMgmtActions.SetCustomerConfiguration(customerId, ConfigHandling.getDefaultConfiguration());
		    Thread.sleep(checkUPdatesInterval); //Waits until EP will get the new configuration
		    
		    resetAgentAndPrepareDirs(agentType);
		    
		    //Read configuration and update the host tags
		    String confJsonName = data.get("Configuration Name");
		    String confJson = ConfigHandling.getConfiguration(confJsonName);	    
            confJson = confJson.replaceAll(ConfigHandling.lennyIpPlaceholder, GlobalTools.getClusterToTest());
            
            DsMgmtActions.SetCustomerConfiguration(customerId, confJson);
		    Thread.sleep(checkUPdatesInterval); //Waits until EP will get the new configuration
		    //TODO: compare json cofniguration on agent
		
		    
		    Thread.sleep(60000);//60 seconds
		    createLogs();
		    Thread.sleep(schedule_report_timeout);
	
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
    	int timeout = Integer.parseInt(getGeneralData().get("EP Installation timeout"));
    	
    	//if (agentType.equals("win")){
    		agent.stopEPService(timeout);
    //	}
	    prepareDirectories();
	    agent.clearFile(agent.getDbJsonPath());
	    agent.clearFile(agent.getAgentLogPath()); 
	    
	  //  if (agentType.equals("win")){
	    	agent.startEPService(timeout);
	   // }
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
        String res = agent.verifyExpectedOnCommandResult(command, patt);        
        
        destLogFiles = Utils.extractFileNames(res, "dla_", ".zip");
        org.testng.Assert.assertTrue(destLogFiles.size() > 1, "Failed to find at least 2 log files in log");
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
        String res = agent.verifyExpectedOnCommandResult(command, expectedString);
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

    @AfterTest
    public void clean(){
    	if (destLogFiles != null) {
    		for (int i = 0; i < destLogFiles.size(); i++) {
    			//TODO: delete files form destination
    		}
    	}
    }

    @AfterMethod
    public void Close(){
        if (agent!=null) {
            agent.close();
        }
        
        //Set Basic configuration with LFM false
	    DsMgmtActions.SetCustomerConfiguration(customerId, ConfigHandling.getDefaultConfiguration());
	    
    }


}
