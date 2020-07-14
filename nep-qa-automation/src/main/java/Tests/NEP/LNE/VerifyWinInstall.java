package Tests.NEP.LNE;

import Actions.LNEActions;
import Tests.GenericTest;
import Utils.PropertiesFile.PropertiesFile;
import org.json.JSONObject;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class VerifyWinInstall extends GenericTest {

    private LNEActions action;

    @Factory(dataProvider = "getData")
    public VerifyWinInstall(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "Verify" } )
    public void SetConfigurationDownloadInstallAndVerify () {
        action = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));

        action.CreateAndCleanDownloadFolder();

        String confJson =data.get("Settings Json");
        JSONObject json = new JSONObject(confJson);
        long customerId = json.getLong( "customerId" );

        action.DeleteCurrentInstaller(customerId);

        action.InitCustomerSettings(confJson,Integer.parseInt(data.get("From LNE up until response OK timeout")));

        action.DownloadInstaller(customerId , Integer.parseInt(data.get("Download timeout")));

        action.VerifyFilesExist(30);
        action.VerifyInstallerSignature();

        action.AppendToHostsFile();

        action.UnInstallEndPoint(Integer.parseInt(general.get("EP Installation timeout")));
        action.InstallEndPoint(Integer.parseInt(general.get("EP Installation timeout")));
        action.StopEPService(Integer.parseInt(general.get("EP Service Timeout")));
        action.AddCACertificate();
        action.StartEPService(Integer.parseInt(general.get("EP Service Timeout")));
        action.CheckEndPointActiveByDbJson(Integer.parseInt(general.get("From EP service start until logs show EP active timeout")));

    }

}
