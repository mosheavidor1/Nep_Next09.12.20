package Tests.Environments;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.BrowserActions;
import Tests.RecordedTest;
import Utils.Logs.JLog;
import org.testng.annotations.*;


public class VerifyEndPointOkAtPortalTest extends RecordedTest {
    private BrowserActions action;
    private BaseAgentActions agent1;
    private BaseAgentActions agent2;

    @Factory(dataProvider = "getData")
    public VerifyEndPointOkAtPortalTest(Object dataToSet) {
        super(dataToSet);
        action = new BrowserActions();

    }

    @Test( groups = { "verify" } )
    public void VerifyEndPointStatusAtPortalTest () {
        try {

        JLog.logger.info("Starting VerifyEndPointOkAtPortalTest...");

        agent1 = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        String hostname1 = agent1.getEpName();

        agent2 = AgentActionsFactory.getAgentActions(data.get("EP_Type_2"), data.get("EP_HostName_2"), data.get("EP_UserName_2"), data.get("EP_Password_2"));
        String hostname2 = agent2.getEpName();


            action.LaunchApplication(getGeneralData().get("Browser"));
        action.SetApplicationUrl(getGeneralData().get("Fusion Link"));

        action.Login(getGeneralData().get("Fusion User Name"), getGeneralData().get("Fusion Password"));

        action.GotoCentComSearch(getGeneralData().get("Fusion Link"));
        action.GotoCentComEndpointsPage(data.get("Customer"));

        action.CheckEndPointOkInCentCom(hostname1);
        action.CheckEndPointOkInCentCom(hostname2);
        }
        catch (Exception e){
            org.testng.Assert.fail("Could not verify endpoint status at portal: " + "\n" + e.toString());

        }

    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        if (action != null) {
            action.CloseApplication();
        }
        if (agent1!=null) {
            agent1.close();
        }

        if (agent2!=null) {
            agent2.close();
        }

    }

}
