package Tests.Environments;

import Actions.BrowserActions;
import Actions.ManagerActions;
import Tests.GenericTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class InstallEndPointTest extends GenericTest {
    private BrowserActions action;

    @Factory(dataProvider = "getData")
    public InstallEndPointTest(Object dataToSet) {
        super(dataToSet);
        action = new BrowserActions();

    }

    @Test(groups = { "install" } )
    public void InstallTest () {

        action.VerifyFilesExist(5);
        action.UnInstallEndPoint(Integer.parseInt(data.get("Uninstall timeout")));
        action.InstallEndPoint(Integer.parseInt(data.get("Installation timeout")));

        //no more replace certificates
        //action.ReplaceEndPointFilesAndRestartService(Integer.parseInt(data.get("Service Start/Stop timeout")));

        action.CheckEndPointActiveByDbJson(Integer.parseInt(data.get("From service start until logs show active timeout")));


    }


}
