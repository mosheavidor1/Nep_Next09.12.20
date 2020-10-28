package Tests.Environments;

import Actions.BrowserActions;
import Tests.RecordedTest;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class DownloadInstallers extends RecordedTest {
    private BrowserActions action;

    @Factory(dataProvider = "getData")
    public DownloadInstallers(Object dataToSet) {
        super(dataToSet);
        action = new BrowserActions();

    }

    @Test(groups = { "DownloadInstallers" } )
    public void downloadInstallersFromFileCabinet () {
        try {
            JLog.logger.info("Starting download Installers From File Cabinet...");

            action.CreateAndCleanDownloadFolder();

            action.LaunchApplication(getGeneralData().get("Browser"));
            action.SetApplicationUrl(getGeneralData().get("Fusion Link"));

            action.Login(getGeneralData().get("Fusion User Name"), getGeneralData().get("Fusion Password"));

            action.GotoPortalFileCabinetPage(getGeneralData().get("Fusion Link"));
            action.SelectCustomer(data.get("Customer"));

            action.DownloadFilesFromTrustWaveEndPointFolder(data.get("Wait files ready to download timeout"), data.get("Wait because File is still being processed (virus scanned and stored)"));

            action.VerifyFilesExist(Integer.parseInt(data.get("Wait files to be downloaded timeout in seconds")));

            action.VerifyInstallerSignature();  
            

            JLog.logger.info("Finished download Installers.");
        }
        catch (Exception e){
            org.testng.Assert.fail("Could not create download installers from file cabinet " +e.toString());
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
