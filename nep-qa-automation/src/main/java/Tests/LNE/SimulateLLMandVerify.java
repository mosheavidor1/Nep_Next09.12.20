package Tests.LNE;

import Actions.AgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.Vector;


public class SimulateLLMandVerify extends GenericTest {

    private LNEActions manager;
    private AgentActions endpoint;
    public static final String scp_path = "/work/services/siem/var/siem/data/nep/";
    public static final String syslog_path = "/work/services/siem/var/log/";
    static final String command_linuxSIEM = "cat /opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log | grep -e \".zip was sent successfully\"";
    static final String LLM_Syslog_path = "/opt/tw-endpoint/data/llm/monitor_dir/syslogs";
    static final String EP_Syslog_pattern = "LLM Test message #";
    static final String EP_LCA_SYSLOG_log_pattern = "Sent 29 events.";
    static final String command_linuxLCA = "cat /opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log | grep -e \".txt was sent successfully\"";
    static final String command_linuxLCA_SYSLOG = "cat /opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log | grep -e \"Sent 29 events.\"";
    static final int schedule_report_timeout = 120000; //ms
    String right_result;
    @Factory(dataProvider = "getData")
    public SimulateLLMandVerify(Object dataToSet) {
        super(dataToSet);
    }

    @Test()
    public void SimulateLLMandVerifyDelivery()  {
    try {
    JLog.logger.info("Opening...");
    String log_type = data.get("Log_Type");
    JLog.logger.info("log_type: " + log_type);
    String commandSIEM;
    String commandLCA;
    endpoint = new AgentActions(data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
    AgentActions.EP_OS epOs = AgentActions.EP_OS.LINUX;

    commandSIEM = command_linuxSIEM;
    commandLCA = command_linuxLCA;
    right_result = data.get("ExpectedResult");

    manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));

    String confJson = data.get("Settings Json");

    manager.SetCustomerConfiguration(confJson);
    endpoint.StopEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
    endpoint.StartEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
    endpoint.CompareConfigurationToEPConfiguration(confJson, epOs);

    endpoint.clearFile("/opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log", epOs);
    endpoint.clearFile(LLM_Syslog_path, epOs);
    Thread.sleep(10000);
    createLLM_input();
    if (false == checkEPSyslog(LLM_Syslog_path, EP_Syslog_pattern)) {
        JLog.logger.info("pattern " + EP_Syslog_pattern + " was not found in " + LLM_Syslog_path);
        org.testng.Assert.fail("Test LLM for " + log_type + "failed");
    }
    Thread.sleep(schedule_report_timeout);

        boolean res = false;

        if (log_type.equalsIgnoreCase( "SIEM")) {
            res = handleSIEM(commandSIEM);
        } else if (log_type.equalsIgnoreCase("LCA")) {
            res = handleLCA(commandLCA);
        } else if (log_type.equalsIgnoreCase("LCA_SYSLOG")) {
            res = handleLCA_SYSLOG(command_linuxLCA_SYSLOG);
        }else {
            org.testng.Assert.fail("Unknown server log_type: " +  log_type);
        }

        if (!res)
            org.testng.Assert.fail("Could not find pattern in Agent.log for: " + log_type + " or number of lines did not match the expected value: ");

    } catch (Exception e) {
        org.testng.Assert.fail("SimulateLLMandVerifyDelivery failed" + "\n" + e.toString());
     }
    }

     private void createLLM_input() {
         String script;
         script = data.get("LLMScript");
         endpoint.writeAndExecute(script, AgentActions.EP_OS.LINUX);
     }

    public boolean handleSIEM(String command) {
        // Here we have 2 zip files sent to LNE
        boolean result = false;
        String patt = ".zip was sent successfully";
        String res = endpoint.findPattern(command, patt);
        JLog.logger.info("res: " + res);
        if (res == null)
            return false;
        Vector<String> zipFiles = extractFileNames(res, "dla_", ".zip");

        for (int i = 0; i < zipFiles.size(); i++) {
            res = manager.numLinesinFile(scp_path + zipFiles.elementAt(i), EP_Syslog_pattern);
            JLog.logger.info("res: " + res);
            if ((null != res) && (res.contains(right_result)))
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

    private boolean checkEPSyslog(String filename, String pattern) {
        boolean result = false;
        String res = endpoint.findInText(filename, pattern);
        if (res != null) {
            int num_of_patterns = res.split(pattern,-1).length - 1;
            JLog.logger.info("Found " + num_of_patterns + " patterns: " + pattern);
            result = true;
        }
        return result;
    }

    public boolean handleLCA(String command) {
        // Here we have 2 log files sent to LNE
        boolean result = false;

        String patt = ".txt was sent successfully";
        String res = endpoint.findPattern(command, patt);
        JLog.logger.info("res: " + res);
        if (res == null)
            return false;
        Vector<String> logFiles = extractFileNames(res, "dla_", ".txt");
        for (int i = 0; i < logFiles.size(); i++) {
            res = manager.numLinesinFile(scp_path + logFiles.elementAt(i), EP_Syslog_pattern);
            JLog.logger.info("res: " + res);
            if ((null != res) && (res.contains(right_result)))
                result = true;
            else {
                result = false;
                break;
            }
        }
        return result;
    }

    public boolean handleLCA_SYSLOG(String command) {
        boolean result = true;

        String patt = EP_LCA_SYSLOG_log_pattern;
        String res = endpoint.findPattern(command, patt);
        JLog.logger.info("res: " + res);
        if (res == null)
            return false;
        // now check on LNE
        String txtFileMane = syslog_path + data.get("EP_HostName_1") + "/user.log";
        res = manager.numLinesinFile(txtFileMane, EP_Syslog_pattern);
        JLog.logger.info("res: " + res);
        if ((null != res) && (res.contains(EP_LCA_SYSLOG_log_pattern)))
            result = true;
        else {
            result = false;
        }
        return result;
    }

    @AfterMethod
    public void Close(){
        JLog.logger.info("Closing...");
        if (endpoint!=null) {
            endpoint.Close();
        }
        if(manager!=null){
            manager.Close();
        }
    }


}
