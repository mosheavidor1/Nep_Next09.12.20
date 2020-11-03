package Utils;

import Tests.GenericTest;
import Utils.Data.GlobalTools;

/**
 * This class handles the various json configurations needed for the tests. IT should be the only place
 * from where a configuration is fetched during tests. Here, the needed placeholders in the json configuration
 * will be replaced by needed values.
 * 
 * A json configuration is a json which represents all the settings that can be edited via Centcom UI, and get to the agents.
 * @author RSalmon
 *
 */
public class ConfigHandling {
	
	public static final String lennyIpPlaceholder = "\\{lenny-ip\\}";
	
	public static final String dsHostPlaceHolder = "\\{ds-host\\}";

	public static final String schemaVersionPlaceHolder = "\\{schema-version\\}";

	public static final String scpHostPlaceHolder = "\\{scp-host\\}";

	/**
	 * Gets json configuration from Configurations excel sheet, 'Basic Configuration' column
	 * Schema-version and ds-host placeholders are replaced by real values
	 * 
	 * @return
	 */
	public static String getDefaultConfiguration() {
		String jsonConfig = GenericTest.getConfigurations().get("Basic Configuration");
		jsonConfig = replaceSchemaVersion(jsonConfig);
		jsonConfig = replaceDsHost(jsonConfig);
		return jsonConfig;
	}
	
	/**
	 * Gets json configuration from Configurations excel sheet according to configName
	 * Schema-version and ds-host placeholders are replaced by real values
	 * 
	 * @param configName
	 * @return
	 */
	public static String getConfiguration(String configName) {
		String jsonConfig = GenericTest.getConfigurations().get(configName);
		jsonConfig = replaceSchemaVersion(jsonConfig);
		jsonConfig = replaceDsHost(jsonConfig);
		jsonConfig = replaceScpHost(jsonConfig);
		return jsonConfig;
	}
	
	private static String replaceSchemaVersion(String jsonConfig) {
		return jsonConfig.replaceAll(ConfigHandling.schemaVersionPlaceHolder, GlobalTools.currentSchemaVersion);
	}
	
	public static String replaceLennyIp(String jsonConfig) {
		if (!GlobalTools.isLennyEnv()) {
			org.testng.Assert.fail("Please check your tests, the run is not on lenny environment.");
		}
		return jsonConfig.replaceAll(ConfigHandling.lennyIpPlaceholder, GlobalTools.getClusterToTest());
	}
	
	private static String replaceDsHost(String jsonConfig) {
		return jsonConfig.replaceAll(ConfigHandling.dsHostPlaceHolder, GenericTest.getGeneralData().get("DS Name"));
	}

	private static String replaceScpHost(String jsonConfig) {
		return jsonConfig.replaceAll(ConfigHandling.scpHostPlaceHolder, GenericTest.getGeneralData().get("SCP Host"));
	}


}
