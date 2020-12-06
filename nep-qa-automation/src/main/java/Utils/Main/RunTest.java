package Utils.Main;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import Tests.Environments.ClientLogToPortalTest;
import Tests.GenericTest;
import Utils.Data.Endpoint;
import Utils.Data.Excel;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.TestFiles;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.collections.Lists;

import Actions.DsMgmtActions;


public class RunTest {
	public static final String suitesFolder = "src/main/java/TestSuites/";
	public static final String windowsIdentifier = "win";
	public static final String msiIdentifier = "msi";
	public static final String linuxIdentifier = "lnx";
	public static final String ubuntuIdentifier = "ubu";
	private static final String localhost = "127.0.0.1";
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
			throw new IllegalStateException("Test failed, missing test suite name in arguments. Throwing exception for Jenkins to catch.");

		//get cluster to test name
		if (args.length > 1){
			clusterToTest = args[1];
			GlobalTools.setClusterToTest(clusterToTest);
			if (GlobalTools.isPortalEnv() || GlobalTools.isProductionEnv()) {
				new DsMgmtActions(localhost);
			} else {
				new DsMgmtActions(clusterToTest);
			}
		}
		else
			throw new IllegalStateException("Test failed, missing cluster to test in arguments. Throwing exception for Jenkins to catch.");


		int startingEpParams = 2;

		if(args.length>2){
			String param = args[2];
			if(param.toLowerCase().contains("waitforevents")){
				param=param.toLowerCase().replace("waitforevents","");
				ClientLogToPortalTest.longWaitEE= Integer.parseInt(param);
				startingEpParams++;

			}
		}
		
		List<Endpoint> list = new ArrayList<Endpoint>();

		for (int i=startingEpParams; i < args.length; i++){
			String epDetails = args[i];
			String[] arr = epDetails.split("]");
			Endpoint currentEP = new Endpoint();
			if (arr.length > 1) {
				if (arr[0].compareToIgnoreCase(windowsIdentifier) ==0 || arr[0].compareToIgnoreCase(linuxIdentifier) ==0
						|| arr[0].compareToIgnoreCase(msiIdentifier) ==0 || arr[0].compareToIgnoreCase(ubuntuIdentifier) ==0){
					currentEP.type=arr[0];
				}
				else{
					String error = "could not find machine type identifier at the following parameter: " + epDetails + " Valid identifiers expected: " + windowsIdentifier + " " +linuxIdentifier + " " + msiIdentifier + " " + ubuntuIdentifier;
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

		if (!GlobalTools.isPortalEnv() && !GlobalTools.isProductionEnv()) {
			GlobalTools.getLneActions().Close();
		}

		
		// if a test failed throw exception for Jenkins to catch
		int status = testng.getStatus();
		if (status != 0) {
			throw new IllegalStateException("Test failed. Throwing exception for Jenkins to catch. TestNG error code: " + status);
		}

	}
	public static String getLocalp12Name(String customerId) {
		return "/endpoint-111-" + customerId + ".111.p12";
	}
	public static String getLocalCaName() {
		return "ca.jks";
	}
}
