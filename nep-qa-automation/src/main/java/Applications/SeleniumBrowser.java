package Applications;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;

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
	public void Launch(String applicationType, String proxyIP , boolean loadProxyExtension)  {
		try {
			JLog.logger.debug("Selenium browser Launch - Before switch application type: " + applicationType);
			switch (applicationType) {
				case "Chrome":
					System.setProperty("webdriver.chrome.driver", "C:\\Selenium\\chromedriver.exe");
					JLog.logger.debug("Selenium browser Launch - After set chrome property");

					if (proxyIP.compareTo("") != 0) {
						ChromeOptions options = new ChromeOptions();
						org.openqa.selenium.Proxy proxy = new org.openqa.selenium.Proxy();

						proxy.setHttpProxy(proxyIP + ":8080");
						proxy.setSslProxy(proxyIP + ":8443");

						proxy.setNoProxy("localhost; 127.0.0.1; <local>;*.pingidentity.com;login.trustwave.com;*.pingone.com");
						options.setCapability("proxy", proxy);
						if (loadProxyExtension) {
							//location of "Proxy Auto Auth" extension which inserts proxy user name and password automatically
							//https://chrome.google.com/webstore/detail/proxy-auto-auth/ggmdpepbjljkkkdaklfihhngmmgmpggp?hl=en
							String proxyAutoAuth = "C:\\Selenium\\Utils\\extension_2_0.crx";
							File extension = new File(proxyAutoAuth);
							if (!extension.exists())
								org.testng.Assert.fail("Could not find Proxy Auto Extension. Please copy it to the following location: " + proxyAutoAuth);

							options.addExtensions(extension);
						}


						driver = new ChromeDriver(options);

					} else {
						HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
						JLog.logger.debug("Selenium browser Launch - After creating new hashMap");

						// This Chrome Preference is needed to not block download of exe files
						chromePrefs.put("safebrowsing.enabled", "false");

						String downloadFilepath = PropertiesFile.getManagerDownloadFolder();
						//Chrome does not accept / delimiter. Therefore replacing it with \ delimiter
						downloadFilepath = downloadFilepath.replace("/","\\");
						JLog.logger.debug("Selenium browser Launch - After reading MasterDownloadFolder property: " + downloadFilepath);

						chromePrefs.put("download.default_directory", downloadFilepath);
						JLog.logger.debug("Selenium browser Launch - After put to hash" );

						ChromeOptions options = new ChromeOptions();
						JLog.logger.debug("Selenium browser Launch - After new chrome options" );

						options.setExperimentalOption("prefs", chromePrefs);
						JLog.logger.debug("Selenium browser Launch - After chrome setExperimentalOption" );

						JLog.logger.debug("Selenium browser Launch - Before setting several chrome options" );

						options.addArguments("start-maximized"); // https://stackoverflow.com/a/26283818/1689770
						options.addArguments("enable-automation"); // https://stackoverflow.com/a/43840128/1689770
						options.addArguments("--no-sandbox"); //https://stackoverflow.com/a/50725918/1689770
						options.addArguments("--disable-infobars"); //https://stackoverflow.com/a/43840128/1689770
						options.addArguments("--disable-dev-shm-usage"); //https://stackoverflow.com/a/50725918/1689770
						options.addArguments("--disable-browser-side-navigation"); //https://stackoverflow.com/a/49123152/1689770
						options.addArguments("--disable-gpu"); //https://stackoverflow.com/questions/51959986/how-to-solve-selenium-chromedriver-timed-out-receiving-message-from-renderer-exc

						JLog.logger.debug("Selenium browser Launch - Before creating new chrome driver" );


						driver = new ChromeDriver(options);
						JLog.logger.debug("Selenium browser Launch - After creating new chrome driver" );


					}

					break;


				case "Firefox":
					System.setProperty("webdriver.gecko.driver", "C:\\Selenium\\geckodriver.exe");

					if (proxyIP.compareTo("") != 0) {

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

					} else {
						driver = new FirefoxDriver();
					}
					break;

				case "IE":
					File file = new File("C:/Selenium/IEDriverServer.exe");
					System.setProperty("webdriver.ie.driver", file.getAbsolutePath());

					if (proxyIP.compareTo("") != 0) {

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
			JLog.logger.debug("After Setting implicit timeout" );
			driver.manage().window().maximize();
			JLog.logger.debug("After maximizing window " );

		}
		catch (Exception e) {
			org.testng.Assert.fail("Could load the following browser: " + applicationType + "\n" + e.toString());
		}


	}

	public static void ChangeImplicitWait(int seconds){
		try {
			if (seconds == 0)
				seconds = implicitWait;
			if (driver != null)
				driver.manage().timeouts().implicitlyWait(seconds, TimeUnit.SECONDS);
		}
		catch (Exception e) {
			org.testng.Assert.fail("Could not set Selenium driver implicit wait time. " + "\n" + e.toString());
		}

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
	public void Launch(String applicationType)  {
		this.Launch(applicationType, "", false);
		

	}

	@Override
	public void LoadUrl(String URL) {
		try {
			JLog.logger.debug("LoadURL: Before check if driver equals to null " );

			if (driver == null)
				return;
			JLog.logger.debug("LoadURL: After check if driver equals to null " );

			if (!URL.startsWith("http"))
				URL = "http://" + URL;
			JLog.logger.debug("LoadURL: URL =  " + URL);

			driver.get(URL);
			JLog.logger.debug("LoadURL: after get URL =  " + URL);

		}
		catch (Exception e) {
			org.testng.Assert.fail("Could not load URL at browser. " + "\n" + e.toString());
		}


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
