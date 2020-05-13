package Tests.Mobile;

import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import Actions.MobileActions;
import Pages.Mobile.GenericMobilePage;
import Tests.GenericTest;

public class InstallApplication extends GenericTest {
	private MobileActions action;
		
	 @Factory (dataProvider = "getData")
	 public InstallApplication(Object dataToSet) {
	 		super(dataToSet);
	 		action = new MobileActions();
	 	}

  @Test (priority=1)
  public void InstallApp() throws Exception {
	  	action.RemoveApp(data.get("App Package Name"),data.get("Device Name"), data.get("Version"));
	  	
	  	action.LaunchApplication(data.get("Device Name"), data.get("Version"), data.get("apk Path"),data.get("App Package Name"),data.get("App Activity"),data.get("Dont Reset Application"));
	  	
	  	action.InstallTrustwaveApp(data.get("Company ID"), data.get("User Name"), data.get("Password"), data.get("Expected Status"), data.get("Status Wait Time"));

	  	action.CloseDriver();

 	  	action.LaunchApplication(data.get("Device Name"), data.get("Version"), "" ,data.get("App Package Name"),data.get("App Activity"),"true");
	  	action.ReconnectSwgApp(data.get("Expected Status"), data.get("Status Wait Time"));
 
	  	action.StartActivity(GenericMobilePage.chromePackage, GenericMobilePage.chromeActivity);
	  	action.OpenSafeSite(data.get("Safe Site"));
 
 	  action.StartActivity(GenericMobilePage.chromePackage, GenericMobilePage.chromeActivity);
	  action.OpenMaliciousSite(data.get("Malicious Site"));
  
  }

 // @AfterClass
  public void afterClass()  {
	  action.CloseDriver();
  }

}
