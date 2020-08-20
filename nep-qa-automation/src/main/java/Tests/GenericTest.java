package Tests;

import java.util.HashMap;

import Utils.Main.RunTest;
import Utils.Capture.VideoCapture;
import Utils.TestNG.InvokedMethodListener;
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
	public static Object[] getDataForInstances(ITestNGMethod m) throws Exception {

		String fullTestName = m.getConstructorOrMethod().getName(); //m.getMethodName();
		String[] arr = fullTestName.split("\\.");
		String sheetName = arr[arr.length - 1];
		String fileName = PropertiesFile.readProperty("Excel.fileLocation");
		fileName = RunTest.runAtDirectory + fileName;
		Excel testData = new Excel(fileName,sheetName);
		Object[] getTestData = testData.getTestData();

		if (getTestData == null)
			JLog.logger.error("Could not find Excel sheet for this test: " +sheetName);

		//if it is the first time general settings were not been read. read it now.
		if (general == null ) {

			Excel generalSettings = new Excel(fileName, generalSettingsIdentifier);

			Object[] getGenericSettings = generalSettings.getTestData();
			if (getGenericSettings == null) {
				JLog.logger.error("Could not find General Settings Excel sheet");//as there is an exception when error occurs at Excel class this code currently unreachable. Leave it if future design will allow no General settings
			}
			else {
				general = (HashMap<String, String>) getGenericSettings[0];
			}


		}

		return getTestData;

	}

}



