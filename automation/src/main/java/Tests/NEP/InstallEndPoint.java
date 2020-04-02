package Tests.NEP;

import Actions.NepActions;
import Tests.GenericTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class InstallEndPoint extends GenericTest {
    private NepActions action;

    @Factory(dataProvider = "getData")
    public InstallEndPoint(Object dataToSet) {
        super(dataToSet);
        action = new NepActions();

    }

    @Test(alwaysRun = true)
    public void InstallTest () throws Exception {

        action.UnInstallEndPoint(Integer.parseInt(data.get("Uninstall timeout")));
        action.InstallEndPoint(Integer.parseInt(data.get("Installation timeout")));
        action.ReplaceEndPointFilesAndRestartService(Integer.parseInt(data.get("Service Start/Stop timeout")));
        action.CheckEndPointActiveByDbJson(Integer.parseInt(data.get("From service start until logs show active timeout")));

    }


}
