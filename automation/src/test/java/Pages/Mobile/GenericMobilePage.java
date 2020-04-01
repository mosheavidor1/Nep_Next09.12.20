package Pages.Mobile;

import org.openqa.selenium.support.PageFactory;

import Applications.AndroidApplication;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;

public class GenericMobilePage {
	public static final String chromePackage = "com.android.chrome";
	public static final String chromeActivity = "com.google.android.apps.chrome.Main";

	protected AndroidDriver<AndroidElement> driver;
	
    public GenericMobilePage()
    {
        driver = AndroidApplication.GetDriver();
        PageFactory.initElements(new AppiumFieldDecorator(driver), this);

    }


}
