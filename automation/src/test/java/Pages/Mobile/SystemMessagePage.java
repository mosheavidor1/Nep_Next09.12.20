package Pages.Mobile;

import WebElements.Mobile.ButtonMobile;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;

public class SystemMessagePage extends GenericMobilePage {

	private static final String okButtonID = "android:id/button1";
	
    @AndroidFindBy(id = okButtonID)
    private AndroidElement okButton_Element;
 	public ButtonMobile okButton;

    public  SystemMessagePage() {
    	okButton = new ButtonMobile(okButton_Element ,  okButtonID,  "id");
    	
    }
    

}
