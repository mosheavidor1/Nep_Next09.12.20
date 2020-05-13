package Tests.SWG;

import Actions.CloudActions;
import Tests.GenericTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class OpenClose extends GenericTest {

	private CloudActions action;

	@Factory (dataProvider = "getData")
	public OpenClose(Object dataToSet) {
		super(dataToSet);
	 	action = new CloudActions();
	 }

	 @Test (alwaysRun = true)
	 public void SAMLTest1 () throws Exception {


		while (true) {
			action.LaunchApplication(data.get("Browser"));
			action.SetApplicationUrl(data.get("Block Site"));
			action.CheckNOTBlockedPage(data.get("Block Site"));
			action.CloseApplication();
		}


	 }


	
}
