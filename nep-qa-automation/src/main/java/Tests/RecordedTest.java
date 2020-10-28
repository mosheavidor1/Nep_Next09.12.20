package Tests;

import Applications.SeleniumBrowser;
import Utils.Logs.JLog;
import Utils.TestNG.InvokedMethodListener;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordedTest extends GenericTest{

	public RecordedTest(Object dataToSet) {
		super(dataToSet);
	}

	@BeforeMethod
	public void BeforeMethod() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("-dd-MM-yyyy-HH.mm.ss");
		Date date = new Date();
		//remove video creation just to check if that is damaging select customer
		/*video = new VideoCapture(".\\test-output\\Capture", this.getClass().getSimpleName() + " " + dateFormat.format(date));
		try {
			//video.startRecording();
		} catch (Exception e) {
			JLog.logger.warn("Could not start video recording. If multiple screens are used do not change the browser location to avoid this issue.");
		}*/
		String captureFilesPrefix = System.getProperty("user.dir") + "/test-output/capture/" + this.getClass().getSimpleName() +  dateFormat.format(date);
		screenShot = captureFilesPrefix + ".png";
		JLog.logger.debug("screenshot file: "+ screenShot );
		InvokedMethodListener.screenShot = screenShot;
		InvokedMethodListener.video = captureFilesPrefix + ".avi";

	}


	@AfterMethod
	public void afterMethod() throws Exception {
			//video.stopRecording();
			if (SeleniumBrowser.InstanceExist()) {
				File scrFile = ((TakesScreenshot) SeleniumBrowser.GetDriver()).getScreenshotAs(OutputType.FILE);
				FileUtils.copyFile(scrFile, new File(screenShot));
			}



	}
}



