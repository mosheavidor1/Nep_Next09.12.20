package Pages.Mobile;

import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;

public class PingIdentityPage extends GenericMobilePage {
	
    @AndroidFindBy(id = "ping-username")
    public AndroidElement userName;
    
    @AndroidFindBy(id = "ping-password")
    public AndroidElement password;
    
    @AndroidFindBy(id = "btn-sign-in")
    public AndroidElement signInButton;

    

}
