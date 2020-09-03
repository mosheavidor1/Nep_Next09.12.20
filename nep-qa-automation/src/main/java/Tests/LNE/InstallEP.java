package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;



public class InstallEP extends GenericTest {

    private BaseAgentActions agentActions;

    @Factory(dataProvider = "getData")
    public InstallEP(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "VerifyInstallation" } )
    public void InstallEndPoint ()  {

        agentActions = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        agentActions.installEPIncludingRequisites(Integer.parseInt(general.get("EP Installation timeout")), Integer.parseInt(general.get("EP Service Timeout")), Integer.parseInt(general.get("From EP service start until logs show EP active timeout") ));

    }

    @AfterMethod
    public void Close(){
        if (agentActions!=null) {
            JLog.logger.info("Closing");
            agentActions.close();
        }
    }

}
