package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.AgentActionsInterface;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import javax.swing.*;
import java.util.Vector;


public class SimulateLFMandVerify extends GenericTest {

    private LNEActions lennyActions;
    private BaseAgentActions agent;
    private String customerId;
    
    static final String scp_path = "/work/services/siem/var/siem/data/nep/";
    static final int schedule_report_timeout = 120000; //ms

    String right_result1, right_result2;
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
		    right_result1 = data.get("ExpectedResult1");
		    right_result2 = data.get("ExpectedResult2");
		
		
		    JLog.logger.info("log_type: " + log_type + " Expected results: " + right_result1 + " and " + right_result2);
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
	
	        boolean res = false;
	
	        if (log_type.equalsIgnoreCase( "SIEM")) {
	            res = handleSIEM(commandSIEM);
	        } else if (log_type.equalsIgnoreCase("LCA")) {
	            res = handleLCA(commandLCA, commandLCA2);
	        } else {
	            org.testng.Assert.fail("Unknown server log_type: " +  log_type);
	        }
	
	        if (!res)
	            org.testng.Assert.fail("Could not find pattern in Agent.log for: " + log_type + " or number of lines did not match the expected value: ");

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

    public boolean handleSIEM(String command) {
        // Here we have 2 zip files sent to LNE
        boolean result = false;
        String patt = ".zip was sent successfully";
        String res = agent.findPattern(command, patt);
        JLog.logger.info("res: " + res);
        if (res == null)
            return false;
        Vector<String> zipFiles = extractFileNames(res, "dla_", ".zip");

        for (int i = 0; i < zipFiles.size(); i++) {
            res = lennyActions.numLinesinFile(scp_path + zipFiles.elementAt(i), null);
            JLog.logger.info("res: " + res);
            if ((null != res) && (res.contains(right_result1) || res.contains((right_result2))))
                result = true;
            else {
                result = false;
                break;
            }
        }
        return result;
    }

    public Vector<String> extractFileNames(String lines, String startPattern, String endPattern) {
        Vector<String> fileNames = new Vector<String>();
        int file_start = 0;
        for (int i = 0;i < 2; i++) {
            int start = lines.indexOf(startPattern, file_start);
            JLog.logger.info("start: " + start);
            if (start == -1)
                break;
            int stop = lines.indexOf(endPattern, start);
            JLog.logger.info("stop: " + stop);
            if (stop == -1)
                break;
            String zipFileMane = lines.substring(start, stop + endPattern.length());
            JLog.logger.info("file[" + i + "] Name: " + zipFileMane);
            fileNames.add(zipFileMane);
            file_start = stop;
        }
        return fileNames;
    }

    public boolean handleLCA(String command1, String command2) {
        // Here we have 2 log files sent to LNE
        boolean result = false;

        String patt = ".log-tag.log was sent successfully";
        String res = agent.findPattern(command1, patt);
        JLog.logger.info("res: " + res);
        if (res == null)
            return false;
        Vector<String> logFiles = extractFileNames(res, "log-src.", ".log-tag.log");
        for (int i = 0; i < logFiles.size(); i++) {
            res = lennyActions.numLinesinFile(scp_path + logFiles.elementAt(i), null);
            JLog.logger.info("res: " + res);
            if ((null != res) && (res.contains(right_result1)))
                result = true;
            else {
                result = false;
                break;
            }
        }
        if (!result)
            return result;
        // now searching for .txt.tag-log
        String command = command2;
        patt = ".txt-tag.log was sent successfully";
        res = agent.findPattern(command, patt);
        JLog.logger.info("res: " + res);
        if (res == null)
            return false;
        logFiles = extractFileNames(res, "txt-src.", ".txt-tag.log");
        for (int i = 0; i < logFiles.size(); i++) {
            res = lennyActions.numLinesinFile(scp_path + logFiles.elementAt(i), null);
            JLog.logger.info("res: " + res);
            if ((null != res) && (res.contains(right_result2)))
                result = true;
            else {
                result = true;
                break;
            }
        }
        return result;
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
