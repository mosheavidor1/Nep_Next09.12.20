package Tests.Environments;

import Actions.BrowserActions;
import Tests.RecordedTest;
import Utils.Logs.JLog;

import org.testng.annotations.*;


public class SelectCustomerDownloadInstaller extends RecordedTest {
    private BrowserActions action;

    @Factory(dataProvider = "getData")
    public SelectCustomerDownloadInstaller(Object dataToSet) {
        super(dataToSet);
        action = new BrowserActions();

    }

    @Test(groups = { "SelectCustomerDownloadInstaller" } )
    public void selectCustomerDownloadInstaller () {
    	
    	JLog.logger.info("Starting selectCustomerDownloadInstaller...");

        action.CreateAndCleanDownloadFolder();

        action.LaunchApplication(getGeneralData().get("Browser"));
        action.SetApplicationUrl(getGeneralData().get("Fusion Link"));

        action.Login(getGeneralData().get("Fusion User Name"), getGeneralData().get("Fusion Password"));

        action.GotoPortalFileCabinetPage(getGeneralData().get("Fusion Link"));
        action.SelectCustomer(data.get("Customer"));
        action.DeleteAllDownloads();


        action.GotoCentComSearch(getGeneralData().get("Fusion Link"));
        action.PublishNewDownloads(data.get("Customer"),Integer.parseInt(data.get("Wait for publish to be completed")));


        action.GotoPortalFileCabinetPage(getGeneralData().get("Fusion Link"));
        action.SelectCustomer(data.get("Customer"));

        action.DownloadFilesFromTrustWaveEndPointFolder(data.get("Wait files ready to download timeout"),data.get("Wait because File is still being processed (virus scanned and stored)"));

        action.VerifyFilesExist(Integer.parseInt(data.get("Wait files to be downloaded timeout in seconds")));

        action.VerifyInstallerSignature();
        
        JLog.logger.info("Finished selectCustomerDownloadInstaller.");


    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        if (action != null) {
            action.CloseApplication();
        }

    }



}
