package Tests.NEP.LNE;

import Actions.LNEActions;
import Tests.GenericTest;
import Utils.PropertiesFile.PropertiesFile;
import org.json.JSONObject;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class VerifyInstall extends GenericTest {

    private LNEActions action;

    @Factory(dataProvider = "getData")
    public VerifyInstall(Object dataToSet) {
        super(dataToSet);
    }

    @Test()
    public void SetConfigurationDownloadInstallAndVerify () {
        action = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));

        action.CreateAndCleanDownloadFolder();

        String conjJson =data.get("Settings Json");
        JSONObject json = new JSONObject(conjJson);
        long customerId = json.getLong( "customerId" );

        action.InitCustomerSettings(conjJson);
        action.DownloadInstaller(general.get("LNE File Cabinet Path"),customerId , Integer.parseInt(data.get("Download timeout")));

        action.VerifyFilesExist(30);
        action.VerifyInstallerSignature();

        action.AppendToHostsFile();

        action.UnInstallEndPoint(Integer.parseInt(general.get("EP Installation timeout")));
        action.InstallEndPoint(Integer.parseInt(general.get("EP Installation timeout")));
        action.StopEPService(Integer.parseInt(general.get("EP Installation timeout")));
        action.AddCACertificate();
        action.StartEPService(Integer.parseInt(general.get("EP Installation timeout")));
        action.CheckEndPointActiveByDbJson(Integer.parseInt(general.get("From EP service start until logs show EP active timeout")));


    }

}