package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.Vector;


public class SimulateLFMandVerify extends GenericTest {

    private LNEActions lennyActions;
    private BaseAgentActions agent;
    private String customerId;
    
    private static final String scp_path = "/work/services/siem/var/siem/data/nep/";
    private static final int schedule_report_timeout = 120000; //ms
    private static final String wasSentSuccessString = " was sent successfully";
    private static final String file1Format = "%s-src.";    
    private static final String file2Format = ".%s-tag.log";

    String expectedResult1, expectedResult2;
    @Factory(dataProvider = "getData")
    public SimulateLFMandVerify(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
    }

    @Test()
    public void SimulateLFMandVerifyDelivery()  {
    	try {
	    	JLog.logger.info("Starting SimulateLFMandVerifyDelivery test ...");
	    	
		    String log_type = data.get("Log_Type");
		    expectedResult1 = data.get("ExpectedResult1");
		    expectedResult2 = data.get("ExpectedResult2");
		
		
		    JLog.logger.info("log_type:{} Expected results: {} and {}", log_type, expectedResult1, expectedResult2);
		    agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
		    
		    String commandSIEM = agent.getVerifySiemCommand();
		    String commandLCA = agent.getVerifyLFMLca2Command();
		    String commandLCA2 = agent.getVerifyLca2Command();
		    String logFile = agent.getAgentLogPath();
		    
		    lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
		
		    String confJson = data.get("Settings Json");
		   // agent.stopEPService(Integer.parseInt(general.get("EP Service Timeout")));
		   // Thread.sleep(5000);
		    lennyActions.SetCustomerConfiguration(customerId, confJson);
		
		
		    prepareDirectories();
		    agent.clearFile(logFile);
		    clearLFMDataromDB();
		 //   Thread.sleep(10000); //TODO: IS it needed, is it enough to wait until check updates occurs?
		  //  agent.startEPService(Integer.parseInt(general.get("EP Service Timeout")));
		 //TODO
		    //   agent.compareConfigurationToEPConfiguration(true);
		    Thread.sleep(10000);
		    createLogs();
		    Thread.sleep(schedule_report_timeout);
	
	        if (log_type.equalsIgnoreCase( "SIEM")) {
	            handleSIEM(commandSIEM);
	        } else if (log_type.equalsIgnoreCase("LCA")) {
	        	handleLCA(commandLCA, "log", expectedResult1);
	        	handleLCA(commandLCA2, "txt", expectedResult2);
	        } else {
	            org.testng.Assert.fail("Unknown server log_type: " +  log_type);
	        }

    	} catch (Exception e) {
    		org.testng.Assert.fail("SimulateLFMandVerifyDelivery failed" + "\n" + e.toString());
    	}
    }

     private void prepareDirectories() {
         String script_name = agent.getScriptName("LFM_Create_Dir");
         if (null == script_name)
             org.testng.Assert.fail("Can't find script : LFM_Create_Dir");
         String script_text = data.get(script_name);
         agent.writeAndExecute(script_text);
     }


     private void createLogs() {
         String script_name = agent.getScriptName("LFM_Create_Log");
         if (null == script_name)
             org.testng.Assert.fail("Can't find script : LFM_Create_Log");
         String script_text = data.get(script_name);
         agent.writeAndExecute(script_text);
     }

    public void handleSIEM(String command) {
        // Here we have 2 zip files sent to LNE
        String patt = ".zip was sent successfully";
        String res = agent.verifyExpectedOnCommandResult(command, patt);
        JLog.logger.info("res: " + res);
        org.testng.Assert.assertFalse(res == null);
        Vector<String> zipFiles = extractFileNames(res, "dla_", ".zip");

        for (int i = 0; i < zipFiles.size(); i++) {
            res = lennyActions.numLinesinFile(scp_path + zipFiles.elementAt(i), null);
            JLog.logger.info("res: " + res);
            org.testng.Assert.assertTrue((null != res) && (res.contains(expectedResult1) || res.contains((expectedResult2))));
        }
    }

    public Vector<String> extractFileNames(String lines, String startPattern, String endPattern) {
        Vector<String> fileNames = new Vector<String>();
        int file_start = 0;
        for (int i = 0;i < 2; i++) {
            int start = lines.indexOf(startPattern, file_start);
            int stop = lines.indexOf(endPattern, start);
            JLog.logger.info("start: {}, stop: {}", start, stop);
            org.testng.Assert.assertTrue(start != -1 && stop != -1);
            String zipFileMane = lines.substring(start, stop + endPattern.length());
            JLog.logger.info("file[" + i + "] Name: " + zipFileMane);
            fileNames.add(zipFileMane);
            file_start = stop;
        }
        return fileNames;
    }

    public void handleLCA(String command, String fileName, String expectedResult) {
        // Here we have 2 log files sent to LNE

        String expectedString = String.format(file2Format, fileName) + wasSentSuccessString;
        String res = agent.verifyExpectedOnCommandResult(command, expectedString);
        JLog.logger.info("res: " + res);
        org.testng.Assert.assertTrue(res != null);
        Vector<String> logFiles = extractFileNames(res, String.format(file1Format, fileName), String.format(file2Format, fileName));
        for (int i = 0; i < logFiles.size(); i++) {
            res = lennyActions.numLinesinFile(scp_path + logFiles.elementAt(i), null);
            JLog.logger.info("res: " + res);
            org.testng.Assert.assertTrue((null != res) && (res.contains(expectedResult)));
        }
        
    }

    public void clearLFMDataromDB() {
        agent.deleteLFMData();
    }

    @AfterMethod
    public void Close(){
        JLog.logger.info("Closing...");
        if (agent!=null) {
            agent.close();
        }
        if(lennyActions!=null){
            lennyActions.Close();
        }
    }


}
