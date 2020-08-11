package Tests.Environments;

import Actions.AgentActions;
import Actions.BrowserActions;
import Tests.GenericTest;
import Tests.RecordedTest;
import Utils.Data.Endpoint;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.io.IOException;


public class ClientLogToPortalTest extends RecordedTest {
    private BrowserActions browser;
    private AgentActions endpoint;

    @Factory(dataProvider = "getData")
    public ClientLogToPortalTest(Object dataToSet) {
        super(dataToSet);
        browser = new BrowserActions();

    }

    @Test( groups = { "logs" } )
    public void SendLogsAndVerify () {
        JLog.logger.debug("Test Started. log entry to appear at portal timeout: " + data.get("Log To Appear Timeout"));
        endpoint = new AgentActions(data.get("EP_HostName_1"),data.get("EP_UserName_1"), data.get("EP_Password_1"));
        endpoint.ChangeReportInterval(data.get("Report Interval") , 120);

        LogEntry entry = new LogEntry(data.get("Event Type"),data.get("Event ID"),data.get("Event Log"),data.get("Event Source"),data.get("Event Description"),data.get("Add time stamp to description"));
        endpoint.WriteEvent(entry);

        browser.LaunchApplication(general.get("Browser"));
        browser.SetApplicationUrl(data.get("Fusion Link"));

        browser.Login(data.get("Fusion User Name"), data.get("Fusion Password"));
        browser.GotoEventExplorer(data.get("Fusion Link"));

        browser.SelectCustomer(data.get("Customer"));
        browser.VerifyMessageExistsInPortal(entry,Integer.parseInt(data.get("Log To Appear Timeout")));
        JLog.logger.debug("Test ended");

    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        if (browser != null) {
            browser.CloseApplication();
        }
        if (endpoint != null) {
            endpoint.Close();
        }
    }

}
