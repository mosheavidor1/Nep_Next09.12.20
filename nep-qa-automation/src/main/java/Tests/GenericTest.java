package Tests;

import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

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
	protected static HashMap<String, String> general=null;
	protected VideoCapture video;
	protected String screenShot;
	public static final String generalSettingsIdentifier = "General Settings";

	@SuppressWarnings("unchecked")
	public GenericTest(Object dataToSet) {
		data = (HashMap<String, String>) dataToSet;
		screenShot = "";

	}

	@DataProvider(name = "getData")
	public static Object[] getDataForInstances(ITestNGMethod m)  {

		String fullTestName = m.getConstructorOrMethod().getName(); //m.getMethodName();
		String[] arr = fullTestName.split("\\.");
		String sheetName = arr[arr.length - 1];
		String fileName = PropertiesFile.readProperty("Excel.fileLocation");
		fileName = RunTest.runAtDirectory + fileName;
		Excel testData = new Excel();
		boolean excelFileStatus = testData.setExcelFileAndSheet(fileName, sheetName);
		Object[] getTestData = null;
		if (excelFileStatus)
			getTestData = testData.getTestData();

		if (getTestData == null)
			JLog.logger.error("Could not find Excel sheet for this test.");

		//if it is the first time general settings were not been read. read it now.
		if (general == null ) {

			Excel generalSettings = new Excel();

			excelFileStatus = generalSettings.setExcelFileAndSheet(fileName, generalSettingsIdentifier);
			Object[] getGenericSettings = null;
			if (excelFileStatus) {
				getGenericSettings = generalSettings.getTestData();
				general = (HashMap<String, String>) getGenericSettings[0];
			}
			else
				JLog.logger.error("Could not find General Settings Excel sheet");//as there is an exception when error occurs at Excel class this code currently unreachable. Leave it if future design will allow no General settings

		}

		return getTestData;

	}

}



