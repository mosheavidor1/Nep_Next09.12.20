package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Data.GlobalTools;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


//https://jira.trustwave.com/browse/NEP-1230
public class WLMCreateEvent extends GenericTest {

   
    private BaseAgentActions agent;
    private static String customerId;

    public static final String scp_path = "/work/services/siem/var/siem/data/nep/";
    public static final String syslog_path = "/work/services/siem/var/log/";
    String expectedLines;
    private static final int schedule_report_timeout = 120000; //120 seconds
    
    private static int checkUPdatesInterval;
    private static String agentIp;
    private static final String host_value_to_update = "\\{lenny-ip\\}";
    
    private static final LNEActions lennyActions = GlobalTools.getLneActions();
    
    @Factory(dataProvider = "getData")
    public WLMCreateEvent(Object dataToSet) {
        super(dataToSet);
        
       
    }
    
    @BeforeTest
    public void init() {
    	customerId = getGeneralData().get("Customer Id");
        checkUPdatesInterval = Integer.parseInt(getGeneralData().get("Check Updates Timeout")) * 1000; //35 seconds
        
    }

    @Test(groups = { "WLMCreateEvent" } )
    public void WLMCreateEventAndVerify()  {
        try {
        	
            if (!data.get("EP_Type_1").equals("win")) {
    	    	JLog.logger.info("This test should not run for {} OS, skipping", data.get("EP_Type_1"));
    	    	return;
    	    }

            String log_type = data.get("Log_Type");
            expectedLines = data.get("expectedResult");
            
            JLog.logger.info("Starting WLMCreateEventAndVerify. log_type: {}. Expected result value: {}", log_type, expectedLines);
           
            agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
            
            //Read configuration and update the host tags
            String confJson = data.get("Settings Json");            
            confJson = confJson.replaceAll(host_value_to_update, GlobalTools.getClusterToTest());
            
            agentIp = data.get("EP_HostName_1");
            
            DsMgmtActions.SetCustomerConfiguration(customerId, confJson);            
            Thread.sleep(checkUPdatesInterval); //Waits until EP will get the new configuration
            //TODO: compare json cofniguration on agent
            
           // agent.clearFile(agent.getAgentLogPath()); //clear fails since file is in use
            //TODO: instead of clearing the file, tail it to a different new file
            if (log_type.equalsIgnoreCase("LCA_SYSLOG")) {
            	clearSyslogFile();
            }
           
            createEvents();
            Thread.sleep(schedule_report_timeout);

            if (log_type.equalsIgnoreCase( "SIEM")) {
                verifyLogsSentToSiem();
            } else if (log_type.equalsIgnoreCase("LCA")) {
                verifyLogsSentToLCA();
            } else if (log_type.equalsIgnoreCase("LCA_SYSLOG")) {
                verifyLogsSentToLCA_SYSLOG();
            } else {
                org.testng.Assert.fail("Unknown server log_type: " +  log_type);
            }
            JLog.logger.info("Completed test successfully\n");

        }
        catch (Exception e) {
        	JLog.logger.error("Test failed", e);
            org.testng.Assert.fail("Test failed " + e.toString());
        }

    }
    
    private void clearSyslogFile() {
    	JLog.logger.info("Going to clear file 'user.log' on Lenny");
    	String syslogFileName = syslog_path + agentIp + "/user.log";
    	if (lennyActions.fileExists(syslogFileName)) {
    		lennyActions.clearFile(syslogFileName);
    		JLog.logger.info("Done");
    		return;
    	}       
        JLog.logger.info("File was not found on path {}", syslogFileName);
    }
    
    public void verifyLogsSentToSiem() {
    	JLog.logger.info("Going to verify logs sent to SIEM");
    	
        String command = agent.getVerifySiemCommand();
        String expectedStr = ".zip was sent successfully";
        
        String res = agent.verifyExpectedOnCommandResult(command, expectedStr);
        
        int start = res.lastIndexOf("dla_");
        int stop = res.lastIndexOf(".zip");
        org.testng.Assert.assertTrue(start != -1 && stop != -1, "Failed to find txt file name in result");
        
        String zipFileMane = res.substring(start, stop + 4);
        JLog.logger.info("Going to verify number of lines in {}", scp_path + zipFileMane);
        res = lennyActions.numLinesinFile(scp_path + zipFileMane, null);
        org.testng.Assert.assertTrue(res != null && res.contains(expectedLines));
        JLog.logger.info("done");
    }

    public void verifyLogsSentToLCA() {
    	JLog.logger.info("Going to verify logs sent to LCA");
    	
        String command = agent.getVerifyLcaCommand();       
        String patt = ".txt was sent successfully";
        
        String res = agent.verifyExpectedOnCommandResult(command, patt);
        
        int start = res.lastIndexOf("dla_");
        int stop = res.lastIndexOf(".txt");
        org.testng.Assert.assertTrue(start != -1 && stop != -1, "Failed to find txt file name in result");
        
        String txtFileMane = res.substring(start, stop + 4);
        JLog.logger.info("Going to verify number of lines in {}", scp_path + txtFileMane);
        res = lennyActions.numLinesinFile(scp_path + txtFileMane, null);        
        org.testng.Assert.assertTrue(res != null && res.contains(expectedLines));
        JLog.logger.info("done");
    }

    public void verifyLogsSentToLCA_SYSLOG() {
    	JLog.logger.info("Going to verify logs sent to LCA SYSLOG");
    	
        String command = "type " + agent.getAgentLogPath() + " | find /n \"SecureSyslogCollector: sent " + expectedLines + " events\"";
        String expectedStr = "SecureSyslogCollector: sent " + expectedLines + " events";
        
        String res = agent.verifyExpectedOnCommandResult(command, expectedStr);
                
        String txtFileMane = syslog_path + agentIp + "/user.log";
        if (!lennyActions.fileExists(txtFileMane)) {
        	JLog.logger.info("File on path {} does not exist", txtFileMane);
        	org.testng.Assert.fail("Could not find user.log file");        	
        }
        JLog.logger.info("Going to verify number of lines in {} and expect {} lines", txtFileMane, expectedLines);
        res = lennyActions.numLinesinFile(txtFileMane, null);
        org.testng.Assert.assertTrue(res != null && res.contains(expectedLines));
        JLog.logger.info("done");
    }
   
    public void createEvents() {
    	JLog.logger.info("Starting writing events.");
    	
        String[][] obj = {
/*                {"information", "111","Microsoft-Windows-Windows Defender/Operational", "Windows Defender","WLM test log included - win defender info"},
                {"error", "555","Microsoft-Windows-Windows Defender/Operational", "Windows Defender","WLM test log included - win defender error" },
                {"information", "110","Microsoft-Windows-Windows Defender/Operational", "WLM test log excluded - win defender info" },
                {"error", "556","Microsoft-Windows-Windows Defender/Operational", "WLM test log excluded - win defender error" },
     */           {"error", "111","application", "wlm_test_source","WLM test log included - application error"},
                {"warning", "333","application", "wlm_test_source","WLM test log included - application warning" },
                {"error", "110","application", "wlm_test_source","WLM test log excluded - application error" },
                {"error", "111","application", "wlm_test_source2","WLM test log excluded - application error"},
                {"information", "111","system", "TestSource1","WLM test log included - system information" },
                {"information", "111","system", "TestSource2","WLM test log included - system information" },
                {"error", "111","system", "TestSource2","WLM test log excluded - system error"},
                {"information", "333","system", "TestSource1","WLM test log excluded - system information" },
  /*              {"error", "999","setup", "SetupTestSource","WLM test log included - setup error" },
                {"error", "1000","setup", "SetupTestSource","WLM test log included - setup error" },
                {"warning", "999","setup", "SetupTestSource","WLM test log excluded - setup warning" },
                {"error", "123","setup", "SetupTestSource1","WLM test log excluded - setup error" },
       */        };

        for (int i = 0; i < obj.length; i++) {
            LogEntry lent = new LogEntry(obj[i][0],obj[i][1],obj[i][2],obj[i][3],obj[i][4],true);
            agent.writeEvent(lent);
        }
        
        JLog.logger.info("Finished writing events.");
    }
    
    
    @AfterMethod
    public void Close(){
        if (agent!=null) {
            agent.close();
        }
    }


}
