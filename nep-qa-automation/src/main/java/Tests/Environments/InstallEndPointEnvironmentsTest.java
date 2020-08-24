package Tests.Environments;

import Actions.AgentActions;
import Actions.BrowserActions;
import Actions.ManagerActions;
import Tests.GenericTest;
import Tests.RecordedTest;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class InstallEndPointEnvironmentsTest extends RecordedTest {
    private AgentActions endpoint;

    @Factory(dataProvider = "getData")
    public InstallEndPointEnvironmentsTest(Object dataToSet) {
        super(dataToSet);


    }

    @Test(groups = { "install" } )
    public void InstallTest () {
        endpoint = new AgentActions(data.get("EP_HostName_1"),data.get("EP_UserName_1"), data.get("EP_Password_1"));
        AgentActions.EP_OS epOs = data.get("EP_Type_1").contains("win") ? AgentActions.EP_OS.WINDOWS : AgentActions.EP_OS.LINUX;
        endpoint.UninstallEndpoint(epOs,Integer.parseInt(data.get("Installation timeout")));
        endpoint.CopyInstaller(epOs);
        endpoint.InstallEndPointWithoutAdditions(epOs, Integer.parseInt(data.get("Installation timeout")));
        endpoint.CheckEndPointActiveByDbJson(Integer.parseInt(data.get("From service start until logs show active timeout")), epOs);

    }

    @AfterMethod
    public void Close(){
        if (endpoint!=null) {
            endpoint.Close();
        }
    }

}
