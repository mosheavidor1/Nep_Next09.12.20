package Actions;

import java.net.MalformedURLException;

import org.testng.Assert;

import Applications.AndroidApplication;
import Pages.Mobile.ChromeMainPage;
import Pages.Mobile.LoginPage;
import Pages.Mobile.PingIdentityPage;
import Pages.Mobile.SuccessPopUp;
import Pages.Mobile.SwgMainPage;
import Pages.Mobile.SystemMessagePage;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;

public class MobileActions {
	
	private static AndroidApplication application;
	
	
	public void InstallTrustwaveApp(String companyID, String userName, String password, String expectedStatus, String statusWaitTime) throws Exception {
		
		LoginPage login = new LoginPage();
		login.companyID.sendKeys(companyID);
		login.nextButton.click();
		
		PingIdentityPage ping = new PingIdentityPage();
		ping.userName.sendKeys(userName);
		
		application.HideKeyboard();
		ping.password.sendKeys(password);
		application.HideKeyboard();
		
		ping.signInButton.click();
		
		SuccessPopUp pop = new SuccessPopUp();
		pop.okButton.click();
		
		SystemMessagePage sys = new SystemMessagePage();
		
		sys.okButton.click();
		//Select certificate message does not always accepts Appium click  - use workaround function
		sys.okButton.ClickIfExists();
		
		SendDevicePinKeys();
		
		sys.okButton.click();
		
		SendDevicePinKeys();

		sys.okButton.click();
		
		pop.okButton.click();
		
		//Create connection message do not always appear - if it appears click it
		sys.okButton.ClickIfExists();
		
		SwgMainPage main = new SwgMainPage();
		main.statusButton.WaitUntilTextBe(expectedStatus, Integer.valueOf(statusWaitTime));
		Assert.assertEquals(main.statusButton.getText(), expectedStatus);

	}
	
	public void ReconnectSwgApp(String expectedStatus, String statusWaitTime) {
	
		SwgMainPage main = new SwgMainPage();
		main.reconnectButton.click();
		main.statusButton.WaitUntilTextBe(expectedStatus, Integer.valueOf(statusWaitTime));
		Assert.assertEquals(main.statusButton.getText(), expectedStatus);
		
	}
	
	public void ChromeOpenSite(String SafeSiteUrl) {
		  ChromeMainPage chromeMain = new ChromeMainPage();
		  chromeMain.menuButton.click();
		  chromeMain.chromeIncognito.click();
		  chromeMain.urlBar.sendKeys(SafeSiteUrl+"\n");

	}
	public void OpenSafeSite( String SafeSiteUrl) {
		  ChromeMainPage chromeMain = new ChromeMainPage();
		  ChromeOpenSite(SafeSiteUrl);
		  //Assert.assertEquals(chromeMain.googleLogo.isDisplayed(),true,"Safe site did not load");
		  Assert.assertEquals(chromeMain.chromeLockIcon.isDisplayed(),true,"Safe site did not load");
		  
		  

	}
	
	public void OpenMaliciousSite( String maliciousSiteUrl) {
		  ChromeMainPage chromeMain = new ChromeMainPage();
		  ChromeOpenSite(maliciousSiteUrl);
	     Assert.assertEquals(chromeMain.pageBlocked.getText(), "Page Blocked");

	}

	
	
	
	public void SendDevicePinKeys() {
    	AndroidDriver<AndroidElement> driver =AndroidApplication.GetDriver();

		KeyEvent key = new KeyEvent(AndroidKey.NUMPAD_1);
		driver.pressKey(key);
		key = new KeyEvent(AndroidKey.NUMPAD_2);
		driver.pressKey(key);
		key = new KeyEvent(AndroidKey.NUMPAD_3);
		driver.pressKey(key);
		key = new KeyEvent(AndroidKey.NUMPAD_4);
		driver.pressKey(key);
		key = new KeyEvent(AndroidKey.ENTER);
		driver.pressKey(key);

	}
	
	public MobileActions() {
		application = AndroidApplication.GetInstance();
	}

	
    public void LaunchApplication(String deviceName, String version, String apkPath, String appPackage, String appActivity, String resetTrueOrFalse ) throws MalformedURLException
    {
    	application.Launch( deviceName,  version,  apkPath,  appPackage,  appActivity,  resetTrueOrFalse );
    }

    public void StartActivity( String appPackage, String appActivity ) 
    {
    	application.StartActivity(  appPackage,  appActivity );
    }

    public void RemoveApp( String appPackage, String deviceName, String androidVersion ) throws MalformedURLException 
    {
    	application.RemoveApp(  appPackage, deviceName, androidVersion);
    }

    public void CloseDriver() {
    	AndroidApplication.CloseDriver();
    }
}
