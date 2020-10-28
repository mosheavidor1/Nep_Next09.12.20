package Tests.Environments;

import Actions.BrowserActions;
import Tests.RecordedTest;
import Utils.Logs.JLog;

import org.testng.annotations.*;


public class CreateNewInstallers extends RecordedTest {
    private BrowserActions action;

    @Factory(dataProvider = "getData")
    public CreateNewInstallers(Object dataToSet) {
        super(dataToSet);
        action = new BrowserActions();

    }

    @Test(groups = { "CreateNewInstallers" } )
    public void CreateNewInstalllersAndDeleteOldInstallers () {
        try {

            JLog.logger.info("Starting Create New Installers...");

            action.LaunchApplication(getGeneralData().get("Browser"));
            action.SetApplicationUrl(getGeneralData().get("Fusion Link"));

            action.Login(getGeneralData().get("Fusion User Name"), getGeneralData().get("Fusion Password"));

            action.GotoPortalFileCabinetPage(getGeneralData().get("Fusion Link"));
            action.SelectCustomer(data.get("Customer"));
            action.DeleteAllDownloads();


            action.GotoCentComSearch(getGeneralData().get("Fusion Link"));
            action.PublishNewDownloads(data.get("Customer"), Integer.parseInt(data.get("Wait for publish to be completed")));
            
            JLog.logger.info("Finished Create New Installers.");
        }
        catch (Exception e){
            org.testng.Assert.fail("Could not create new installers " +e.toString());
        }


    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        if (action != null) {
            action.CloseApplication();
        }

    }



}
