package Tests.LNE;

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


public class InitAndCleanup extends GenericTest {

	private BaseAgentActions agentActions;
	private LNEActions lneActions = GlobalTools.getLneActions();
    private String customerId;
    private static boolean initWasDone = false;
    public static String epNameForConnectivityTest = "ChiefEp";
    public static Instant whenInit = Instant.now();
    public static SimulatedAgentActions simulatedAgentForConnectivityTest;
    private SimulatedAgentActions simulatedAgent;
    

    @Factory(dataProvider = "getData")
    public InitAndCleanup(Object dataToSet) {
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
    @Test(groups = { "InitAndCleanup" }, priority=10)//init should run before cleanup, since cleanup alreasy uses the simulated agent
    public void init()  {
    	
    	 if (initWasDone) { //Init should run only once
    		 return;
    	 }
    	
    	 JLog.logger.info("Starting init...");
    	 
    	 prepareCustomerCaAndCertificates();  
    	 changeServicesProperties();
    	 initWasDone = true;    	 
    	 
    	 JLog.logger.info("Finished init successfully");
    	
    }
    
    

    /**
     * Need to delete existing endpoints from DS side + check update until we get uninstall action (then entities are actually deleted for DB)
     * This is required to be sure that the tests really test a new installation flow even if Lenny was just upgraded by update services job
     * The cleanup is tolerant for errors since we should succeed even if Lenny was re-deployed and we have no leftovers from previous runs
     */
    @Test(groups = { "InitAndCleanup" }, priority=11)
    public void cleanup()  {

        JLog.logger.info("Starting Cleanup...");

        JLog.logger.info("Cleaning binary updates test leftovers if they exists");
        getDbConnector().cleanGlobalVersionsAfterBinaryUpdate();
        getDbConnector().cleanEndpointBinVerEpRequest();

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
        
        
        simulatedAgent.sendCheckUpdatesWithoutVerify(uuid, epName, GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
        
        
        JLog.logger.info("Finished Cleanup successfully");
        
    }
    
    //First step of https://jira.trustwave.com/browse/NEP-1279
    @Test(groups = { "InitAndCleanup" }, priority=12 )
    public void defineAgentForConnectivityTest(){
        JLog.logger.info("Starting InitTests test ...");
        
        simulatedAgentForConnectivityTest = new SimulatedAgentActions(getGeneralData().get("DS Name"), customerId);
        simulatedAgentForConnectivityTest.register(customerId, "1.2.3.4", epNameForConnectivityTest, "66-7B-EB-71-99-44", "Windows 10");
        simulatedAgentForConnectivityTest.sendCheckUpdatesAndGetAction(epNameForConnectivityTest, GlobalTools.currentBinaryBuild, 1, 0, GlobalTools.currentSchemaVersion, customerId);
        whenInit = Instant.now();
        lneActions.verifyCallToUpdateEpStateCentcomCommand(LNEActions.CentcomMethods.UPDATE_ENDPOINT_STATE, customerId, epNameForConnectivityTest,"OK");
        JLog.logger.info("registered ep {} and verified status, status OK ",epNameForConnectivityTest);

    }
    
    
	private static void prepareCustomerCaAndCertificates() {
		
		if (GlobalTools.isPortalEnv() || GlobalTools.isProductionEnv()) {
			return;
		}
		
		String LocalCertDirName = PropertiesFile.getManagerDownloadFolder()+ "/" + GlobalTools.getClusterToTest();
		if (!TestFiles.Exists(LocalCertDirName)) {
			TestFiles.CreateFolder(LocalCertDirName);
		}

        TestFiles.DeleteAllFiles(LocalCertDirName);

        String customerId = GenericTest.getGeneralData().get("Customer Id");

        String LNEclientp12 = GlobalTools.getLneActions().getClientp12Path(customerId);
		GlobalTools.getLneActions().copy2ManagerMachine(LNEclientp12,LocalCertDirName);
		
		String LNEclientCA = GlobalTools.getLneActions().getClientCaPath();
		GlobalTools.getLneActions().copy2ManagerMachine(LNEclientCA,LocalCertDirName);

		
	}
	
	private void changeServicesProperties() {
        try{
            Map<String, String> dsPropertiesChange = new HashMap<>();
            Map<String, String> dsMgmtPropertiesChange = new HashMap<>();
            Map<String, String> isPropertiesChange = new HashMap<>();


            dsMgmtPropertiesChange.put("ep-conn-check.run-every-milliseconds","300000");


            boolean dsPropertyChanged = lneActions.changePropertyInPropertySet(LNEActions.NepService.DS, dsPropertiesChange);
            boolean dsMgmtPropertyChanged = lneActions.changePropertyInPropertySet(LNEActions.NepService.DS_MGMT, dsMgmtPropertiesChange);
            boolean isPropertyChanged = lneActions.changePropertyInPropertySet(LNEActions.NepService.IS, isPropertiesChange);

            if(dsPropertyChanged || dsMgmtPropertyChanged || isPropertyChanged){
                lneActions.restartStubServiceWaitForFinish(60);
              

                if(dsPropertyChanged){
                    lneActions.restartDsService();
//                    lneActions.restartServiceWaitForFinish(LNEActions.NepService.DS,300);
                }
                if(dsMgmtPropertyChanged){
                    lneActions.restartDsMgmtService();
//                    lneActions.restartServiceWaitForFinish(LNEActions.NepService.DS_MGMT,300);
                }
                if(isPropertyChanged){
                    lneActions.restartIsService();
//                    lneActions.restartServiceWaitForFinish(LNEActions.NepService.IS,300);
                }

                Thread.sleep(60000);
            }


        }catch (InterruptedException e){

        }
    }
	
    @AfterMethod
    public void Close(){
    	if (agentActions!=null) {
            agentActions.close();
        }
    }

}
