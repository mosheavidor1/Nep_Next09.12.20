package Tests.NEP;

import Actions.NepActions;
import Tests.GenericTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class InstallEndPointTest extends GenericTest {
    private NepActions action;

    @Factory(dataProvider = "getData")
    public InstallEndPointTest(Object dataToSet) {
        super(dataToSet);
        action = new NepActions();

    }

    @Test(groups = { "install" } )
    public void InstallTest () throws Exception {
        action.VerifyFilesExist(1);
        action.UnInstallEndPoint(Integer.parseInt(data.get("Uninstall timeout")));
        action.InstallEndPoint(Integer.parseInt(data.get("Installation timeout")));

        //no more replace certificates
        //action.ReplaceEndPointFilesAndRestartService(Integer.parseInt(data.get("Service Start/Stop timeout")));

        action.CheckEndPointActiveByDbJson(Integer.parseInt(data.get("From service start until logs show active timeout")));


    }


}
