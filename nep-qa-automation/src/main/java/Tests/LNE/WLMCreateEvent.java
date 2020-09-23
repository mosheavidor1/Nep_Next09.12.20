package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

import java.time.Duration;
import java.time.LocalDateTime;

import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;



public class WLMCreateEvent extends GenericTest {

    private LNEActions lennyActions;
    private BaseAgentActions agent;
    private String customerId;

    public static final String scp_path = "/work/services/siem/var/siem/data/nep/";
    public static final String syslog_path = "/work/services/siem/var/log/";
    // result depends on filter settings in relevant config.json and on number of created events per config.json
    // currently, number of lines detected by ep is 2.
    String right_result;
    private static final int schedule_report_timeout = 120000; //120 seconds
    private static final int checkInterval = 5000; //5 seconds
    private static int checkUPdatesInterval;
    
    private static final String localIP = "127.0.0.1";
    
    @Factory(dataProvider = "getData")
    public WLMCreateEvent(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
        checkUPdatesInterval = Integer.parseInt(general.get("Check Updates Timeout")) * 1000; //35 seconds
    }

    @Test()
    public void WLMCreateEventAndVerify()  {
        try {
        	
            if (!data.get("EP_Type_1").equals("win")) {
    	    	JLog.logger.info("This test should not run for {} OS, skipping", data.get("EP_Type_1"));
    	    	throw new SkipException("This test should not run for OS other than Windows, skipping");
    	    }

            String log_type = data.get("Log_Type");
            right_result = data.get("expectedResult");

            JLog.logger.info("Starting WLMCreateEventAndVerify. log_type: {}. Expected result value: {}", log_type, right_result);
            
             lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
            
            String confJson = data.get("Settings Json");
            lennyActions.SetCustomerConfiguration(customerId, confJson);
            
            Thread.sleep(checkUPdatesInterval); //Waits until EP will get the new configuration
            //TODO: how to compare?
            
            agent.clearFile(agent.getAgentLogPath());
            if (log_type.equalsIgnoreCase("LCA_SYSLOG")) {
            	clearSyslogFile();
            }

           
            createEvents();
            Thread.sleep(schedule_report_timeout);
            boolean res = false;

            if (log_type.equalsIgnoreCase( "SIEM")) {
                res = handleSIEM();
            } else if (log_type.equalsIgnoreCase("LCA")) {
                res = handleLCA();
            } else if (log_type.equalsIgnoreCase("LCA_SYSLOG")) {
                res = handleLCA_SYSLOG();
            } else {
                org.testng.Assert.fail("Unknown server log_type: " +  log_type);
            }

            if (!res)
                org.testng.Assert.fail("Could not find pattern in Agent.log for: " + log_type + " or number of lines did not match the expected value: " +right_result);
        }
        catch (Exception e) {
                org.testng.Assert.fail("WLMCreateEventAndVerify ." + "\n" + e.toString());
            }

    }
    
    private void clearSyslogFile() {
    	JLog.logger.info("Going to clear file 'user.log' on Lenny");
    	String syslogFileName = syslog_path + getAgentIPFolder() + "/user.log";
    	if (lennyActions.fileExists(syslogFileName)) {
    		lennyActions.clearFile(syslogFileName);
    		JLog.logger.info("Done");
    		return;
    	}
        JLog.logger.info("File on path {} does not exist", syslogFileName);
        syslogFileName = syslog_path + "127.0.0.1" + "/user.log";
        if (lennyActions.fileExists(syslogFileName)) {
        	JLog.logger.info("Cleard on path {}", syslogFileName);
    		return;
        }
        JLog.logger.info("File was not found");
    }
    
    public boolean handleSIEM() {
    	JLog.logger.info("Starting handleSIEM");
    	
        String command = agent.getVerifySiemCommand();
        String patt = ".zip was sent successfully";
        JLog.logger.info("Going to run command {}", command);
        String res = agent.findPattern(command, patt);
        
        if (res == null) {
        	JLog.logger.error("Find pattern response is null!");
            return false;
        }
        JLog.logger.info("Find pattern response: {}", res);
        
        int start = res.lastIndexOf("dla_");
        JLog.logger.info("start: {}", start);
        if (start == -1)
            return false;
        int stop = res.lastIndexOf(".zip");
        JLog.logger.info("stop: {}", stop);
        if (stop == -1)
            return false;
        String zipFileMane = res.substring(start, stop + 4);
        JLog.logger.info("zip file Name: {}", zipFileMane);

        JLog.logger.info("Going to verify number of lines in {}", scp_path + zipFileMane);
        res = lennyActions.numLinesinFile(scp_path + zipFileMane, null);
        JLog.logger.info("Got: " + res);

        return ((null != res) && (res.contains(right_result)));
    }

    public boolean handleLCA() {
    	JLog.logger.info("Starting handleLCA");
        String command = agent.getVerifyLcaCommand();
        JLog.logger.info("Going to run command {}", command);
        String patt = ".txt was sent successfully";
        
        String res = agent.findPattern(command, patt);
       
        if (res == null) {
        	JLog.logger.error("Find pattern response is null!");
            return false;
        }
        
        JLog.logger.info("Find pattern response: {}", res);
        
        int start = res.lastIndexOf("dla_");
        JLog.logger.info("start: " + start);
        if (start == -1)
            return false;
        int stop = res.lastIndexOf(".txt");
        JLog.logger.info("stop: " + stop);
        if (stop == -1)
            return false;
        
        String txtFileMane = res.substring(start, stop + 4);
        JLog.logger.info("txt file Name: " + txtFileMane);
        
        JLog.logger.info("Going to verify number of lines in {}", scp_path + txtFileMane);
        res = lennyActions.numLinesinFile(scp_path + txtFileMane, null);
        JLog.logger.info("Got: " + res);
        
        return ((null != res) && (res.contains(right_result)));
    }

    public boolean handleLCA_SYSLOG() {
    	JLog.logger.info("Starting handleLCA_SYSLOG");
    	
        String command = "type " + agent.getAgentLogPath() + " | find /n \"SecureSyslogCollector: sent " + right_result + " events\"";
        String patt = "SecureSyslogCollector: sent " + right_result + " events";
        JLog.logger.info("Going to run command {}", command);
        String res = agent.findPattern(command, patt);
        
        if (res == null) {
        	JLog.logger.error("Find pattern response is null!");
            return false;
        }
        JLog.logger.info("Find pattern response: {}", res);
        String txtFileMane = syslog_path + getAgentIPFolder() + "/user.log";
        if (!lennyActions.fileExists(txtFileMane)) {
        	JLog.logger.info("File on path {} does not exist", txtFileMane);
        	txtFileMane = syslog_path + "127.0.0.1" + "/user.log";
        	if (!lennyActions.fileExists(txtFileMane)) {
        		JLog.logger.error("File on path {} does not exist either", txtFileMane);
        		 org.testng.Assert.fail("Could not find user.log file");
        	}
        }
        JLog.logger.info("Going to verify number of lines in {}", txtFileMane);
        res = lennyActions.numLinesinFile(txtFileMane, null);
        JLog.logger.info("Got: " + res);
        
        return ((null != res) && (res.contains(right_result)));
    }
    
    private String getAgentIPFolder() {
    	String hostIp = data.get("EP_HostName_1");
    	String lennyIp = PropertiesFile.readProperty("ClusterToTest");
    	
    	if (hostIp != null && lennyIp != null && hostIp.equals(lennyIp)) {
    		return localIP;
    	}
    	return hostIp;
    	
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
    
    //Waits until check updates will run, and uninstall will be done as a result
    /*
    public void checkUpdatesUntilConfIsUpdated(String conf) {

        boolean deleted = false;

        try {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(Integer.parseInt(general.get("Check Updates Timeout")));

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                if (!endpointServiceRunning()) {
                    deleted = true;
                    JLog.logger.info("Endpoint service was stopped!");
                    break;
                }
            }
        } catch (InterruptedException e) {
        	JLog.logger.info("Got interrupted exception");
        }

        if(!deleted){
            org.testng.Assert.fail("Endpoint deleted verification failed, the endpoint service still running.");
        }
    }*/

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
