package Tests.SWG;

import Actions.CloudActions;
import Tests.GenericTest;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.io.IOException;

public class BlockURLTest extends GenericTest {

	private CloudActions action;

	@Factory (dataProvider = "getData")
	public BlockURLTest(Object dataToSet) {
		super(dataToSet);
	 	action = new CloudActions();
	 }

	 @Test (alwaysRun = true)
	 public void BlockURLTest () throws Exception {

		 String action = PropertiesFile.readProperty("Action").toUpperCase();
		 switch (action){
			 case "DELETERULES":
				 DeleteURLRules();
				 break;
			 case "CHECKALLOWED":
				 CheckSiteAllowed();
				 break;
			 case "ADDRULE":
				 AddBlockRole();
				 break;
			 case "CHECKBLOCKED":
				 CheckSiteBlocked();
				 break;
			 default:
			 	String errorMessage = "Action: '" + action + "' is invalid. Please send one of the following actions to this test: DELETERULES, CHECKALLOWED, ADDRULE, CHECKBLOCKED";
				org.testng.Assert.fail( errorMessage);


		 }

	 }

	 public void DeleteURLRules() throws Exception {
		 OpenPoliciesPage();
		 action.DeleteRules(data.get("Block Site"));
		 action.CloseApplication();

	 }

	 public void CheckSiteAllowed () throws IOException {
		 action.LaunchApplication(data.get("Browser"),PropertiesFile.getCurrentClusterLB() );
		 action.SetApplicationUrl(data.get("Block Site"));
		 action.CheckNOTBlockedPage(data.get("Block Site"));
		 action.CloseApplication();
	 }

	 public void AddBlockRole() throws Exception {
		 OpenPoliciesPage();
		 action.AddRule(data.get("Block Site"));
		 action.CloseApplication();

	 }

	 public void CheckSiteBlocked () throws IOException {
		 action.LaunchApplication(data.get("Browser"),PropertiesFile.getCurrentClusterLB() );
		 action.SetApplicationUrl(data.get("Block Site"));
		 action.CheckBlockedPage();

	 }
	 
	 public void OpenPoliciesPage() throws Exception {
		 
	     action.LaunchApplication(data.get("Browser"));
		 action.SetApplicationUrl(PropertiesFile.getCurrentClusterLink());

		 action.Login(PropertiesFile.getUserName(), PropertiesFile.getPassword());

		 action.GotoSWGConfigurationPage(PropertiesFile.getCurrentClusterLink());

		 action.SelectCustomerAtConfigurationPage(PropertiesFile.getCustomerNameOrID() );

		 action.GotoSWGPoliciesPage(PropertiesFile.getCurrentClusterLink());

		 
	 }
	  
	
	  @BeforeClass
	  public void beforeClass() throws Exception {


	  }

	@AfterClass
	public void afterClass() throws Exception {

		 action.CloseApplication();

	}

	
	
}
