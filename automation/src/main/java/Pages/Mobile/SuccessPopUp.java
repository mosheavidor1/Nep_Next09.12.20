package Pages.Mobile;

import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;

public class SuccessPopUp extends GenericMobilePage {
	
    @AndroidFindBy(id = "com.trustwave.trustwave:id/confirm_button")
    public AndroidElement okButton;

}
