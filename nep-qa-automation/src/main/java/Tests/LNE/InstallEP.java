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

    private AgentActions agent;

    @Factory(dataProvider = "getData")
    public InstallEP(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "VerifyInstallation" } )
    public void InstallEndPoint ()  {
        JLog.logger.info("Opening...");

        agent = new AgentActions(data.get("EP_HostName_1"),data.get("EP_UserName_1"), data.get("EP_Password_1"));
        agent.UnInstallEndPoint(Integer.parseInt(general.get("EP Installation timeout")));
        agent.CopyInstaller();
        agent.AppendToHostsFile();
        agent.InstallEndPoint(Integer.parseInt(general.get("EP Installation timeout")));
        agent.StopEPService(Integer.parseInt(general.get("EP Service Timeout")));
        agent.AddCaCertificate();
        agent.StartEPService(Integer.parseInt(general.get("EP Service Timeout")));
        agent.CheckEndPointActiveByDbJson(Integer.parseInt(general.get("From EP service start until logs show EP active timeout")));

    }

    @AfterMethod
    public void Close(){
        if (agent!=null) {
            JLog.logger.info("Closing");
            agent.Close();
        }
    }

}
