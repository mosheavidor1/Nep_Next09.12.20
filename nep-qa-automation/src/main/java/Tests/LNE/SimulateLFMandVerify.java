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


public class SimulateLFMandVerify extends GenericTest {

    private LNEActions manager;
    private AgentActions endpoint;
    String scp_path = "/work/services/siem/var/siem/data/nep/";
    static final int schedule_report_timeout = 60000; //ms
    String right_result = "2";
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
        String command = "type C:\\ProgramData\\Trustwave\\NEPAgent\\logs\\NewAgent_0.log | find /n \".zip was sent successfully\"";
        String patt = ".zip was sent successfully";
        String res = endpoint.findPattern(command, patt);
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

        res = manager.numLinesinFile(scp_path + zipFileMane);
        JLog.logger.info("res: " + res);
        if ((null != res) && (res.contains(right_result)))
            return true;
        return false;
    }

    public boolean handleLCA() {
        String command = "type C:\\ProgramData\\Trustwave\\NEPAgent\\logs\\NewAgent_0.log | find /n \".txt was sent successfully\"";
        String patt = ".txt was sent successfully";
        String res = endpoint.findPattern(command, patt);
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
        res = manager.numLinesinFile(scp_path + txtFileMane);
        JLog.logger.info("res: " + res);
        if ((null != res) && (res.contains(right_result)))
            return true;
        return false;
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
