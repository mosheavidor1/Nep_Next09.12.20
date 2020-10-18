package Utils.Data;

import java.util.Arrays;

import Actions.LNEActions;
import Tests.GenericTest;
import Utils.PropertiesFile.PropertiesFile;

public class GlobalTools {
	
	private static String clusterToTest;
	private static LNEActions lennyActions;
	private static Boolean portalEnv;
	private static Boolean productionEnv;
	
	public static final String [] environmentsNamesArray = {"qa","inc","stg","ams","apj","emea"};
    
	
	
	public static void setClusterToTest(String testedCluster) {
		clusterToTest = testedCluster;
	}
	
	public static LNEActions getLneActions() {
		if (lennyActions != null) {
			return lennyActions;
		}
		lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), GenericTest.getGeneralData().get("LNE User Name"), 
				GenericTest.getGeneralData().get("LNE Password"), Integer.parseInt(GenericTest.getGeneralData().get("LNE SSH port")));
		return lennyActions;
	}
	
	public static String getClusterToTest(){
		return clusterToTest;
	}
	
	public static boolean isProductionEnv() {
		
		if (productionEnv != null) {
			return productionEnv.booleanValue();
		}
		
    	try {
    		productionEnv = clusterToTest.contains("-");
		}
		catch (Exception e) {
			org.testng.Assert.fail("Failed to check whther cluster to test is production env");
			productionEnv = false;
		}
    	return productionEnv;
	}
	
	public static boolean isPortalEnv()  {
		
		if (portalEnv != null) {
			return portalEnv.booleanValue();
		}
    	try {
    		portalEnv = Arrays.asList(environmentsNamesArray).contains(clusterToTest.trim().toLowerCase());
		}
		catch (Exception e) {
			org.testng.Assert.fail("Failed to check whther cluster to test is production env");
			portalEnv = false;
		}
    	return portalEnv;

	}
	
	public static boolean isPortalEnvironment(String value) {
		return Arrays.asList(environmentsNamesArray).contains(value.trim().toLowerCase());
	}

}
