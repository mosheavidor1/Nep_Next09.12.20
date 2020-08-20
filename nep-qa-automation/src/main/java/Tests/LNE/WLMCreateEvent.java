package Tests.LNE;

import Actions.AgentActions;
import Actions.BrowserActions;
import Actions.LNEActions;
import Actions.BrowserActions.*;
import Actions.ManagerActions;
import Tests.GenericTest;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.Remote.SSHManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static Actions.ManagerActions.execCmd;


public class WLMCreateEvent extends GenericTest {

    private LNEActions manager;
    private AgentActions endpoint;
    private BrowserActions action;
    private SSHManager connection;
    public static final int connection_port =22;
    public static final String scp_path = "/work/services/siem/var/siem/data/nep/";
    public static final String syslog_path = "/work/services/siem/var/log/";
    // result depends on filter settings in relevant config.json and on number of created events per config.json
    // currently, number of lines detected by ep is 2.
    String right_result = "4";
    static final int schedule_report_timeout = 62000; //ms
    @Factory(dataProvider = "getData")
    public WLMCreateEvent(Object dataToSet) {
        super(dataToSet);
    }

    @Test()
    public void WLMCreateEventAndVerify()  {
        try {
            String ret;

            JLog.logger.info("Opening...");
            String log_type = data.get("Log_Type");
            JLog.logger.info("log_type: " + log_type);

            manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            endpoint = new AgentActions(data.get("EP_HostName_1"),data.get("EP_UserName_1"), data.get("EP_Password_1"));
            String confJson =data.get("Settings Json");
            manager.SetCustomerConfiguration(confJson);
            endpoint.StopEPService(Integer.parseInt(general.get("EP Service Timeout")), AgentActions.EP_OS.WINDOWS);
            endpoint.StartEPService(Integer.parseInt(general.get("EP Service Timeout")), AgentActions.EP_OS.WINDOWS);
            endpoint.CompareConfigurationToEPConfiguration(confJson, AgentActions.EP_OS.WINDOWS);

            connection = new SSHManager(data.get("EP_UserName_1"),data.get("EP_Password_1"),data.get("EP_HostName_1"), connection_port );

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
            connection.Close();
            if (!res)
                org.testng.Assert.fail("Could not find pattern in Agent.log for: " + log_type + " or number of lines did not match the expected value: " +right_result);
        }
        catch (Exception e) {
                org.testng.Assert.fail("WLMCreateEventAndVerify ." + "\n" + e.toString());
            }

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

        res = manager.numLinesinFile(scp_path + zipFileMane, null);
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
        res = manager.numLinesinFile(scp_path + txtFileMane, null);
        JLog.logger.info("res: " + res);
        if ((null != res) && (res.contains(right_result)))
            return true;
        return false;
    }

    public boolean handleLCA_SYSLOG() {
        String command = "type C:\\ProgramData\\Trustwave\\NEPAgent\\logs\\NewAgent_0.log | find /n \"SecureSyslogCollector: sent " + right_result + " events\"";
        String patt = "SecureSyslogCollector: sent " + right_result + " events";
        String res = endpoint.findPattern(command, patt);
        if (res == null)
            return false;

        String txtFileMane = syslog_path + data.get("EP_HostName_1") + "/user.log";
        JLog.logger.info("txt file Name: " + txtFileMane);
        res = manager.numLinesinFile(txtFileMane, null);
        JLog.logger.info("res: " + res);
        if ((null != res) && (res.contains(right_result)))
            return true;
        return false;
    }

     public void createEvents_old() {
        String[] events = {"eventcreate  /Id 111  /D \"WLM test log included - application error\" /T error /so wlm_test_source /L Application",
        "eventcreate  /Id 333  /D \"WLM test log included - application warning\" /T warning /so wlm_test_source /L Application",
        "eventcreate  /Id 110  /D \"WLM test log included - application error\" /T error /so wlm_test_source /L Application",
        "eventcreate  /Id 111  /D \"WLM test log included - application error\" /T error /so wlm_test_source2 /L Application"};
 /*       "eventcreate  /Id 999  /D \"WLM test log included - setup error\" /T error /L setup /so SetupTestSource",
        "eventcreate  /Id 1000  /D \"WLM test log included - setup error\" /T error /L \"setup\" /so SetupTestSource",
        "eventcreate  /Id 999  /D \"WLM test log excluded - setup warning\" /T warning /L \"setup\" /so SetupTestSource",
        "eventcreate  /Id 123  /D \"WLM test log excluded - setup error\" /T error /L \"setup\" /so SetupTestSource1"};*/
        for (int i = 0; i < events.length; i++) {
            String evt_res = connection.Execute(events[i]);
            if (!evt_res.contains("SUCCESS: An event of type"))
                org.testng.Assert.fail("Could no add log event.\nAdd event result: " + evt_res + "\nCommand sent: " + events[i]);
        }

    }

    public void createEvents() {
        String createEvents_bat = data.get("WLMcreateEvent");
        endpoint.writeAndExecute(createEvents_bat, AgentActions.EP_OS.WINDOWS);
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
