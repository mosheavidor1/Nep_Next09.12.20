package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
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
    String syslogFileName;
    static final int schedule_report_timeout = 120000; //ms
    
    @Factory(dataProvider = "getData")
    public WLMCreateEvent(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
    }

    @Test()
    public void WLMCreateEventAndVerify()  {
        try {
        	JLog.logger.info("Starting WLMCreateEventAndVerify...");
        	
            if (!data.get("EP_Type_1").equals("win")) {
    	    	JLog.logger.info("This test should not run for {} OS, skipping", data.get("EP_Type_1"));
    	    	return;
    	    }

            JLog.logger.info("Opening...");
            String log_type = data.get("Log_Type");
            right_result = data.get("expectedResult");
            JLog.logger.info("log_type: " + log_type + " ; Expected result value: " + right_result);
            syslogFileName = syslog_path + data.get("EP_HostName_1") + "/user.log";

            lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
            
            String confJson =data.get("Settings Json");
            lennyActions.SetCustomerConfiguration(customerId, confJson);
            agent.stopEPService(Integer.parseInt(general.get("EP Service Timeout")));
            agent.clearFile(agent.getAgentLogPath());
            agent.startEPService(Integer.parseInt(general.get("EP Service Timeout")));

            Thread.sleep(10000);
           // TODO
            //agent.compareConfigurationToEPConfiguration(true);
            lennyActions.clearFile(syslogFileName);
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

    public boolean handleSIEM() {
        String command = agent.getVerifySiemCommand();
        String patt = ".zip was sent successfully";
        String res = agent.findPattern(command, patt);
        JLog.logger.info("res: " + res);
        if (res == null)
            return false;
        int start = res.lastIndexOf("dla_");
        JLog.logger.info("start: " + start);
        if (start == -1)
            return false;
        int stop = res.lastIndexOf(".zip");
        JLog.logger.info("stop: " + stop);
        if (stop == -1)
            return false;
        String zipFileMane = res.substring(start, stop + 4);
        JLog.logger.info("zip file Name: " + zipFileMane);

        res = lennyActions.numLinesinFile(scp_path + zipFileMane, null);
        JLog.logger.info("res: " + res);
        if ((null != res) && (res.contains(right_result)))
            return true;
        return false;
    }

    public boolean handleLCA() {
        String command = agent.getVerifyLcaCommand();
        String patt = ".txt was sent successfully";
        String res = agent.findPattern(command, patt);
        JLog.logger.info("res: " + res);
        if (res == null)
            return false;
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
        res = lennyActions.numLinesinFile(scp_path + txtFileMane, null);
        JLog.logger.info("res: " + res);
        if ((null != res) && (res.contains(right_result)))
            return true;
        return false;
    }

    public boolean handleLCA_SYSLOG() {
        String command = "type " + agent.getAgentLogPath() + " | find /n \"SecureSyslogCollector: sent " + right_result + " events\"";
        String patt = "SecureSyslogCollector: sent " + right_result + " events";
        String res = agent.findPattern(command, patt);
        if (res == null)
            return false;

        String txtFileMane = syslog_path + data.get("EP_HostName_1") + "/user.log";
        JLog.logger.info("txt file Name: " + txtFileMane);
        res = lennyActions.numLinesinFile(txtFileMane, null);
        JLog.logger.info("res: " + res);
        if ((null != res) && (res.contains(right_result)))
            return true;
        return false;
    }

    public void createEvents() {
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
