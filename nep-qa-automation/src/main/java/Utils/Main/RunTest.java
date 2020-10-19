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

import Tests.LNE.DsMgmtActions;


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
			throw new IllegalStateException("Test failed, missing test suite name in arguments. Throwing exception for Jenkins to catch.");

		//get cluster to test name
		if (args.length > 1){
			clusterToTest = args[1];
			GlobalTools.setClusterToTest(clusterToTest);
			new DsMgmtActions(clusterToTest);//TODO: to adjust to portal environments
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
		
		prepareCustomerCaAndCertificates();
		
		List<Endpoint> list = new ArrayList<Endpoint>();

		for (int i=startingEpParams; i < args.length; i++){
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

		if (!GlobalTools.isPortalEnv() && !GlobalTools.isProductionEnv()) {
			GlobalTools.getLneActions().Close();
		}

		
		// if a test failed throw exception for Jenkins to catch
		int status = testng.getStatus();
		if (status != 0) {
			throw new IllegalStateException("Test failed. Throwing exception for Jenkins to catch. TestNG error code: " + status);
		}

	}
	
	/**
	 * This function copies from Lenny to Manager machine the Root CA and customer certificate.
	 * This preparation is needed so that simulated agent will be able to connect the proxy in order
	 * to send requests to DS
	 * 
	 * In case we run against portal env we assume that the manager already contains the Root CA and certificate 
	 */
	public static void prepareCustomerCaAndCertificates() {
		
		if (GlobalTools.isPortalEnv() || GlobalTools.isProductionEnv()) {
			return;
		}
		
		String LocalCertDirName = PropertiesFile.getManagerDownloadFolder()+ "/" + GlobalTools.getClusterToTest();
		if (!TestFiles.Exists(LocalCertDirName))
			TestFiles.CreateFolder(LocalCertDirName);

		String customerId = GenericTest.getGeneralData().get("Customer Id");
		String LNEclientp12 = GlobalTools.getLneActions().getClientp12Path(customerId);
		String LNEclientCA = GlobalTools.getLneActions().getClientCaPath();
		String Localclientp12 = LocalCertDirName + "/" + getLocalp12Name(customerId);
		String LocalclientCA = LocalCertDirName + "/" + getLocalCaName();
		if (!TestFiles.Exists(Localclientp12))
			GlobalTools.getLneActions().copy2ManagerMachine(LNEclientp12,LocalCertDirName);
		if (!TestFiles.Exists(LocalclientCA))
			GlobalTools.getLneActions().copy2ManagerMachine(LNEclientCA,LocalCertDirName);

		
	}

	public static String getLocalp12Name(String customerId) {
		return "/endpoint-111-" + customerId + ".111.p12";
	}
	public static String getLocalCaName() {
		return "ca.jks";
	}
}
