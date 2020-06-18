package Tests.NEP;

import Actions.NepActions;
import Tests.GenericTest;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class ChangeEPConfigurationTest extends GenericTest {
    private NepActions action;

    @Factory(dataProvider = "getData")
    public ChangeEPConfigurationTest(Object dataToSet) {
        super(dataToSet);
        action = new NepActions();

    }

    @Test(groups = { "download" } )
    public void DownloadTest () {

        action.LaunchApplication(general.get("Browser"));
        action.SetApplicationUrl(general.get("Fusion Link"));

        action.Login(general.get("Fusion User Name"), general.get("Fusion Password"));

        action.GotoCentComSearch(general.get("Fusion Link"));
        action.ChangeConfigurationAndPublish(data.get("Customer"),Integer.parseInt(data.get("Wait for publish to be completed")));

        //put here code for checking json file


    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        action.CloseApplication();

    }



}
