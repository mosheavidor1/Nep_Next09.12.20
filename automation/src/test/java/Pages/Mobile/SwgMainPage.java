package Pages.Mobile;

import WebElements.Mobile.ButtonMobile;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;

public class SwgMainPage extends GenericMobilePage {
	
	private static final String statusButtonID = "com.trustwave.trustwave:id/tv_connection";
	//private static final String reconnectButtonID = "com.trustwave.trustwave:id/tv_reconnect";
	
    @AndroidFindBy(id = "com.trustwave.trustwave:id/tv_reconnect")
    public AndroidElement reconnectButton;

    @AndroidFindBy(id = statusButtonID)
    private AndroidElement statusButton_Element;
 	public ButtonMobile statusButton;

    public  SwgMainPage() {
    	statusButton = new ButtonMobile(statusButton_Element ,  statusButtonID,  "id");
    	
    }

	
	


}
