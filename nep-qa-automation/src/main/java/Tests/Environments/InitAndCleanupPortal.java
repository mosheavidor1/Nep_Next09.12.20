package Tests.Environments;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.DsMgmtActions;
import Actions.LNEActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.TestFiles;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class InitAndCleanupPortal extends GenericTest {

	private BaseAgentActions agentActions;
    private String customerId;
    private static boolean initWasDone = false;
    public static String epNameForConnectivityTest = "ChiefEp";
    public static Instant whenInit = Instant.now();
    public static SimulatedAgentActions simulatedAgentForConnectivityTest;
    private SimulatedAgentActions simulatedAgent;
    

    @Factory(dataProvider = "getData")
    public InitAndCleanupPortal(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }
    
    /**
	 * This function copies from Lenny to Manager machine the Root CA and customer certificate.
	 * This preparation is needed so that simulated agent will be able to connect the proxy in order
	 * to send requests to DS
	 * 
	 * In case we run against portal env we assume that the manager already contains the Root CA and certificate 
	 */
    /*
    @Test(groups = { "InitAndCleanup" }, priority=10)//init should run before cleanup, since cleanup alreasy uses the simulated agent
    public void init()  {
    	
    	 if (initWasDone) { //Init should run only once
    		 return;
    	 }
    	
    	 JLog.logger.info("Starting init...");
    	 
    	 prepareCustomerCaAndCertificates();
    	 initWasDone = true;    	 
    	 
    	 JLog.logger.info("Finished init successfully");
    	
    }*/
    
    

    /**
     * Need to delete existing endpoints from DS side + check update until we get uninstall action (then entities are actually deleted for DB)
     * This is required to be sure that the tests really test a new installation flow even if Lenny was just upgraded by update services job
     * The cleanup is tolerant for errors since we should succeed even if Lenny was re-deployed and we have no leftovers from previous runs
     */
    @Test(groups = { "InitAndCleanup" }, priority=11)
    public void cleanup()  {

        JLog.logger.info("Starting Cleanup...");
        
        agentActions = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        
        JLog.logger.info("Going to check if agent exists.");
        String epName = agentActions.getEpName();
        
        if (epName == null || epName.isEmpty()) {
        	JLog.logger.info("EP name is null, skipping cleanup!");
        	return;
        }
        
        simulatedAgent = new SimulatedAgentActions(getGeneralData().get("DS Name"), customerId);
        String uuid = getDbConnector().getUuidByName(epName, customerId);
        
        if (uuid == null) {
        	JLog.logger.info("Endpoint {} was not found in DB, nothing to clean, skipping.", epName);
        	return;
        }
        
        JLog.logger.info("Agent exists, going to delete it from DS Mgmt.");
        DsMgmtActions.deleteWithoutVerify(customerId, epName);
        
        try {
        	Thread.sleep(2000);//Sleep 2 seconds
        }
        catch(InterruptedException ex) {
        	JLog.logger.info("Failed to sleep");
        }
        
        
        simulatedAgent.sendCheckUpdatesWithoutVerify(uuid, epName,"1.2.0.100", 0, 0, "1.1.1", customerId);
        
        JLog.logger.info("Finished Cleanup successfully");
        
    }
       
    /*
	private static void prepareCustomerCaAndCertificates() {
		
		if (GlobalTools.isPortalEnv() || GlobalTools.isProductionEnv()) {
			return;
		}
		
		String LocalCertDirName = PropertiesFile.getManagerDownloadFolder()+ "/" + GlobalTools.getClusterToTest();
		if (!TestFiles.Exists(LocalCertDirName)) {
			TestFiles.CreateFolder(LocalCertDirName);
		}
		
		String customerId = GenericTest.getGeneralData().get("Customer Id");
		
		String Localclientp12 = LocalCertDirName + "/" + getLocalp12Name(customerId);
		if (!TestFiles.Exists(Localclientp12)) {
			String LNEclientp12 = GlobalTools.getLneActions().getClientp12Path(customerId);
			GlobalTools.getLneActions().copy2ManagerMachine(LNEclientp12,LocalCertDirName);
		}
		
		String LocalclientCA = LocalCertDirName + "/" + getLocalCaName();		
		if (!TestFiles.Exists(LocalclientCA)) {
			String LNEclientCA = GlobalTools.getLneActions().getClientCaPath();
			GlobalTools.getLneActions().copy2ManagerMachine(LNEclientCA,LocalCertDirName);
		}
		
	}
	
	
	private static String getLocalp12Name(String customerId) {
		return "/endpoint-111-" + customerId + ".111.p12";
	}
	private static String getLocalCaName() {
		return "ca.jks";
	}
*/
    @AfterMethod
    public void Close(){
    	if (agentActions!=null) {
            agentActions.close();
        }
    }

}
