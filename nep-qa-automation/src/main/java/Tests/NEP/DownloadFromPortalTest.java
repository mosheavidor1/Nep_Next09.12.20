package Tests.NEP;

import Actions.NepActions;
import Tests.GenericTest;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.*;

public class DownloadFromPortalTest extends GenericTest {
    private NepActions action;

    @Factory(dataProvider = "getData")
    public DownloadFromPortalTest(Object dataToSet) {
        super(dataToSet);
        action = new NepActions();

    }

    @Test(groups = { "download" } )
    public void DownloadTest () throws Exception {

        action.CreateAndCleanDownloadFolder();

        action.LaunchApplication(data.get("Browser"));
        action.SetApplicationUrl(PropertiesFile.getCurrentClusterLink());

        action.Login(PropertiesFile.getUserName(), PropertiesFile.getPassword());

        action.GotoPortalFileCabinetPage(PropertiesFile.getCurrentClusterLink());
        action.SelectCustomer(data.get("Customer"));
        action.DeleteAllDownloads();


        action.GotoCentComSearch(PropertiesFile.getCurrentClusterLink());
        action.PublishNewDownloads(data.get("Customer"),Integer.parseInt(data.get("Wait for publish to be completed")));


        action.GotoPortalFileCabinetPage(PropertiesFile.getCurrentClusterLink());
        action.SelectCustomer(data.get("Customer"));

        action.DownloadFilesFromTrustWaveEndPointFolder(data.get("Wait files ready to download timeout"),data.get("Wait because File is still being processed (virus scanned and stored)"));

        action.VerifyFilesExist(Integer.parseInt(data.get("Wait files to be downloaded timeout in seconds")));

        action.VerifyInstallerSignature();


    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        action.CloseApplication();

    }



}
