package Tests.Environments;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.BrowserActions;
import Actions.DsMgmtActions;
import Actions.SimulatedAgentActions;
import Tests.RecordedTest;
import Utils.ConfigHandling;
import Utils.Logs.JLog;

import java.time.Instant;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class InitAndCleanupPortal extends RecordedTest {

	private BaseAgentActions agent;
	private BrowserActions browserActions;
    private String customerId;
    public static String epNameForConnectivityTest = "ChiefEp";
    public static Instant whenInit = Instant.now();
    public static SimulatedAgentActions simulatedAgentForConnectivityTest;
    
    

    @Factory(dataProvider = "getData")
    public InitAndCleanupPortal(Object dataToSet) {
        super(dataToSet);
        browserActions = new BrowserActions();
        customerId = getGeneralData().get("Customer Id");
    }
    
    
    

    /**
     * Need to delete existing endpoints from Centcom UI + DS side + check update until we get uninstall action (then entities are actually deleted for DB)
     * This is required to be sure that the tests really test a new installation flow 
     * The cleanup is tolerant for errors since we should succeed even if agent is missing from UI/DS-Mgmt 
     */
    @Test(groups = { "InitAndCleanup" }, priority=11)
    public void cleanupAgents()  {

        JLog.logger.info("Starting Cleanup agents");
        
        agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        String epName = agent.getEpName();
        if (epName == null || epName.isEmpty()) {
        	JLog.logger.warn("EP name is null, skipping cleanup!");
        	return;
        }
        
        JLog.logger.info("Going to send delete action to Centcom for agent on hostanem {}", epName);
        boolean deletedInUI = deleteEndpointFromCentCom(epName);
        
        if (deletedInUI) {
        	JLog.logger.info("Agent was found and delete action was done in Centcom, going to wait until service is uninstalled from agent");
        	verifyServiceUninstalledOrForceUninstall(Integer.parseInt(getGeneralData().get("EP Installation timeout")));
        	return;
        }
       
       	JLog.logger.info("Agent wasn't found in UI, going to delete from DS Mgmt (by simulated agent) if exists in DB");
        
        String uuid = getDbConnector().getUuidByName(epName, customerId);
        
        if (uuid == null) {
        	JLog.logger.info("Endpoint {} was not found in DB, going to uninsatll if service is running on agent.", epName);
        	verifyServiceUninstalledOrForceUninstall(0);  
        	return;
        }
        
        JLog.logger.info("Agent exists, going to delete it from DS Mgmt.");
        DsMgmtActions.deleteAndVerifyResponse(customerId, epName);        
        JLog.logger.info("Done. Going to wait until service is uninstalled from agent");
        verifyServiceUninstalledOrForceUninstall(Integer.parseInt(getGeneralData().get("EP Installation timeout")));    	
        JLog.logger.info("Going to set default configuration for this customer.");
        
        DsMgmtActions.setCustomerConfig(getGeneralData().get("Customer Id"), ConfigHandling.getDefaultConfiguration());
        JLog.logger.info("Done");       
    }
    
    private void verifyServiceUninstalledOrForceUninstall(int timeout) {
    	boolean deleted = agent.checkDeleted(timeout);
    	if (deleted) {
    		JLog.logger.info("Done. Service is not running on agent");
    		return;
    	}
		JLog.logger.info("Service is still running, going to force uninstall");
		agent.uninstallEndpoint(Integer.parseInt(getGeneralData().get("EP Installation timeout")));
		deleted = agent.checkDeleted(Integer.parseInt(getGeneralData().get("EP Installation timeout")));
    	if (!deleted) {
    		 org.testng.Assert.fail("Endpoint deleted verification failed, the endpoint service still running. Please check!");
    		 return;
    	}
    	JLog.logger.info("Done.");
    }
     
    /**
     * Delete the endpoint with name 'hostname' from Centcom UI
     * Returns - true if an endpoint with this name was found, else otherwise 
     */
    private boolean deleteEndpointFromCentCom (String hostname) {
        try {

	        agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
	        
	
	        browserActions.LaunchApplication(getGeneralData().get("Browser"));
	        browserActions.SetApplicationUrl(getGeneralData().get("Fusion Link"));
	
	        browserActions.Login(getGeneralData().get("Fusion User Name"), getGeneralData().get("Fusion Password"));
	
	        browserActions.GotoCentComSearch(getGeneralData().get("Fusion Link"));
	        browserActions.GotoCentComEndpointsPage(data.get("Customer"));
	
	        return browserActions.DeleteEpFromCentCom(hostname);
        }
        catch (Exception e){
            org.testng.Assert.fail("Could not delete endpoint from CentCom: " + "\n" + e.toString(), e);
            return false;
        }
        

    }
   
    
    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        if (browserActions != null) {
        	browserActions.CloseApplication();
        }
        if (agent!=null) {
        	agent.close();
        }


    }

}
