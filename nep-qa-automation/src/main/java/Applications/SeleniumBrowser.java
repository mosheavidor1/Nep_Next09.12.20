package Applications;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import Utils.PropertiesFile.PropertiesFile;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
//import org.openqa.selenium.ie.InternetExplorerDriver;
//import org.openqa.selenium.ie.InternetExplorerOptions;
//import org.openqa.selenium.remote.CapabilityType;
//import org.openqa.selenium.remote.DesiredCapabilities;

public class SeleniumBrowser implements Application {

	public static final int implicitWait = 120;
	private static SeleniumBrowser browser = null;
	private static WebDriver driver = null;
	
	private SeleniumBrowser() {
		
	}
	
	public static SeleniumBrowser GetInstance()
	{
		if(browser == null)
			browser = new SeleniumBrowser();

		return browser;
	}
	
	public static WebDriver GetDriver() {
		return driver;
	}

	
	//@SuppressWarnings("deprecation")
	@Override
	public void Launch(String applicationType, String proxyIP , boolean loadProxyExtension) throws IOException {
		String exePath;
        switch (applicationType)
        {
            case "Chrome":
				System.setProperty("webdriver.chrome.driver", "C:\\Selenium\\chromedriver.exe");

				if (proxyIP.compareTo("") !=0) {
					ChromeOptions options = new ChromeOptions();
    				org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();

    				proxy.setHttpProxy(proxyIP + ":8080");
    				proxy.setSslProxy(proxyIP + ":8443");

    				proxy.setNoProxy("localhost; 127.0.0.1; <local>;*.pingidentity.com;login.trustwave.com;*.pingone.com");
					options.setCapability("proxy", proxy);
					if(loadProxyExtension) {
						//location of "Proxy Auto Auth" extension which inserts proxy user name and password automatically
						//https://chrome.google.com/webstore/detail/proxy-auto-auth/ggmdpepbjljkkkdaklfihhngmmgmpggp?hl=en
						String proxyAutoAuth = "C:\\Selenium\\Utils\\extension_2_0.crx";
						File extension = new File(proxyAutoAuth);
						if(!extension.exists())
							org.testng.Assert.fail("Could not find Proxy Auto Extension. Please copy it to the following location: " + proxyAutoAuth);

						options.addExtensions(extension);
					}


    				driver = new ChromeDriver(options);

				}
            	else {
					HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
					chromePrefs.put("safebrowsing.enabled", "false");
					//chromePrefs.put("profile.default_content_settings.popups", 0);
					//chromePrefs.put("download.prompt_for_download", "false");
					String downloadFilepath = PropertiesFile.readProperty("DownloadFolder");
					chromePrefs.put("download.default_directory", downloadFilepath);

					ChromeOptions options = new ChromeOptions();
					options.setExperimentalOption("prefs", chromePrefs);

                    driver = new ChromeDriver(options);

            	}
            	
                break;


			case "Firefox":
				System.setProperty("webdriver.gecko.driver", "C:\\Selenium\\geckodriver.exe");

            	if (proxyIP.compareTo("") !=0) {

    				FirefoxProfile profile = new FirefoxProfile();
    				FirefoxOptions options = new FirefoxOptions();
    				profile.setPreference("network.proxy.type", 1);
    				profile.setPreference("network.proxy.http", proxyIP);
    				profile.setPreference("network.proxy.http_port", 8080);
    				profile.setPreference("network.proxy.ssl", proxyIP);
					profile.setPreference("network.proxy.ssl_port", 8443);
					profile.setPreference("network.proxy.no_proxies_on", "localhost, 127.0.0.1, <local>,*.pingidentity.com,login.trustwave.com,*.pingone.com");
    				// options.setHeadless(true);
    				options.setProfile(profile);
    				options.addPreference("security.sandbox.content.level", 5);
    				driver = new FirefoxDriver(options);
    				//driver.manage().deleteAllCookies();

            	}
            	else {
                    driver = new FirefoxDriver();
            	}
                break;
                
            case "IE" :
				File file = new File("C:/Selenium/IEDriverServer.exe");
				System.setProperty("webdriver.ie.driver", file.getAbsolutePath());

       		 	if (proxyIP.compareTo("")!=0) {

    				org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();
    				proxy.setHttpProxy(proxyIP + ":8080");
    				proxy.setSslProxy(proxyIP + ":8443");
    				// proxy.setNoProxy("localhost, 127.0.0.1, *.pingone.com,
    				// *.pingidentity.com, shib19.finjan.com, login.trustwave.com,
    				// inc.tw-test.net, inb.tw-test.net, ina.tw-test.net,
    				// login.windows.net, login.microsoftonline.com,
    				// secure.aadcdn.microsoftonline-p.com, *.oktapreview.com,
    				// *.oktacdn.com");

       		 	}
				//InternetExplorerOptions options = new InternetExplorerOptions();

                break;
                
            default:
                driver = null;
                break;

        }


		driver.manage().timeouts().implicitlyWait(implicitWait, TimeUnit.SECONDS);
        driver.manage().window().maximize();

	}

	public static void ChangeImplicitWait(int seconds){
		if (seconds ==0)
			seconds=implicitWait;
		driver.manage().timeouts().implicitlyWait(seconds, TimeUnit.SECONDS);

	}

	public static boolean InstanceExist ()	{
		if (driver != null)
			{
				try
				{
					return (driver.getWindowHandles() != null); // allways returns true if browser instance exist or thrown error
				}
				catch (Exception e)
				{
					return false;
					// means that browser was closed by user
				}
			}
		return false; // means that it wasn't created yet or was closed by developer programmally
	}


	@Override
	public void Launch(String applicationType) throws IOException {
		this.Launch(applicationType, "", false);
		

	}

	@Override
	public void LoadUrl(String URL) {
		if (driver == null)
			return;
		
	     if ( ! URL.startsWith("http") )
	    	 URL= "http://" + URL;

		driver.get(URL);
		
	}

	@Override
	public void MoveToNewWindow() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void CloseCurrentWindow() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void Close() {
		if(driver != null)
			driver.quit();
	}


}
