package Tests.Environments;

import Actions.BrowserActions;
import Tests.GenericTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.io.IOException;

public class ChangeEPConfigurationTest extends GenericTest {
    private BrowserActions action;

    @Factory(dataProvider = "getData")
    public ChangeEPConfigurationTest(Object dataToSet) {
        super(dataToSet);
        action = new BrowserActions();

    }

    // This is just preparation for a test the will change configuration at the environments and verify it is accepted successfully
    @Test(groups = { "download" } )
    public void DownloadTest () throws IOException {

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
