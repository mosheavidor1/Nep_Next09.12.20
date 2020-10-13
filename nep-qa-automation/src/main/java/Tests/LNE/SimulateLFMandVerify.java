package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Utils;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.Vector;


public class SimulateLFMandVerify extends GenericTest {

    private LNEActions lennyActions;
    private BaseAgentActions agent;
    private static String customerId;
    
    private static final String scp_path = "/work/services/siem/var/siem/data/nep/";
    private static final int schedule_report_timeout = 120000; //120 seconds
    private static final String wasSentSuccessString = " was sent successfully";
    private static int checkUPdatesInterval;
    private static final String filePrefixFormat = "%s-src.";    
    private static final String fileSuffixFormat = ".%s-tag.log";
    
    private Vector<String> destLogFiles;
    
    private static final String host_value_to_update = "\\{lenny-ip\\}";
    
    
    String expectedResult1, expectedResult2;
    @Factory(dataProvider = "getData")
    public SimulateLFMandVerify(Object dataToSet) {
        super(dataToSet);
    }
    
    @BeforeTest
    public void init() {
    	customerId = general.get("Customer Id");
        checkUPdatesInterval = Integer.parseInt(general.get("Check Updates Timeout")) * 1000; //35 seconds
        destLogFiles = null;
    }

    @Test(groups = { "SimulateLFMandVerify" } )
    public void SimulateLFMandVerifyDelivery()  {
    	try {
		    String log_type = data.get("Log_Type");
		    expectedResult1 = data.get("ExpectedResult1");
		    expectedResult2 = data.get("ExpectedResult2");
		    
		    		    
		    if (log_type.equalsIgnoreCase( "SIEM")) {
		    	return;
		    }
		   		   		
		    JLog.logger.info("Starting SimulateLFMandVerifyDelivery. Agent type: {}. Log type: {}. Expected results: {} and {}.", data.get("EP_Type_1"), log_type, expectedResult1, expectedResult2);
		    agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
		    lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
		    
		    //Set Basic configuration with LFM false
		    String confJson = data.get("Basic Conf");             
		    lennyActions.SetCustomerConfiguration(customerId, confJson);
		    Thread.sleep(checkUPdatesInterval); //Waits until EP will get the new configuration
		   
		    
		    prepareDirectories();
		    agent.clearFile(agent.getDbJsonPath());
		    agent.clearFile(agent.getAgentLogPath()); 
		    
		    
		    String commandSIEM = agent.getVerifySiemCommand();
		    String commandLCA = agent.getVerifyLFMLca2Command();
		    String commandLCA2 = agent.getVerifyLca2Command();
		    
		   
		    
		    //Read configuration and update the host tags
		    confJson = data.get("Settings Json");		    
            confJson = confJson.replaceAll(host_value_to_update, PropertiesFile.readProperty("ClusterToTest"));
            
		    lennyActions.SetCustomerConfiguration(customerId, confJson);
		    Thread.sleep(checkUPdatesInterval); //Waits until EP will get the new configuration
		    //TODO: compare json cofniguration on agent
		
		    
		    Thread.sleep(30000);
		    createLogs();
		    Thread.sleep(schedule_report_timeout);
	
	        if (log_type.equalsIgnoreCase( "SIEM")) {
	            verifyLogsSentToSiem(commandSIEM, Integer.parseInt(expectedResult1) + Integer.parseInt(expectedResult2));
	        } else if (log_type.equalsIgnoreCase("LCA")) {
	        	verifyLogsSentToLca(commandLCA, "log", Integer.parseInt(expectedResult1));
	        	verifyLogsSentToLca(commandLCA2, "txt", Integer.parseInt(expectedResult2));
	        } else {
	            org.testng.Assert.fail("Unknown server log_type: " +  log_type);
	        }
	        
	        JLog.logger.info("Completed test successfully\n");

    	} catch (Exception e) {
    		JLog.logger.error("Test failed", e);
            org.testng.Assert.fail("Test failed " + e.toString());
    	}
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
        // Here we have 2 zip files sent to LNE
    	JLog.logger.info("Going to verify logs sent to SIEM");
        String patt = ".zip was sent successfully";
        String res = agent.verifyExpectedOnCommandResult(command, patt);        
        
        destLogFiles = Utils.extractFileNames(res, "dla_", ".zip");
        org.testng.Assert.assertTrue(destLogFiles.size() > 1, "Failed to find at least 2 log files in log");
        int sumOfRows = 0;
        for (int i = 0; i < destLogFiles.size(); i++) {
            res = lennyActions.numLinesinFile(scp_path + destLogFiles.elementAt(i), null);
            org.testng.Assert.assertTrue(null != res , "Failed, num of lines is null in file" + destLogFiles.elementAt(i));
            int rowsCount = Integer.parseInt(res.trim());
            JLog.logger.info("Found '{}' rows in file '{}'", rowsCount, destLogFiles.elementAt(i));
            sumOfRows += rowsCount;
        }
        org.testng.Assert.assertTrue(sumOfRows == totalExpectedRows, "Failed to find" );
        JLog.logger.info("done");
    }

    public void verifyLogsSentToLca(String command, String fileName, int expectedResult) {
    	JLog.logger.info("Going to verify logs sent to LCA");
        // Here we have 2 log files sent to LNE

    	//File C:\ProgramData\Trustwave\NEPAgent\scp\send\txt-src.20201012.135813414.txt-tag.log was sent successfully
        String expectedString = String.format(fileSuffixFormat, fileName) + wasSentSuccessString;
        String res = agent.verifyExpectedOnCommandResult(command, expectedString);
        
        destLogFiles = Utils.extractFileNames(res, String.format(filePrefixFormat, fileName), String.format(fileSuffixFormat, fileName));
        int sumOfRows = 0;
        for (int i = 0; i < destLogFiles.size(); i++) {
            res = lennyActions.numLinesinFile(scp_path + destLogFiles.elementAt(i), null);
            org.testng.Assert.assertTrue(null != res , "Failed, num of lines is null in file" + destLogFiles.elementAt(i));
            int rowsCount = Integer.parseInt(res.trim());
            JLog.logger.info("Found '{}' rows in file '{}'", rowsCount, destLogFiles.elementAt(i));
            sumOfRows += rowsCount;
        }
        org.testng.Assert.assertTrue(sumOfRows == expectedResult, "Failed to find" );
        JLog.logger.info("done");
    }

    @AfterTest
    public void clean(){
    	if (destLogFiles != null) {
    		for (int i = 0; i < destLogFiles.size(); i++) {
    			//lennyActions.
    		}
    	}
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
