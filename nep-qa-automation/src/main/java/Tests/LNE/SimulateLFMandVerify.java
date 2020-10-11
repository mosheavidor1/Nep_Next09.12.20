package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Utils;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.Vector;


public class SimulateLFMandVerify extends GenericTest {

    private LNEActions lennyActions;
    private BaseAgentActions agent;
    private static String customerId;
    
    private static final String scp_path = "/work/services/siem/var/siem/data/nep/";
    private static final int schedule_report_timeout = 150000; //120 seconds
    private static final String wasSentSuccessString = " was sent successfully";
    private static int checkUPdatesInterval;
    private static final String file1Format = "%s-src.";    
    private static final String file2Format = ".%s-tag.log";
    
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
    }

    @Test(groups = { "SimulateLFMandVerify" } )
    public void SimulateLFMandVerifyDelivery()  {
    	try {
		    String log_type = data.get("Log_Type");
		    expectedResult1 = data.get("ExpectedResult1");
		    expectedResult2 = data.get("ExpectedResult2");
		    /*
		    if (data.get("EP_Type_1").equals("lnx")) {
    	    	return;
    	    }*/
		    
		    if (!log_type.equalsIgnoreCase( "SIEM")) {
		    	return;
		    }
		   		   		
		    JLog.logger.info("Starting SimulateLFMandVerifyDelivery. Agent type: {}. Log type: {}. Expected results: {} and {}.", data.get("EP_Type_1"), log_type, expectedResult1, expectedResult2);
		    agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
		    lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
		    
		    agent.clearFile(agent.getDbJsonPath());
		    agent.clearFile(agent.getAgentLogPath()); 
		    
		    String commandSIEM = agent.getVerifySiemCommand();
		    String commandLCA = agent.getVerifyLFMLca2Command();
		    String commandLCA2 = agent.getVerifyLca2Command();
		    
		   
		    
		    //Read configuration and update the host tags
		    String confJson = data.get("Settings Json");		    
            confJson = confJson.replaceAll(host_value_to_update, PropertiesFile.readProperty("ClusterToTest"));
            
		    lennyActions.SetCustomerConfiguration(customerId, confJson);
		    Thread.sleep(checkUPdatesInterval); //Waits until EP will get the new configuration
		    //TODO: compare json cofniguration on agent
		
		    prepareDirectories();
		    createLogs();
		    Thread.sleep(schedule_report_timeout);
	
	        if (log_type.equalsIgnoreCase( "SIEM")) {
	            verifyLogsSentToSiem(commandSIEM);
	        } else if (log_type.equalsIgnoreCase("LCA")) {
	        	verifyLogsSentToLca(commandLCA, "log", expectedResult1);
	        	verifyLogsSentToLca(commandLCA2, "txt", expectedResult2);
	        } else {
	            org.testng.Assert.fail("Unknown server log_type: " +  log_type);
	        }

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

    public void verifyLogsSentToSiem(String command) {
        // Here we have 2 zip files sent to LNE
    	JLog.logger.info("Going to verify logs sent to SIEM");
        String patt = ".zip was sent successfully";
        String res = agent.verifyExpectedOnCommandResult(command, patt);
        JLog.logger.info("res: " + res);
        org.testng.Assert.assertFalse(res == null);
        Vector<String> zipFiles = Utils.extractFileNames(res, "dla_", ".zip");

        for (int i = 0; i < zipFiles.size(); i++) {
            res = lennyActions.numLinesinFile(scp_path + zipFiles.elementAt(i), null);
            JLog.logger.info("res: " + res);           
            org.testng.Assert.assertTrue(null != res , "Failed, response is null.");
            org.testng.Assert.assertTrue((res.contains(expectedResult1) || res.contains(expectedResult2)), "Failed to find" );        
            
        }
        JLog.logger.info("done");
    }

    public void verifyLogsSentToLca(String command, String fileName, String expectedResult) {
    	JLog.logger.info("Going to verify logs sent to LCA");
        // Here we have 2 log files sent to LNE

        String expectedString = String.format(file2Format, fileName) + wasSentSuccessString;
        String res = agent.verifyExpectedOnCommandResult(command, expectedString);
        JLog.logger.info("res: " + res);
        org.testng.Assert.assertTrue(res != null);
        Vector<String> logFiles = Utils.extractFileNames(res, String.format(file1Format, fileName), String.format(file2Format, fileName));
        for (int i = 0; i < logFiles.size(); i++) {
            res = lennyActions.numLinesinFile(scp_path + logFiles.elementAt(i), null);
            JLog.logger.info("res: " + res);
            org.testng.Assert.assertTrue(null != res , "Failed, response is null.");
            org.testng.Assert.assertEquals(res, expectedResult, "Failed: number of messages is: " + res + ". Expected is: " + expectedResult);        
            
        }
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
