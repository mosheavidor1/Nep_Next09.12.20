package Tests.LNE;

import Actions.DsMgmtActions;
import Actions.SimulatedAgentActions;
import Actions.LNEActions.CentcomMethods;
import Tests.GenericTest;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

//https://jira.trustwave.com/browse/NEP-1430
public class RenameEndpoint extends GenericTest {

    private String customerId;
    
    private static final String simulatedAgentIp1 = "1.2.3.4";
    private static final String simulatedAgentOs = "Windows 10";
    private static final String simulatedAgentMac1 = "84-7B-EB-21-22";
    private static final String simulatedAgentName = "endpointName";
    private static final String simulatedAgentNewName = "endpointNewName";
    
    SimulatedAgentActions simulatedAgent = null;
    SimulatedAgentActions simulatedAgent2 = null;
    
    
    @Factory(dataProvider = "getData")
    public RenameEndpoint(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }

    @Test(groups = { "RenameEndpoint" })
    public void renameEndpoint()  {
    	try {
            JLog.logger.info("Starting RenameEndpoint test");
            
            simulatedAgent = new SimulatedAgentActions(getGeneralData().get("DS Name"), customerId);
            simulatedAgent.register(customerId, simulatedAgentIp1, simulatedAgentName, 
            		simulatedAgentMac1, simulatedAgentOs);
            
            simulatedAgent.sendCheckUpdatesAndGetResponse(simulatedAgentNewName, GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
            
            GlobalTools.getLneActions().verifyCallToCentcom(CentcomMethods.RENAME_ENDPOINT, customerId, simulatedAgentName, simulatedAgentNewName);
            
	        
	        JLog.logger.info("RenameEndpoint completed successfully.");

        } catch (Exception e) {
            org.testng.Assert.fail("RenameEndpoint failed:", e);
        }
        
    }
    
    

    @AfterMethod
    public void Close(){
        
    	if (simulatedAgent != null) {
    		DsMgmtActions.deleteWithoutVerify(customerId, simulatedAgentNewName);
    		
    		simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
           
    	}
        
    }


}
