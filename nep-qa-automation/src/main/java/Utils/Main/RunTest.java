package Utils.Main;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import Utils.Data.Endpoint;
import Utils.Data.Excel;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.collections.Lists;


public class RunTest {
	public static final String suitesFolder = "src/main/java/TestSuites/";
	public static final String windowsIdentifier = "win";
	public static final String linuxIdentifier = "lnx";
	public static String runAtDirectory ="";

	public static void main(String[] args) throws URISyntaxException {

		String currentSuite="";
		String clusterToTest="";
		
		//if Running from jar file get the jar file path
		String jarDir = new File(RunTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
		if(jarDir.contains(".jar")) {
			int end = jarDir.lastIndexOf(File.separator);
			runAtDirectory = jarDir.substring(0,end+1);
		}

		//get test suite file name
		if (args.length > 0)
			currentSuite = args[0];
		else
			currentSuite = "DownloadInstallAndSendLogs.xml";

		//get cluster to test name
		if (args.length > 1){
			clusterToTest = args[1];
			PropertiesFile.writeProperty("ClusterToTest",clusterToTest.toLowerCase() );
			PropertiesFile.saveFile("Set cluster to test: " + clusterToTest);
		}

		List<Endpoint> list = new ArrayList<Endpoint>();

		for (int i=2; i < args.length; i++){
			String epDetails = args[i];
			String[] arr = epDetails.split("]");
			Endpoint currentEP = new Endpoint();
			if (arr.length > 1) {
				if (arr[0].compareToIgnoreCase(windowsIdentifier) ==0 || arr[0].compareToIgnoreCase(linuxIdentifier) ==0){
					currentEP.type=arr[0];
				}
				else{
					String error = "could not find machine type identifier at the following parameter: " + epDetails + " Valid identifiers expected: " + windowsIdentifier + " " +linuxIdentifier;
					JLog.logger.error(error);
					throw new IllegalStateException(error);
				}
				currentEP.hostName = arr[1];
				if (arr.length > 2){
					currentEP.userName = arr[2];
					if (arr.length > 3){
						currentEP.password = arr[3];
					}
				}
			}
			else {
				String error = "could not find delimiter ] at the following endpoint details parameter: " + epDetails ;
				JLog.logger.error(error);
				throw new IllegalStateException(error);
			}
			list.add(currentEP);
		}

		Excel.epList =list;

		TestListenerAdapter tla = new TestListenerAdapter();
		TestNG testng = new TestNG();
		List<String> suites = Lists.newArrayList();
		suites.add(runAtDirectory+ suitesFolder + currentSuite);//path to xml..
		testng.setTestSuites(suites);
        testng.addListener(tla);
		testng.run();
		
		// if a test failed throw exception for Jenkins to catch
		int status = testng.getStatus();
		if (status != 0) {
			throw new IllegalStateException("Test failed. Throwing exception for Jenkins to catch. TestNG error code: " + status);
		}

	}

}
