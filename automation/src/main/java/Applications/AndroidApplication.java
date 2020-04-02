package Applications;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.remote.DesiredCapabilities;

import Pages.Mobile.GenericMobilePage;
import io.appium.java_client.android.Activity;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.remote.MobileCapabilityType;

public class AndroidApplication   {
	
	private static AndroidDriver<AndroidElement> driver;
	private static AndroidApplication app = null;

	private AndroidApplication () {

	}
	
	public static AndroidApplication GetInstance() {
		if(app == null)
			app = new AndroidApplication();
		return app;

	}
	public static AndroidDriver<AndroidElement> GetDriver() {
		return driver;
	}
	
	public static void CloseDriver() {
		driver.quit();
	}

	public void HideKeyboard() {
		driver.hideKeyboard();
	}
	public void Launch (String deviceName, String version, String apkPath, String appPackage, String appActivity, String resetTrueOrFalse ) throws MalformedURLException {
		DesiredCapabilities capabilities = new DesiredCapabilities();

		if(apkPath.compareTo("")!=0) {
			File classpathRoot = new File(System.getProperty("user.dir"));
			//File appDir = new File(classpathRoot, "/Apps/");
			//File app = new File(appDir, apkPath);
			File app = new File(classpathRoot, apkPath);
			capabilities.setCapability(MobileCapabilityType.APP, app.getAbsolutePath());
		}

		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, deviceName);
		capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, version);
		capabilities.setCapability(MobileCapabilityType.APPLICATION_NAME, "Android");
		capabilities.setCapability(MobileCapabilityType.NO_RESET, resetTrueOrFalse);
		capabilities.setCapability("appPackage", appPackage);
		capabilities.setCapability("appActivity", appActivity);
		driver = new AndroidDriver<AndroidElement>(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);
		driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);

	}

	public void StartActivity (String appPackage, String appActivity) {
	  	Activity activity = new Activity(appPackage, appActivity);
		driver.startActivity(activity);
	}
	

	public void RemoveApp (String appPackage,String deviceName, String androidVersion) throws MalformedURLException {
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, deviceName);
		capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, androidVersion);
		capabilities.setCapability(MobileCapabilityType.APPLICATION_NAME, "Android");
		capabilities.setCapability(MobileCapabilityType.NO_RESET, "true");
		capabilities.setCapability("appPackage", GenericMobilePage.chromePackage);
		capabilities.setCapability("appActivity", GenericMobilePage.chromeActivity);
		driver = new AndroidDriver<AndroidElement>(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);
	    driver.removeApp(appPackage);
	    driver.quit();
	}

	



}
