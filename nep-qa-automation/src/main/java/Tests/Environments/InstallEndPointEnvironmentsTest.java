package Tests.Environments;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Actions.LNEActions.CentcomMethods;
import Tests.RecordedTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class InstallEndPointEnvironmentsTest extends RecordedTest {
	
	private LNEActions lneActions;
    private BaseAgentActions agent;
    private String customerId;

    @Factory(dataProvider = "getData")
    public InstallEndPointEnvironmentsTest(Object dataToSet) {
        super(dataToSet);
        lneActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
        customerId = general.get("Customer Id");
    }

    @Test(groups = { "install" } )
    public void InstallTest () {
    	
    	JLog.logger.info("Starting InstallEndPointEnvironmentsTest::InstallTest ...");
    	
        agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        agent.uninstallEndpoint(Integer.parseInt(data.get("Installation timeout")));
        agent.copyInstaller();
        agent.installEndpoint(Integer.parseInt(data.get("Installation timeout")));
        lneActions.verifyCallToCentcom(CentcomMethods.REGISTER, customerId, agent.getEpName());

    }

    @AfterMethod
    public void Close(){
        if (agent!=null) {
            agent.close();
        }
        if(lneActions!=null){
            lneActions.Close();
        }
    }

}
