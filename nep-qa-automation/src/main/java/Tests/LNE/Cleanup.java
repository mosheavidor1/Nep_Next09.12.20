package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class Cleanup extends GenericTest {

	private BaseAgentActions agentActions;
    private LNEActions lennyActions;
    private String customerId;
    
    
    private SimulatedAgentActions simulatedAgent;
    

    @Factory(dataProvider = "getData")
    public Cleanup(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
    }

    /**
     * Need to delete existing endpoints from DS side + check update until we get uninstall action (then entities are actually deleted for DB)
     * This is required to be sure that the tests really test a new installation flow even if Lenny was just upgraded by update services job
     * The cleanup is tolerant for errors since we should succeed even if Lenny was re-deployed and we have no leftovers from previous runs
     */
    @Test(groups = { "Cleanup" })
    public void cleanup()  {

        JLog.logger.info("Starting Cleanup...");
        
        lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
        
        agentActions = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        
        JLog.logger.info("Going to check if agent exists.");
        String epName = agentActions.getEpName();
        
        if (epName == null || epName.isEmpty()) {
        	JLog.logger.info("EP name is null, skipping cleanup!");
        	return;
        }
        
        JLog.logger.info("Agent exists, going to delete it from DS Mgmt.");
        lennyActions.deleteWithoutVerify(customerId, epName);
        
        try {
        	Thread.sleep(2000);//Sleep 2 seconds
        }
        catch(InterruptedException ex) {
        	JLog.logger.info("Failed to sleep");
        }
        
        simulatedAgent = new SimulatedAgentActions();
        simulatedAgent.sendCheckUpdatesWithoutVerify(simulatedAgent.getName(),"1.2.0.100", 0, 0, "1.1.1", customerId);
        
    }

    @AfterMethod
    public void Close(){
    	if (agentActions!=null) {
            agentActions.close();
        }
        if(lennyActions!=null){
            lennyActions.Close();
        }
    }

}
