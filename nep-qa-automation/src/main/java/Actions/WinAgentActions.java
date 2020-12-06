package Actions;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.Remote.SSHManager;

public class WinAgentActions extends BaseAgentActions implements AgentActionsInterface{
	
	//public static final String installationFolder = "C:/Program Files/Trustwave/NEPAgent";
    public static final String windowsInstallationFile = ManagerActions.windowsInstallationFile;
    public static final String msiInstallationFile = "TrustwaveEndpoint.msi";

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
    private static final String windows_service = "\"" + exexInstPath +"\\NepaService.exe\"";
    private static final String windows_agent_settings = "/C:/Program Files/Trustwave/NEPAgent/settings.json";
    private static final String scriptExtension = ".bat";
    
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
    	if (osName.equalsIgnoreCase("MSI")) {
            return msiInstallationFile;
        }
    	else {
            return windowsInstallationFile;
        }
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

    public String getServiceExecutablePath() {
        return windows_service;
    }

    public String getSettingsPath() {
        return windows_agent_settings;
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
            String installFile = null;
            if(osName.equalsIgnoreCase("MSI")){
                installFile = msiInstallationFile;
            }
            else {
                installFile = windowsInstallationFile;
            }
            String installerLocation = DownloadFolder + "/" + installFile;

            installerLocation=installerLocation.substring(1);

            String command=null;
            if (osName.equalsIgnoreCase("MSI")){
                installerLocation =installerLocation.replaceAll("/","\\\\");
                command = "msiexec.exe /x " + installerLocation + " /q";
            }
            else {
                command = installerLocation + " /q /uninstall";
            }

            
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
            String installerName=null;
            if(osName.equalsIgnoreCase("MSI")){
                installerName = msiInstallationFile;
            }
            else {
                installerName = windowsInstallationFile;
            }
            String installerLocation = getDownloadFolder() + "/" + installerName;

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

	public void writeAndExecute(String text, String scriptFilename) {
		try {
			JLog.logger.info("Going to save script and run {}", "/C:/home/" + scriptFilename + scriptExtension);
	        connection.WriteTextToFile(text, "/C:/home/" + scriptFilename + scriptExtension);
	        connection.Execute("C:\\home\\" + scriptFilename + scriptExtension);
		}
		catch(Exception ex) {
			org.testng.Assert.fail("Failed in writeAndExecute function", ex);
		}
    }
	
	public String getClearFileCommand(String filename) {
		if (filename.startsWith("/")) {
			filename = filename.substring(1);
		}
		return "powershell \"Clear-Content " + filename + "\"";
	}

	public String getScriptName(String scriptName) {
        return scriptNamesMap.get(scriptName);
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

    public String getEpNameAndDomain() {

        try {

            String result = connection.Execute("ipconfig /all");
            String domainIdentifier ="Primary Dns Suffix";
            String fullHostname;

            if (null != result) {
                int start = result.indexOf(domainIdentifier);
                String domain = result.substring(start+domainIdentifier.length());
                domain=domain.substring(0,domain.indexOf("\n"));
                domain=domain.substring(domain.indexOf(":")+ 1);
                domain = domain.trim();
                fullHostname=this.getEpName();
                if (!domain.isEmpty()){
                    fullHostname+="." + domain;
                }
            }
            else {
                return null;
            }
            return fullHostname;
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not get ep and domain name for Endpoint: "+epIp + "\n" + e.toString());
            return null;
        }

    }




}
