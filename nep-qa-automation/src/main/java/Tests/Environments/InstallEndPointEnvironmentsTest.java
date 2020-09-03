package Tests.Environments;

import Actions.AgentActionsFactory;
import Actions.AgentActionsInterface;
import Actions.BaseAgentActions;
import Actions.BrowserActions;
import Actions.ManagerActions;
import Tests.GenericTest;
import Tests.RecordedTest;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class InstallEndPointEnvironmentsTest extends RecordedTest {
    private BaseAgentActions agent;

    @Factory(dataProvider = "getData")
    public InstallEndPointEnvironmentsTest(Object dataToSet) {
        super(dataToSet);


    }

    @Test(groups = { "install" } )
    public void InstallTest () {
        agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        agent.uninstallEndpoint(Integer.parseInt(data.get("Installation timeout")));
        agent.copyInstaller();
        agent.installEndpointWithoutAdditions(Integer.parseInt(data.get("Installation timeout")));
        agent.checkEndPointActiveByDbJson(Integer.parseInt(data.get("From service start until logs show active timeout")));

    }

    @AfterMethod
    public void Close(){
        if (agent!=null) {
            agent.close();
        }
    }

}
