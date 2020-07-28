package Tests.Environments;

import Actions.BrowserActions;
import Tests.GenericTest;
import org.testng.annotations.*;

import java.io.IOException;

public class VerifyEndPointOkAtPortalTest extends GenericTest {
    private BrowserActions action;

    @Factory(dataProvider = "getData")
    public VerifyEndPointOkAtPortalTest(Object dataToSet) {
        super(dataToSet);
        action = new BrowserActions();

    }

    @Test( groups = { "verify" } )
    public void VerifyEndPointStatusAtPortalTest () throws IOException {


        action.LaunchApplication(general.get("Browser"));
        action.SetApplicationUrl(general.get("Fusion Link"));

        action.Login(general.get("Fusion User Name"), general.get("Fusion Password"));

        action.GotoCentComSearch(general.get("Fusion Link"));
        action.CheckEndPointOkInCentCom(data.get("Customer"));

    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        action.CloseApplication();

    }

}
