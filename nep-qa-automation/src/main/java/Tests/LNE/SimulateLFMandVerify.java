package Tests.LNE;

import Actions.AgentActions;
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

    private LNEActions manager;
    private AgentActions endpoint;
    String scp_path = "/work/services/siem/var/siem/data/nep/";
    static final String command_winSIEM = "type C:\\ProgramData\\Trustwave\\NEPAgent\\logs\\NewAgent_0.log | find /n \".zip was sent successfully\"";
    static final String command_linuxSIEM = "cat /opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log | grep -e \".zip was sent successfully\"";
    static final String command_winLCA = "type C:\\ProgramData\\Trustwave\\NEPAgent\\logs\\NewAgent_0.log | find /n \".log-tag.log was sent\"";
    static final String command_linuxLCA = "cat /opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log | grep -e \".log-tag.log was sent\"";
    static final int schedule_report_timeout = 65000; //ms
    static final String expected_SIEM_win = "3";
    static final String expected_LCA_win = "3";
    static final String expected_SIEM_lnx = "5";
    static final String expected_LCA_lnx = "5";
    String right_result_SIEM;
    String right_result_LCA;
    @Factory(dataProvider = "getData")
    public SimulateLFMandVerify(Object dataToSet) {
        super(dataToSet);
    }

    @Test()
    public void SimulateLFMandVerifyDelivery()  {
    try {
    JLog.logger.info("Opening...");
    String log_type = data.get("Log_Type");
    JLog.logger.info("log_type: " + log_type);
    String commandSIEM;
    String commandLCA;
    endpoint = new AgentActions(data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
    AgentActions.EP_OS epOs = data.get("EP_Type_1").contains("win") ? AgentActions.EP_OS.WINDOWS : AgentActions.EP_OS.LINUX;
    if (epOs == AgentActions.EP_OS.WINDOWS) {
        commandSIEM = command_winSIEM;
        commandLCA = command_winLCA;
        right_result_SIEM = expected_SIEM_win;
        right_result_LCA = expected_LCA_win;
    } else {
        commandSIEM = command_linuxSIEM;
        commandLCA = command_linuxLCA;
        right_result_SIEM = expected_SIEM_lnx;
        right_result_LCA = expected_LCA_lnx;
    }

    prepareDirectories(epOs);


    manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));

    String confJson = data.get("Settings Json");

    manager.SetCustomerConfiguration(confJson);
    endpoint.StopEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
    endpoint.StartEPService(Integer.parseInt(general.get("EP Service Timeout")), epOs);
    endpoint.CompareConfigurationToEPConfiguration(confJson, epOs);

    createLogs(epOs);
    Thread.sleep(schedule_report_timeout);

        boolean res = false;

        if (log_type.equalsIgnoreCase( "SIEM")) {
            res = handleSIEM(commandSIEM);
        } else if (log_type.equalsIgnoreCase("LCA")) {
            res = handleLCA(commandLCA);
        } else {
            org.testng.Assert.fail("Unknown server log_type: " +  log_type);
        }

        if (!res)
            org.testng.Assert.fail("Could not find pattern in Agent.log for: " + log_type + " or number of lines did not match the expected value: ");

    } catch (Exception e) {
        org.testng.Assert.fail("SimulateLFMandVerifyDelivery failed" + "\n" + e.toString());
     }
    }

     private void prepareDirectories(AgentActions.EP_OS os) {
         String script;
         if (os == AgentActions.EP_OS.WINDOWS)
             script = data.get("WinDirscript");
         else
             script = data.get("linuxDirscript"); //TBD
         endpoint.writeAndExecute(script, os);
     }


     private void createLogs(AgentActions.EP_OS os) {
         String script;
         if (os == AgentActions.EP_OS.WINDOWS)
                script = data.get("WIncreateLogs");
         else
                script = data.get("LinuxcreateLogs"); //TBD

         endpoint.writeAndExecute(script, os);
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
            res = manager.numLinesinFile(scp_path + zipFiles.elementAt(i));
            JLog.logger.info("res: " + res);
            if ((null != res) && (res.contains(right_result_SIEM) || res.contains((expected_LCA_lnx))))
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

    public boolean handleLCA(String command) {
        // Here we have 2 log files sent to LNE
        boolean result = false;

        String patt = ".log-tag.log was sent successfully";
        String res = endpoint.findPattern(command, patt);
        JLog.logger.info("res: " + res);
        if (res == null)
            return false;
        Vector<String> logFiles = extractFileNames(res, "log-src.", ".log-tag.log");
        for (int i = 0; i < logFiles.size(); i++) {
            res = manager.numLinesinFile(scp_path + logFiles.elementAt(i));
            JLog.logger.info("res: " + res);
            if ((null != res) && (res.contains(right_result_LCA)))
                result = true;
            else {
                result = false;
                break;
            }
        }
        if (!result)
            return result;
        // now searching for .txt.tag-log
        command = "type C:\\ProgramData\\Trustwave\\NEPAgent\\logs\\NewAgent_0.log | find /n \".txt-tag.log was sent\"";
        patt = ".txt-tag.log was sent successfully";
        res = endpoint.findPattern(command, patt);
        JLog.logger.info("res: " + res);
        if (res == null)
            return false;
        logFiles = extractFileNames(res, "txt-src.", ".txt-tag.log");
        for (int i = 0; i < logFiles.size(); i++) {
            res = manager.numLinesinFile(scp_path + logFiles.elementAt(i));
            JLog.logger.info("res: " + res);
            if ((null != res) && (res.contains(right_result_LCA)))
                result = true;
            else {
                result = true;
                break;
            }
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
