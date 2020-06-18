package Tests.NEP;

import Actions.NepActions;
import Tests.GenericTest;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.*;

public class VerifyEndPointOkAtPortalTest extends GenericTest {
    private NepActions action;

    @Factory(dataProvider = "getData")
    public VerifyEndPointOkAtPortalTest(Object dataToSet) {
        super(dataToSet);
        action = new NepActions();

    }

    @Test( groups = { "verify" } )
    public void VerifyEndPointStatusAtPortalTest () {


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
