package Utils.Main;

import java.io.File;
import java.util.List;

import Utils.PropertiesFile.PropertiesFile;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.collections.Lists;


public class RunTest {
	public static final String suitesFolder = "nep-qa-automation/src/main/java/TestSuites/NEP/";
	public static String runAtDirectory ="";

	public static void main(String[] args) throws Exception {

		String currentSuite="";
		String clusterToTest="";
		
		//if Running from jar file get the jar file path
		String jarDir = new File(RunTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
		if(jarDir.contains(".jar")) {
			int end = jarDir.lastIndexOf("\\");
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

		//get cluster to test name
		if (args.length > 2){
			String action = args[2].toUpperCase();
			PropertiesFile.writeProperty("Action",action );
			PropertiesFile.saveFile("Set action to test: " + action);
		}


		TestListenerAdapter tla = new TestListenerAdapter();
		TestNG testng = new TestNG();
		List<String> suites = Lists.newArrayList();
		suites.add(runAtDirectory+ suitesFolder + currentSuite);//path to xml..
		testng.setTestSuites(suites);
        testng.addListener(tla);
		testng.run();
		
		// if exit code is needed this code will help
		/*int status = testng.getStatus();
		if (status != 0) {
			//System.exit(status);
			throw new IllegalStateException("TestNG exited with error code: " + status);
		}*/



	}

}
