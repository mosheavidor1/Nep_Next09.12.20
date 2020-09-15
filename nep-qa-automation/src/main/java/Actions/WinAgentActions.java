package Actions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

public class WinAgentActions extends BaseAgentActions implements AgentActionsInterface{
	
	public static final String installationFolder = "/C:/Program Files/Trustwave/NEPAgent";
    public static final String windowsInstallationFile = ManagerActions.windowsInstallationFile;
    public static final String windowsHostsFile = "/C:/Windows/System32/drivers/etc/hosts";
    public static final String exexInstPath = "C:\\Program Files\\Trustwave\\NEPAgent";
    public static final String dbJsonPath = "/C:/ProgramData/Trustwave/NEPAgent/db.json";
    public static final String versionJsonWindowsPath = "/C:/Program Files/Trustwave/NEPAgent/version.json";
    private static final String winLog = "C:\\ProgramData\\Trustwave\\NEPAgent\\logs\\NewAgent_0.log";
    private static final String command_winSIEM = "type " + winLog + " | find /n \".zip was sent successfully\"";
    private static final String command_winLCA = "type " + winLog + " | find /n \".txt was sent\"";
    private static final String command_winLCA2 = "type " + winLog + " | find /n \".txt-tag.log was sent\"";
    private static final String command_winLCA3 = "type " + winLog + " | find /n \".log-tag.log was sent\"";
    public static final String configJsonWindowsPath_1_1 = "/C:/ProgramData/Trustwave/NEPAgent/config.json";
    public static final String configJsonWindowsPath_1_2_gen = "/C:/ProgramData/Trustwave/NEPAgent/General/";
    public static final String configJsonWindowsPath_1_2_new = "/C:/ProgramData/Trustwave/NEPAgent/General/new";
    public static final String configJsonWindowsPath_1_2_stable = "/C:/ProgramData/Trustwave/NEPAgent/General/stable";
    
    private static final String startCommand = "Net start NepaService";
    private static final String stopCommand = "Net stop NepaService";
    
    Map<String, String> scriptNamesMap;
    
    public WinAgentActions(String epIp, String epUserName, String epPassword) {
    	super(epIp, epUserName, epPassword);
    	if (!connection.IsFileExists("/C:/home")) {
            connection.CreateDirectory("/C:/home");
    	}
        scriptNamesMap = new HashMap<String, String>();
        scriptNamesMap.put("LFM_Create_Dir", "WinDirscript");
        scriptNamesMap.put("LFM_Create_Log", "WIncreateLogs");
        scriptNamesMap.put("key3", "value3");
    }
    
    public String getInstallationFile() {
    	return windowsInstallationFile;
    }
    
    public String getRemoteCaFile() {
    	return "/" + nepa_caDotPemPath + nepa_caDotPemFileName;
    }
    
    public String getHostsFile() {
    	return windowsHostsFile;
    }
    
    public String getDbJsonPath() {
    	return dbJsonPath;
    }
    
    public String getVersionJsonPath() {
    	return versionJsonWindowsPath;
    }
       
    public String getVerifySiemCommand() {
    	return command_winSIEM;
    }
    public String getVerifyLcaCommand() {
    	return command_winLCA;
    }
    public String getVerifyLca2Command() {
    	return command_winLCA2;
    }
    public String getVerifyLFMLca2Command() {
        return command_winLCA3;
    }
    public String getAgentLogPath() {
    	return winLog;
    }
    
    public boolean endpointServiceRunning() {
    	try {
            String command = "net start";
            String result = connection.Execute(command);

            String proofStr = "Trustwave Endpoint Agent Service";

            if (result.contains(proofStr)) {
                return true;
            }
            return false;

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not check if endpoint service exist." + "\n" + e.toString(), e);
            return false;
        }
    }
	

	public void uninstallEndpoint(int timeout) {
        try {
            JLog.logger.info("Uninstalling EP if exists...");

            String DownloadFolder = getDownloadFolder();
            String installerLocation = DownloadFolder + "/" + windowsInstallationFile;

            if (! connection.IsFileExists(installerLocation)) {
                installerLocation = DownloadFolder + "/" + windowsInstallationFile;
            }
            installerLocation=installerLocation.substring(1);
            String command = installerLocation + " /q /uninstall";

            
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            connection.Execute(command);
            boolean found = true;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                if (!endpointServiceRunning()) {
                    found = false;
                    break;
                }
            }

            if (found)
                org.testng.Assert.fail("Uninstall failed. Trustwave Endpoint Agent Service still found after timeout(sec): " + Integer.toString(timeout));
          

            found = true;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {

                String result = connection.Execute("tasklist");
                if (!result.contains(windowsInstallationFile)) {
                    found = false;
                    break;
                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

            }

            if (found)
                org.testng.Assert.fail("Uninstall failed. Trustwave installation process is still active after timeout (sec): " + Integer.toString(timeout) + "   Installation process: " + windowsInstallationFile);


        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not uninstall end point" + "\n" + e.toString());
        }

    }
	
	public void installEndpoint(int timeout) {
        try {
            JLog.logger.info("Installing EP...");
            String installerLocation = getDownloadFolder() + "/" + windowsInstallationFile;

            if (! connection.IsFileExists(installerLocation)){
                org.testng.Assert.fail("Could not find installation file at the following path: " + installerLocation + " At machine: "+ getEpIp());
            }

            //  remove the leading "/"
            installerLocation = installerLocation.substring(1);

            JLog.logger.debug("installer location: " + installerLocation);

            String command = installerLocation + " /q "/*+ host*/ ;
            connection.Execute(command);

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);
            boolean found = false;

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                if (endpointServiceRunning()) {
                    found = true;
                    break;
                }
            }

            if (!found)
                org.testng.Assert.fail("Trustwave Endpoint installation failed. Trustwave Endpoint Agent Service was not found on services list");

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not install endpoint." + "\n" + e.toString());
        }

    }                                                                                                                         

	
	public String getDownloadFolder() {
        return PropertiesFile.readProperty("EPDownloadWindowsFolder");
    }
	
	public void startEPService(int timeout) {
        try {
            JLog.logger.info("Starting EP service...");

            connection.Execute(startCommand);

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            boolean active = false;

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

                String statusCommand = "sc query \"NepaService\"";
                String result = connection.Execute(statusCommand);

                //JLog.logger.debug("StartEPService: Status command result: " + result);

                String proofStr = "RUNNING";

                if (result.contains(proofStr)) {
                    active = true;
                    break;
                }
            }

            if (!active)
                org.testng.Assert.fail("Failed to start End Point service");
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not start endpoint service." + "\n" + e.toString());
        }

    }
	
	public void stopEPService(int timeout) {
        try {

            JLog.logger.info("Stopping Win EP service...");

            connection.Execute(stopCommand);

            boolean active = true;
            String result = "";

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

                String statusCommand = "sc query \"NepaService\"";
                result = connection.Execute(statusCommand);

                String proofStr = "STOPPED";

                if (result.contains(proofStr)) {
                    active = false;
                    break;
                }
            }

            if (active)
                org.testng.Assert.fail("Failed to stop End Point service");
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not stop endpoint service." + "\n" + e.toString());
        }

    }
	
	public void writeAndExecute(String text) {
        String remoteFilePath = installationFolder + "/createFoldersAndLogs.bat";
        connection.WriteTextToFile(text, remoteFilePath);
        String execPath = exexInstPath + "\\createFoldersAndLogs.bat";
        connection.Execute(execPath);
        connection.DeleteFile(remoteFilePath);
    }
	
	public String getClearFileCommand() {
		return "type nul > ";
	}

	public String getScriptName(String scriptName) {
        String nameExcell = scriptNamesMap.get(scriptName);
        return nameExcell;
    }
    public String getConfigPath(boolean afterUpdate) {
        String conf_path;
        String new_or_stable;
        if (!connection.IsFileExists(configJsonWindowsPath_1_2_new))
            return configJsonWindowsPath_1_1;
            if (afterUpdate) {
                new_or_stable = connection.GetTextFromFile(configJsonWindowsPath_1_2_new);
            } else {
                new_or_stable = connection.GetTextFromFile(configJsonWindowsPath_1_2_stable);
            }
            conf_path = configJsonWindowsPath_1_2_gen + new_or_stable + "/config.json";
        return conf_path;
    }
    }
