package Tests.Environments;

import Actions.BrowserActions;
import Tests.GenericTest;
import Tests.RecordedTest;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.io.IOException;


public class ClientLogToPortalTest extends RecordedTest {
    private BrowserActions action;

    @Factory(dataProvider = "getData")
    public ClientLogToPortalTest(Object dataToSet) {
        super(dataToSet);
        action = new BrowserActions();

    }

    @Test( groups = { "logs" } )
    public void SendLogsAndVerify () {
        JLog.logger.debug("Test Started. log entry to appear at portal timeout: " + data.get("Log To Appear Timeout"));
        action.ChangeReportInterval(data.get("Report Interval"));

        LogEntry entry = new LogEntry(data.get("Event Type"),data.get("Event ID"),data.get("Event Log"),data.get("Event Source"),data.get("Event Description"),data.get("Add time stamp to description"));
        action.WriteEvent(entry);

        action.LaunchApplication(general.get("Browser"));
        action.SetApplicationUrl(data.get("Fusion Link"));

        action.Login(data.get("Fusion User Name"), data.get("Fusion Password"));
        action.GotoEventExplorer(data.get("Fusion Link"));

        action.SelectCustomer(data.get("Customer"));
        action.VerifyMessageExistsInPortal(entry,Integer.parseInt(data.get("Log To Appear Timeout")));
        JLog.logger.debug("Test ended");

    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        action.CloseApplication();

    }

}
