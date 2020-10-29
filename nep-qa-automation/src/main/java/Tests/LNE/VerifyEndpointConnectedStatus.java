package Tests.LNE;

import Actions.DsMgmtActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

//Second step of https://jira.trustwave.com/browse/NEP-1279
//First step exists in InitAndCleanup, then need to waits a while...so other tests are running including BinaryUpdate...
public class VerifyEndpointConnectedStatus extends GenericTest {

    private final String customerId;
    private LNEActions lneActions = GlobalTools.getLneActions();

    @Factory(dataProvider = "getData")
    public VerifyEndpointConnectedStatus(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }

    @Test(groups = { "VerifyEndpointConnectedStatus" } , priority=99)
    public void verifyEndpointConnectedStatus() {
        try {
            JLog.logger.info("Starting verifyEndpointConnectedStatus test ...");
            long timeLeftInSeconds = TimeUnit.MINUTES.toSeconds(15) - Instant.now().minus(InitAndCleanup.whenInit.getEpochSecond(), ChronoUnit.SECONDS).getEpochSecond();
            JLog.logger.info("going to wait for endpoint to no be connected, seconds left until checking status not connected: {}",timeLeftInSeconds);
            if(timeLeftInSeconds>0){
                Thread.sleep(1/*TimeUnit.SECONDS.toMillis(timeLeftInSeconds)*/);
            }
            lneActions.verifyCallToUpdateEpStateCentcomCommand(LNEActions.CentcomMethods.UPDATE_ENDPOINT_STATE,customerId,InitAndCleanup.epNameForConnectivityTest,"NOT_CONNECTED");
            

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    @AfterTest
    public void close() {

        //delete if exist and clean cluster
        try {
            DsMgmtActions.deleteWithoutVerify(customerId, InitAndCleanup.epNameForConnectivityTest);
            InitAndCleanup.simulatedAgentForConnectivityTest.sendCheckUpdatesAndGetResponse(InitAndCleanup.epNameForConnectivityTest, GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);

        } catch (Exception e) {

        } finally {
            JLog.logger.info("Closing...");
            if (lneActions != null) {
                lneActions.Close();
            }
        }
    }
}
