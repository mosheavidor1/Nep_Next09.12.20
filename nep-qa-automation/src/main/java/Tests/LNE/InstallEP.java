package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.DbActions;
import Actions.LNEActions;
import Actions.LNEActions.CentcomMethods;
import DataModel.UpdateEpDetails;
import Tests.GenericTest;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;



public class InstallEP extends GenericTest {

    private BaseAgentActions agentActions;
    private static final LNEActions lneActions = GlobalTools.getLneActions();
    private String customerId;

    @Factory(dataProvider = "getData")
    public InstallEP(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }

    @Test(groups = { "InstallEP" } )
    public void InstallEndPoint ()  {

    	JLog.logger.info("Starting InstallEndPoint test ...");

        String timestamp = DbActions.getCurrentDbTimeStamp();

        agentActions = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        agentActions.reinstallEndpoint(Integer.parseInt(getGeneralData().get("EP Installation timeout")), Integer.parseInt(getGeneralData().get("EP Service Timeout")));
        
        try {
        	Thread.sleep(2000);//Sleep 2 seconds
        }
        catch(InterruptedException ex) {
        	JLog.logger.info("Failed to sleep");
        }
        
        String epName = agentActions.getEpName();
        String epIP =agentActions.getEpIp();


        String timeout = getGeneralData().get("Verify CentCom Call Timeout");
        UpdateEpDetails json = new UpdateEpDetails(customerId, epName, epIP);
        DbActions.verifyCallToCentcom(CentcomMethods.REGISTER,json,timestamp,Integer.parseInt(timeout));

    }

    @AfterMethod
    public void Close(){
        if (agentActions!=null) {
            agentActions.close();
        }
    }

}
