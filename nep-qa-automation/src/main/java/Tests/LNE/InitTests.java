package Tests.LNE;

import Actions.LNEActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Map;

public class InitTests extends GenericTest {

    private LNEActions lneActions = GlobalTools.getLneActions();
    private String customerId;
    public static SimulatedAgentActions simulatedAgent;
    public static String initEpName = "ChiefEp";
    public static Instant whenInit = Instant.now();

    @Factory(dataProvider = "getData")
    public InitTests(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }

    @Test(groups = { "InitTests" } )
    public void initTests(){
        JLog.logger.info("Starting InitTests test ...");
        changeServicesProperties();
        simulatedAgent = new SimulatedAgentActions(customerId);
        simulatedAgent.register(customerId, "1.2.3.4", initEpName, "66-7B-EB-71-99-44", "Windows 10");
        simulatedAgent.sendCheckUpdatesAndGetAction(initEpName, "9.9.9.999", 1, 0, "1.1.1", customerId);
        whenInit = Instant.now();
        lneActions.verifyCallToUpdateEpStateCentcomCommand(LNEActions.CentcomMethods.UPDATE_ENDPOINT_STATE,customerId,initEpName,"OK");
        JLog.logger.info("registered ep {} and verified status, status OK ",initEpName);

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

    @AfterTest
    public void close() {
        //delete if exist and clean cluster
        JLog.logger.info("Closing...");
        if (lneActions != null) {
            lneActions.Close();
        }

    }
}
