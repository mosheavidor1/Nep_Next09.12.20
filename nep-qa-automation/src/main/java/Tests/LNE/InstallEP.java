package Tests.LNE;

import Actions.AgentActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.io.IOException;


public class InstallEP extends GenericTest {

    private AgentActions endpoint;

    @Factory(dataProvider = "getData")
    public InstallEP(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "VerifyInstallation" } )
    public void InstallEndPoint ()  {

        endpoint = new AgentActions(data.get("EP_HostName_1"),data.get("EP_UserName_1"), data.get("EP_Password_1"));
        AgentActions.EP_OS epOs = data.get("EP_Type_1").contains("win") ? AgentActions.EP_OS.WINDOWS : AgentActions.EP_OS.LINUX;
        endpoint.InstallEPIncludingRequisites(epOs, Integer.parseInt(general.get("EP Installation timeout")), Integer.parseInt(general.get("EP Service Timeout")), Integer.parseInt(general.get("From EP service start until logs show EP active timeout") ));

    }

    @AfterMethod
    public void Close(){
        if (endpoint!=null) {
            JLog.logger.info("Closing");
            endpoint.Close();
        }
    }

}
