package Tests.LNE;

import Actions.LNEActions;
import Tests.GenericTest;
import Utils.JsonUtil;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class SetConfigAndDownloadInstaller extends GenericTest {

    private LNEActions manager;

    @Factory(dataProvider = "getData")
    public SetConfigAndDownloadInstaller(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "InitCustomer" } )
    public void InitConfigAndDownloadInstaller () {

        manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
        manager.CreateAndCleanDownloadFolder();
        String confJson =data.get("Settings Json");
        long customerId = JsonUtil.GetCustomerIDFromSentConfiguration(confJson);
        manager.DeleteCurrentInstallerFromLNE(customerId);
        manager.InitCustomerSettings(confJson,Integer.parseInt(data.get("From LNE up until response OK timeout")));
        manager.DownloadInstaller(customerId , Integer.parseInt(data.get("Download timeout")));
        manager.VerifyInstallerSignature();
        manager.WriteCACertificateToFile();
    }

}
