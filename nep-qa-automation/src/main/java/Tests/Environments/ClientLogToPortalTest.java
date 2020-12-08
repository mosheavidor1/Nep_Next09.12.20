package Tests.Environments;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.BrowserActions;
import Tests.RecordedTest;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;



public class ClientLogToPortalTest extends RecordedTest {
    private BrowserActions browser;
    private BaseAgentActions agent;
    public static int longWaitEE = 0;

    @Factory(dataProvider = "getData")
    public ClientLogToPortalTest(Object dataToSet) {
        super(dataToSet);
        browser = new BrowserActions();

    }

    @Test( groups = { "logs" } )
    public void SendLogsAndVerify () {

        try {
            if (! data.get("EP_Type_1").equalsIgnoreCase("win") && !data.get("EP_Type_1").equalsIgnoreCase("msi")) {
                JLog.logger.info("ClientLogToPortalTest - This test should not run for {} OS, skipping test", data.get("EP_Type_1"));
                return;
            }

            JLog.logger.info("ClientLogToPortalTest - Starting test for OS: " + data.get("EP_Type_1"));

            JLog.logger.debug("Test Started. log entry to appear at portal timeout: " + data.get("Log To Appear Timeout"));
            agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
            agent.changeReportInterval(data.get("Report Interval"), 120);

            LogEntry entry = new LogEntry(data.get("Event Type"), data.get("Event ID"), data.get("Event Log"), data.get("Event Source"), data.get("Event Description"), data.get("Add time stamp to description"));
            agent.writeEvent(entry);
            String hostname = agent.getEpNameAndDomain();

            browser.LaunchApplication(getGeneralData().get("Browser"));
            browser.SetApplicationUrl(data.get("Fusion Link"));

            browser.Login(data.get("Fusion User Name"), data.get("Fusion Password"));
            browser.GotoEventExplorer(data.get("Fusion Link"));

            browser.SelectCustomer(data.get("Customer"));
            //if long wait for event explore is assigned use it, else use Excel default value
            browser.VerifyMessageExistsInPortal(entry, hostname, (longWaitEE==0)?Integer.parseInt(data.get("Log To Appear Timeout")) : longWaitEE*60);
            JLog.logger.debug("Test ended");
        }
        catch (Exception e){
            org.testng.Assert.fail("Could not send logs and verify: " + "\n" + e.toString());

        }

    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        if (browser != null) {
            browser.CloseApplication();
        }
        if (agent != null) {
            agent.close();
        }
    }

}
