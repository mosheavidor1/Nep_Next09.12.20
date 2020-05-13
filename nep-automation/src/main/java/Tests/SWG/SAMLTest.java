package Tests.SWG;

import Actions.CloudActions;
import Tests.GenericTest;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.*;

public class SAMLTest extends GenericTest {

	private CloudActions action;

	@Factory (dataProvider = "getData")
	public SAMLTest(Object dataToSet) {
		super(dataToSet);
	 	action = new CloudActions();
	 }

	 @Test (alwaysRun = true)
	 public void SAMLTest1 () throws Exception {

		 action.LaunchApplicationWithProxyExtension(data.get("Browser"), PropertiesFile.getCurrentClusterLB());
		 action.FillProxyExtensionData(data.get("Proxy user name"),data.get("Proxy password"));
		 action.SetApplicationUrl(data.get("Site to verify"));
		 action.LoginToTrustwaveAndPingIdentityFill(data.get("Ping username"),data.get("Ping password"));
		 action.CheckNOTBlockedPage(data.get("Site to verify"));
		 action.CloseApplication();


	 }


	
}
