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
    public void VerifyEndPointStatusAtPortalTest () throws Exception {


        action.LaunchApplication(data.get("Browser"));
        action.SetApplicationUrl(PropertiesFile.getCurrentClusterLink());

        action.Login(PropertiesFile.getUserName(), PropertiesFile.getPassword());

        action.GotoCentComSearch(PropertiesFile.getCurrentClusterLink());
        action.CheckEndPointOkInCentCom(data.get("Customer"));

    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        action.CloseApplication();

    }

}
