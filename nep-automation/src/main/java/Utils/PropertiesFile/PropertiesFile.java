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
	    private static String filePath = "src/main/java/utils/PropertiesFile/config.properties";
	    public static final String [] environmentsNamesArray = {"qa","inc","stg"};
	    

	    private PropertiesFile() throws IOException {
	    	filePath = RunTest.runAtDirectory +filePath;
	        properties = new Properties();
	        loadProperties();
	    }

	    private static void loadProperties() throws IOException {
            inputStream = new FileInputStream(filePath);
            properties.load(inputStream);
	    }

	    public static String readProperty(String key) throws IOException {
	    	if(file==null)
	    		file=new PropertiesFile();
	        return properties.getProperty(key);
	    }

	    public static void writeProperty(String key, String value) throws IOException {
			if(file==null)
				file=new PropertiesFile();
			properties.setProperty(key, value);
		}
		public static void saveFile(String comment) throws IOException {
			properties.store(new FileOutputStream(filePath),comment);
		}

		public static String getCurrentClusterLB () throws IOException {
			String current = readProperty("ClusterToTest");
			if (isEnvironments() || isProduction())
				return readProperty(current);
			else
				return current;
		}


		public static String getCurrentClusterLink () throws IOException {
			String current = readProperty("ClusterToTest");
			if(isProduction())
				return readProperty("productionLink");
			else
				return readProperty(current+"Link");
		}

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


	public static String getUserName() throws IOException {
		if(isProduction())
			return readProperty("UserName-Production");
		else
			return readProperty("UserName-Environments");

	}

	public static String getPassword() throws IOException {
		if(isProduction())
			return readProperty("Password-Production");
		else
			return readProperty("Password-Environments");

	}
	public static String getCustomerName() throws IOException {
		if(isProduction())
			return readProperty("CustomerName-Production");
		else
			return readProperty("CustomerName-Environments");

	}

	public static String getCustomerNameOrID() throws IOException {
		if(isProduction())
			return readProperty("CustomerID-Production");
		else
			return readProperty("CustomerName-Environments");

	}


		public static boolean isProduction() throws IOException {
			String current = readProperty("ClusterToTest");
			if(current.contains("-"))
				return true;
			else
				return false;
		}


	public static boolean isEnvironments() throws IOException {
		String current = readProperty("ClusterToTest");
		current = current.trim().toLowerCase();
		return isEnvironment(current);
	}

	public static boolean isEnvironment(String isThisStringEnvironment){
			return  Arrays.asList(environmentsNamesArray).contains(isThisStringEnvironment.trim().toLowerCase());
		}





}
