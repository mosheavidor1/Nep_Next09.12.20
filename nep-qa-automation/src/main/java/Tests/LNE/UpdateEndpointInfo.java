package Tests.LNE;

import Actions.DbActions;
import Actions.DsMgmtActions;
import Actions.LNEActions.CentcomMethods;
import Actions.SimulatedAgentActions;
import DataModel.EndpointErrorDetails;
import DataModel.UpdateEpDetails;
import Tests.GenericTest;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import org.testng.annotations.*;

//https://jira.trustwave.com/browse/NEP-1430
public class UpdateEndpointInfo extends GenericTest {

    private static String customerId;

    private static final String simulatedAgentIp1 = "1.2.159.134";
    private static final String simulatedAgentOs = "Windows 10";
    private static final String simulatedAgentMac1 = "84-7B-EB-21-55";
    private static final String simulatedAgentName = "SimulatedAgentUpdateDetailsTest";

    private static SimulatedAgentActions simulatedAgent = null;
    private static UpdateEpDetails jsonToVerify=null;

    @Factory(dataProvider = "getData")
    public UpdateEndpointInfo(Object dataToSet) {
        super(dataToSet);

    }

    @BeforeMethod
    public void BeforeUpdateEndpointInfoTest()  {
        //ensure that this code will only be called once. BeforeTest annotation is not good enough as it run when suite starts
        if(jsonToVerify==null) {

            customerId = getGeneralData().get("Customer Id");
            simulatedAgent = new SimulatedAgentActions(getGeneralData().get("DS Name"), customerId);
            simulatedAgent.register(customerId, simulatedAgentIp1, simulatedAgentName, simulatedAgentMac1, simulatedAgentOs);
            jsonToVerify = new UpdateEpDetails();
        }

    }


    @Test(groups = { "UpdateEndpointInfo" })
    public void UpdateEndpointInfoTest()  {
    	try {

    	    //The test is building 2 jsons:
            //jsonToUpdateDS - This json is sent to update DS by UpdateEpInfo. The json resets every call to the method so it contain only the changed values for this method run
            //jsonToVerify - this json used to verify the centcom call created after UpdateEpInfo called. This json do not reset every call to the method is accumulates ols calls values and verifies centcom calls contain them

    	    JLog.logger.info("Starting UpdateEndpointInfo test");
    	    UpdateEpDetails jsonToUpdateDS = new UpdateEpDetails();

            String timestamp = DbActions.getCurrentDbTimeStamp();
            if(!data.get("OS").isEmpty()) {
                jsonToVerify.setOsTypeAndVersion(data.get("OS"));
                jsonToUpdateDS.setOsTypeAndVersion(data.get("OS"));
            }
            if(!data.get("Host Name").isEmpty()) {
                jsonToVerify.setHostName(data.get("Host Name"));
                jsonToUpdateDS.setHostName(data.get("Host Name"));
            }
            else { // if hostname is not sent by UpdateEpInfo centcom calls will reset it to null
                jsonToVerify.setHostName(null);
                jsonToUpdateDS.setHostName(null);
            }
            if(!data.get("Mac Address").isEmpty()) {
                // mac address is not reported to CentCom therefore cannot be verified by CentCom call
                jsonToVerify.setMacAddress(null);
                jsonToUpdateDS.setMacAddress(data.get("Mac Address"));
            }
            if(!data.get("IP").isEmpty()) {
                jsonToVerify.setIp(data.get("IP"));
                jsonToUpdateDS.setIp(data.get("IP"));
            }
            if(!data.get("Reporting Status").isEmpty()) {
                jsonToVerify.setReportingStatus(data.get("Reporting Status"));
                jsonToUpdateDS.setReportingStatus(data.get("Reporting Status"));
            }
            if(!data.get("Bin Version").isEmpty()) {
                jsonToVerify.setReportingStatus(data.get("Reporting Status"));
                jsonToUpdateDS.setReportingStatus(data.get("Reporting Status"));
            }

            String errorMsg = data.get("Last Error Message");
            String errorId = data.get("Last Error ID");
            if(!errorId.isEmpty() || !errorMsg.isEmpty()) {
                errorMsg = (errorMsg.isEmpty()) ? null : errorMsg;
                int errorIdInt = (errorId.isEmpty()) ? 0 : Integer.parseInt(errorId);
                EndpointErrorDetails error = new EndpointErrorDetails(timestamp, errorIdInt, errorMsg);
                jsonToVerify.setLastError(error);
                jsonToUpdateDS.setLastError(error);
            }
            else {
                jsonToVerify.setLastError(null);
                jsonToUpdateDS.setLastError(null);

            }


            simulatedAgent.UpdateEpInfo(simulatedAgent.getAgentUuid(),jsonToUpdateDS);

            String timeout = getGeneralData().get("Verify CentCom Call Timeout");
            jsonToVerify.setCustomerId(customerId);
            jsonToVerify.setName(simulatedAgentName);

            //short sleep to create unique timestamp at CentCom calls - sometimes 2 calls created with the same timestamp without it
            Thread.sleep(2000);

            DbActions.verifyCallToCentcom(CentcomMethods.UPDATE_ENDPOINT,jsonToVerify,timestamp,Integer.parseInt(timeout));

            JLog.logger.info("UpdateEndpointInfo test completed successfully.");

        } catch (Exception e) {
            org.testng.Assert.fail("UpdateEndpointInfo test: " + e.toString());
        }
        
    }
    

    @AfterTest
    public void Close(){
        
    	if (simulatedAgent != null) {
    		DsMgmtActions.deleteWithoutVerify(customerId, simulatedAgentName);
    		simulatedAgent.sendCheckUpdatesAndGetAction(simulatedAgentName, GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
           
    	}
        
    }

}
