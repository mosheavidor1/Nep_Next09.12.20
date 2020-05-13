package Tests.NEP;

import Actions.NepActions;
import Tests.GenericTest;
import Utils.EventsLog.LogEntry;
import Utils.PropertiesFile.PropertiesFile;
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
    public void SendLogsAndVerify () throws Exception {

        action.ChangeReportInterval(data.get("Report Interval"));

        LogEntry entry = new LogEntry(data.get("Event Type"),data.get("Event ID"),data.get("Event Log"),data.get("Event Source"),data.get("Event Description"),data.get("Add time stamp to description"));
        action.WriteEvent(entry);

        action.LaunchApplication(data.get("Browser"));
        action.SetApplicationUrl(PropertiesFile.getCurrentClusterLink());

        action.Login(PropertiesFile.getUserName(), PropertiesFile.getPassword());
        action.GotoEventExplorer(PropertiesFile.getCurrentClusterLink());

        action.SelectCustomer(data.get("Customer"));
        action.VerifyMessageExistsInPortal(entry,Integer.parseInt(data.get("Log To Appear Timeout")));

    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        action.CloseApplication();

    }

}
