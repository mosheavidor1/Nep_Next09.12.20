package Pages.Mobile;


import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;

public class LoginPage extends GenericMobilePage {
	

	    @AndroidFindBy(id = "com.trustwave.trustwave:id/etCompanyId")
	    public AndroidElement companyID;

	    @AndroidFindBy(id = "com.trustwave.trustwave:id/tvNext")
	    public AndroidElement nextButton;



}
