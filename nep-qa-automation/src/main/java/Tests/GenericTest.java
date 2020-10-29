package Tests;

import java.util.HashMap;

import Utils.Main.RunTest;
import Utils.NepDbConnector;
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
	private static HashMap<String, String> general = null;
	private static HashMap<String, String> configurations = null;
	protected VideoCapture video;
	protected String screenShot;
	public static final String generalSettingsIdentifier = "General Settings";
	public static final String configurationsIdentifier = "Configurations";
	private static NepDbConnector dbConnector ;

	@SuppressWarnings("unchecked")
	public GenericTest(Object dataToSet) {
		data = (HashMap<String, String>) dataToSet;
		screenShot = "";

	}
	
	public static NepDbConnector getDbConnector() {
		if (dbConnector == null) {
			dbConnector = new NepDbConnector(general.get("DB URL"), general.get("DB user"), general.get("DB password"));
		}
		return dbConnector;
	}

	@DataProvider(name = "getData")
	public static Object[] getDataForInstances(ITestNGMethod m) throws Exception {

		String fullTestName = m.getConstructorOrMethod().getName(); //m.getMethodName();
		String[] arr = fullTestName.split("\\.");
		String sheetName = arr[arr.length - 1];
		String fileName = RunTest.runAtDirectory + PropertiesFile.readProperty("Excel.fileLocation");
		Excel testData = new Excel(fileName, sheetName);
		Object[] getTestData = testData.getTestData();

		if (getTestData == null)
			JLog.logger.error("Could not find Excel sheet for this test: " +sheetName);

		//if it is the first time general settings were not been read. read it now.
		return getTestData;

	}

	/**
	 * Returns data from the GeneralSettings excel sheet 
	 * @return
	 */
	public static HashMap<String, String> getGeneralData() {
		if (general != null ) {
			return general;
		}
		
		general = getData(generalSettingsIdentifier);
		return general;
	}
	
	/**
	 * Returns data from the Configurations excel sheet 
	 * @return
	 */
	public static HashMap<String, String> getConfigurations() {
		if (configurations != null ) {
			return configurations;
		}
		
		configurations = getData(configurationsIdentifier);
		return configurations;
	}
	
	/**
	 * Returns data from the excel sheet with dataIdentifier name
	 * 
	 * @param dataIdentifier
	 * @return
	 */
	public static HashMap<String, String> getData(String dataIdentifier) {
		
		Excel generalSettings = new Excel(RunTest.runAtDirectory + PropertiesFile.readProperty("Excel.fileLocation"), dataIdentifier);

		Object[] getGenericSettings = generalSettings.getTestData();
		if (getGenericSettings == null) {
			JLog.logger.error("Could not find General Settings Excel sheet");//as there is an exception when error occurs at Excel class this code currently unreachable. Leave it if future design will allow no General settings
			return null;
		}
		return (HashMap<String, String>) getGenericSettings[0];
	}

}



