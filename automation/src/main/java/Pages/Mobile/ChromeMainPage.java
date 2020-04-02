package Pages.Mobile;

import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AndroidFindBy;

public class ChromeMainPage extends GenericMobilePage{

    @AndroidFindBy(id = "com.android.chrome:id/menu_button")
    public AndroidElement menuButton;

    @AndroidFindBy(xpath = "//android.widget.LinearLayout[@index='2']")
    public AndroidElement chromeIncognito;

    @AndroidFindBy(id = "com.android.chrome:id/url_bar")
    public AndroidElement urlBar;
    
    @AndroidFindBy(id = "backGrdText_Id")
    public AndroidElement pageBlocked;
    
    //Chrome lock icon when https connection is successful
    @AndroidFindBy(id = "com.android.chrome:id/location_bar_status_icon")
    public AndroidElement chromeLockIcon;

   

	
}
