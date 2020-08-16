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
    static final int schedule_report_timeout = 65000; //ms
    String right_result = "3";
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

    endpoint = new AgentActions(data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
    AgentActions.EP_OS epOs = data.get("EP_Type_1").contains("win") ? AgentActions.EP_OS.WINDOWS : AgentActions.EP_OS.LINUX;

    prepareWinDirectories(epOs);


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
            res = handleSIEM();
        } else if (log_type.equalsIgnoreCase("LCA")) {
            res = handleLCA();
        } else {
            org.testng.Assert.fail("Unknown server log_type: " +  log_type);
        }

        if (!res)
            org.testng.Assert.fail("Could not find pattern in Agent.log for: " + log_type + " or number of lines did not match the expected value: " +right_result);

    } catch (Exception e) {
        org.testng.Assert.fail("SimulateLFMandVerifyDelivery ." + "\n" + e.toString());
     }
    }

     private void prepareWinDirectories(AgentActions.EP_OS os) {
         String script;
         if (os == AgentActions.EP_OS.WINDOWS)
             script = data.get("script");
         else
             script = data.get("linux_script"); //TBD
         endpoint.writeAndExecute(script, os);
     }


     private void createLogs(AgentActions.EP_OS os) {
         String script;
         if (os == AgentActions.EP_OS.WINDOWS)
                script = data.get("createLogs");
         else
                script = data.get("LinuxcreateLogs"); //TBD

         endpoint.writeAndExecute(script, os);
     }

    public boolean handleSIEM() {
        // Here we have 2 zip files sent to LNE
        boolean result = false;
        String command = "type C:\\ProgramData\\Trustwave\\NEPAgent\\logs\\NewAgent_0.log | find /n \".zip was sent successfully\"";
        String patt = ".zip was sent successfully";
        String res = endpoint.findPattern(command, patt);
        JLog.logger.info("res: " + res);
        if (res == null)
            return false;
        Vector<String> zipFiles = extractFileNames(res, "dla_", ".zip");

        for (int i = 0; i < zipFiles.size(); i++) {
            res = manager.numLinesinFile(scp_path + zipFiles.elementAt(i));
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

    public boolean handleLCA() {
        // Here we have 2 log files sent to LNE
        boolean result = false;
        String command = "type C:\\ProgramData\\Trustwave\\NEPAgent\\logs\\NewAgent_0.log | find /n \".log-tag.log was sent\"";
        String patt = ".log-tag.log was sent successfully";
        String res = endpoint.findPattern(command, patt);
        JLog.logger.info("res: " + res);
        if (res == null)
            return false;
        Vector<String> logFiles = extractFileNames(res, "log-src.", ".log-tag.log");
        for (int i = 0; i < logFiles.size(); i++) {
            res = manager.numLinesinFile(scp_path + logFiles.elementAt(i));
            JLog.logger.info("res: " + res);
            if ((null != res) && (res.contains(right_result)))
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
            if ((null != res) && (res.contains(right_result)))
                result = true;
            else {
                result = false;
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
