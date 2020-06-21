package Utils.PropertiesFile;


import java.io.*;
import java.util.Arrays;
import java.util.Properties;

import Utils.Main.RunTest;

public  class PropertiesFile {
	//reads test properties from config.properties file

	    private static Properties properties;
	    private static InputStream inputStream = null;
	    private static PropertiesFile file=null;
	    private static String filePath = "nep-qa-automation/src/main/java/utils/PropertiesFile/config.properties";
	    public static final String [] environmentsNamesArray = {"qa","inc","stg","ams","apj","emea"};
	    

	    private PropertiesFile() {
	    	filePath = RunTest.runAtDirectory +filePath;
	        properties = new Properties();
	        loadProperties();
	    }

	    private static void loadProperties() {
			try {
				inputStream = new FileInputStream(filePath);
            	properties.load(inputStream);
			}
        	catch (Exception e){
				org.testng.Assert.fail("Could not load properties file: " + filePath + "\n" + e.toString());
	    	}

		}

	    public static String readProperty(String key) {
	    	try {
				if (file == null)
					file = new PropertiesFile();
				return properties.getProperty(key);
			}
	    	catch (Exception e){
				org.testng.Assert.fail("Could not read property:" + key + "  from properties file: " + filePath + "\n" + e.toString());
				return null;
			}


		}

	    public static void writeProperty(String key, String value) {
	    	try {
				if (file == null)
					file = new PropertiesFile();
				properties.setProperty(key, value);
			}
			catch (Exception e){
				org.testng.Assert.fail("Could not write property key: " + key + " property value: " + value +  "  to properties file: " + filePath + "\n" + e.toString());
			}

		}

		public static void saveFile(String comment) {
			try {
				properties.store(new FileOutputStream(filePath),comment);
			} catch (Exception e) {
				org.testng.Assert.fail("Could not save properties file: " + filePath + "\n" + e.toString());
			}
		}

		public static String getCurrentClusterLB () {
			try {
				String current = readProperty("ClusterToTest");
				if (isEnvironments() || isProduction())
					return readProperty(current);
				else
					return current;

			}
			catch (Exception e) {
				org.testng.Assert.fail("Could not get current cluster LB: " + filePath + "\n" + e.toString());
				return  null;
			}


		}

		public static boolean isProduction()  {
	    	try {
				String current = readProperty("ClusterToTest");
				if (current.contains("-"))
					return true;
				else
					return false;
			}
			catch (Exception e) {
				org.testng.Assert.fail("Could not check cluster to test at properties file: " + filePath + "\n" + e.toString());
				return  false;
			}

		}


		public static boolean isEnvironments()  {
	    	try {
				String current = readProperty("ClusterToTest");
				current = current.trim().toLowerCase();
				return isEnvironment(current);
			}
			catch (Exception e) {
				org.testng.Assert.fail("Could not check cluster to test at properties file: " + filePath + "\n" + e.toString());
				return  false;
			}

		}

		public static boolean isEnvironment(String isThisStringEnvironment){
	    	try {
				return Arrays.asList(environmentsNamesArray).contains(isThisStringEnvironment.trim().toLowerCase());
			}
			catch (Exception e) {
				org.testng.Assert.fail("Could not check if this is an environment name: " + isThisStringEnvironment + "\n" + e.toString());
				return  false;
			}

		}



	//legacy function should be replaced for SWG tests with Excel general data
		public static String getCurrentClusterLink () throws IOException {
			String current = readProperty("ClusterToTest");
			if(isProduction())
				return readProperty("productionLink");
			else
				return readProperty(current+"Link");
		}

		//legacy function
		public static String getCurrentClusterNepHost () throws IOException {
			String hostName = readProperty("NepEnvironmentsHostName");
			String cluster = readProperty("ClusterToTest");

			if(! isProduction()) {
				hostName = hostName.replace("XXX", cluster);
				return hostName;
			}
			else
				return "";
		}

	//legacy function should be replaced for SWG tests with Excel general data

	public static String getUserName() throws IOException {
		if(isProduction())
			return readProperty("UserName-Production");
		else
			return readProperty("UserName-Environments");

	}

	//legacy function should be replaced for SWG tests with Excel general data
	public static String getPassword() throws IOException {
		if(isProduction())
			return readProperty("Password-Production");
		else
			return readProperty("Password-Environments");

	}

	//legacy function
	public static String getCustomerName() throws IOException {
		if(isProduction())
			return readProperty("CustomerName-Production");
		else
			return readProperty("CustomerName-Environments");

	}

	//legacy function should be replaced for SWG tests with Excel general data
	public static String getCustomerNameOrID() throws IOException {
		if(isProduction())
			return readProperty("CustomerID-Production");
		else
			return readProperty("CustomerName-Environments");

	}






}
