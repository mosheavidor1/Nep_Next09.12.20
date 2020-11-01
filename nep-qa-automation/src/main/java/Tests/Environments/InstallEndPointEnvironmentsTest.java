package Tests.Environments;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
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

    /**
     * Install the service on agent. 
     * Should run after InitAndCleanupPortal which make sure that service on agent is actually uninstalled 
     */
    @Test(groups = { "InstallEP" } )
    public void InstallTest () {
    	
    	JLog.logger.info("Starting InstallEndPointEnvironmentsTest::InstallTest for {} agent", data.get("EP_Type_1"));
    	
        agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        agent.copyInstallerAndInstall(Integer.parseInt(getGeneralData().get("EP Installation timeout")), Integer.parseInt(getGeneralData().get("EP Service Timeout")));
        
        JLog.logger.info("Finished InstallEndPointEnvironmentsTest::InstallTest");
    }

    @AfterMethod
    public void Close(){
        if (agent!=null) {
            agent.close();
        }
    }

}
