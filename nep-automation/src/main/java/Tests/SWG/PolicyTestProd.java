package Tests.SWG;

import Actions.CloudActions;
import Tests.GenericTest;
import Utils.PropertiesFile.PropertiesFile;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class PolicyTestProd extends GenericTest {

    private static WebDriver driver;

    private CloudActions action;

    @Factory(dataProvider = "getData")
    public PolicyTestProd(Object dataToSet) {
        super(dataToSet);
        action = new CloudActions();


    }



    @Test
    public void PolicyTest1() throws Exception {
//Policy test - login page
        OpenPolicyTestPage();

        //OpenPoliciesPage();







//Run's the policyTest test
        policyTestRunTest();






    }







    public void OpenPolicyTestPage() throws Exception {






        action.LaunchApplication(data.get("Browser"));

        //action.SetApplicationUrl(data.get("URL"));

        //action.Login(data.get("UserName"), data.get("Password"));



        action.SetApplicationUrl(PropertiesFile.getCurrentClusterLink() );   //Cluster Link





        action.Login(PropertiesFile.getUserName(), PropertiesFile.getPassword());  // User name + password from properties file.


        action.GotoSWGPolicyTestPage(PropertiesFile.getCurrentClusterLink()); // Add the Login page Link

        //action.GotoSWGPolicyTestPage(data.get("URL"));  - //URL Link from excel .




    }


    public void policyTestRunTest () throws Exception {


        action.PolicyTestTypeURL(data.get("URL TO BLOCK"));

        action.PolicyTestTypeIP(data.get("IP TO BLOCK"));
        action.ClickOnGo();
        action.PageIsBlocked();
        //  action.PolicyTestBlock();   ////BLOCK MESSAGE policy test


    }



    public void OpenPoliciesPage () throws Exception {

        action.LaunchApplication(data.get("Browser"));
        action.SetApplicationUrl(PropertiesFile.getCurrentClusterLink());


    }




    @BeforeClass
    public void beforeClass () {


    }
//
//        @AfterClass
//        public void afterClass() {
//
//            action.CloseApplication();

}




