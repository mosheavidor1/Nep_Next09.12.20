package Tests.NEP;

import Actions.NepActions;
import Tests.GenericTest;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class ClientLogToPortalTest extends GenericTest {
    private NepActions action;

    @Factory(dataProvider = "getData")
    public ClientLogToPortalTest(Object dataToSet) {
        super(dataToSet);
        action = new NepActions();

    }

    @Test( groups = { "logs" } )
    public void SendLogsAndVerify () {

        JLog.logger.debug("Before Report Interval");

        action.ChangeReportInterval(data.get("Report Interval"));
        JLog.logger.debug("After Report Interval");

        LogEntry entry = new LogEntry(data.get("Event Type"),data.get("Event ID"),data.get("Event Log"),data.get("Event Source"),data.get("Event Description"),data.get("Add time stamp to description"));
        JLog.logger.debug("After new log entry");

        action.WriteEvent(entry);
        JLog.logger.debug("After write event");


        action.LaunchApplication(general.get("Browser"));
        JLog.logger.debug("After launch  application: " + general.get("Browser"));

        action.SetApplicationUrl(data.get("Fusion Link"));
        JLog.logger.debug("After set application url: " + data.get("Fusion Link"));

        action.Login(data.get("Fusion User Name"), data.get("Fusion Password"));
        action.GotoEventExplorer(data.get("Fusion Link"));

        action.SelectCustomer(data.get("Customer"));
        action.VerifyMessageExistsInPortal(entry,Integer.parseInt(data.get("Log To Appear Timeout")));

    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        action.CloseApplication();

    }

}
