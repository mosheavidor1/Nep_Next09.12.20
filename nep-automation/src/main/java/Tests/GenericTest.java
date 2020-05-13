package Tests;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import Actions.WikiActions;
import Applications.SeleniumBrowser;
import Utils.Main.RunTest;
import Utils.Capture.VideoCapture;
import Utils.TestNG.InvokedMethodListener;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestNGMethod;
import org.testng.annotations.*;
import Utils.Data.Excel;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

@Listeners(InvokedMethodListener.class)
public class GenericTest {

	protected HashMap<String, String> data;
	protected VideoCapture video;
	private String screenShot;
	protected WikiActions wikiAction;

	@SuppressWarnings("unchecked")
	public GenericTest(Object dataToSet) {
		data = (HashMap<String, String>) dataToSet;
		screenShot = "";

	}


	@DataProvider(name = "getData")
	public static Object[] getDataForInstances(ITestNGMethod m) throws Exception {

		String fullTestName = m.getConstructorOrMethod().getName(); //m.getMethodName();
		String[] arr = fullTestName.split("\\.");
		String sheetName = arr[arr.length - 1];
		String fileName = PropertiesFile.readProperty("Excel.fileLocation");
		fileName = RunTest.runAtDirectory + fileName;
		boolean excelFileStatus = Excel.setExcelFileAndSheet(fileName, sheetName);
		Object[] getTestData = null;
		if (excelFileStatus)
			getTestData = Excel.getTestData();

		if (getTestData == null)
			JLog.logger.error("Could not find Excel sheet for this test.");

		return getTestData;

	}

	@BeforeMethod
	public void BeforeMethod() throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH.mm.ss");
		Date date = new Date();
		video = new VideoCapture(".\\test-output\\Capture", this.getClass().getSimpleName() + " " + dateFormat.format(date));

		video.startRecording();
		String captureFilesPrefix = System.getProperty("user.dir") + "\\test-output\\Capture\\" + this.getClass().getSimpleName() + " " + dateFormat.format(date);
		screenShot = captureFilesPrefix + ".png";
		InvokedMethodListener.screenShot = screenShot;
		InvokedMethodListener.video = captureFilesPrefix + ".avi";

	}


	@AfterMethod
	public void afterMethod() throws Exception {
		video.stopRecording();

		if (SeleniumBrowser.InstanceExist()) {

			File scrFile = ((TakesScreenshot) SeleniumBrowser.GetDriver()).getScreenshotAs(OutputType.FILE);

			FileUtils.copyFile(scrFile, new File(screenShot));


		}


	}
}



