package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class VerifyConfiguration extends GenericTest {

	private BaseAgentActions agent;

    @Factory(dataProvider = "getData")
    public VerifyConfiguration(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "VerifyConfiguration" } )
    public void VerifyEndpointConfiguration()  {

        JLog.logger.info("Opening...");

        agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        agent.stopEPService(Integer.parseInt(general.get("EP Service Timeout")));
        agent.startEPService(Integer.parseInt(general.get("EP Service Timeout")));
        agent.compareConfigurationToEPConfiguration(true);

    }

    @AfterMethod
    public void Close(){
        JLog.logger.info("Closing...");
        if (agent!=null) {
            agent.close();
        }
    }


}
