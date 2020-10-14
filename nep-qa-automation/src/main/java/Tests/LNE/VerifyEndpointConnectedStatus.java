package Tests.LNE;

import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class VerifyEndpointConnectedStatus extends GenericTest {

    private final String customerId;
    private LNEActions lneActions;

    @Factory(dataProvider = "getData")
    public VerifyEndpointConnectedStatus(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
    }

    @Test(groups = { "VerifyEndpointConnectedStatus" } , priority=99)
    public void verifyEndpointConnectedStatus() {
        try {
            JLog.logger.info("Starting verifyEndpointConnectedStatus test ...");
            lneActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
            long timeLeftInSeconds = TimeUnit.MINUTES.toSeconds(15) - Instant.now().minus(InitTests.whenInit.getEpochSecond(), ChronoUnit.SECONDS).getEpochSecond();
            JLog.logger.info("going to wait for endpoint to no be connected, seconds left until checking status not connected: {}",timeLeftInSeconds);
            if(timeLeftInSeconds>0){
                Thread.sleep(1/*TimeUnit.SECONDS.toMillis(timeLeftInSeconds)*/);
            }
            lneActions.verifyCallToUpdateEpStateCentcomCommand(LNEActions.CentcomMethods.UPDATE_ENDPOINT_STATE,customerId,InitTests.initEpName,"NOT_CONNECTED");
            

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    @AfterTest
    public void close() {

        //delete if exist and clean cluster
        try {
            lneActions.deleteWithoutVerify(customerId, InitTests.initEpName);
            InitTests.simulatedAgent.sendCheckUpdatesAndGetResponse(InitTests.initEpName, "1.2.0.100", 0, 0, "1.1.1", customerId);

        } catch (Exception e) {

        } finally {
            JLog.logger.info("Closing...");
            if (lneActions != null) {
                lneActions.Close();
            }
        }
    }
}
