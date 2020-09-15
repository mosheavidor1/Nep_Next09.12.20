package Actions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

public class LinuxAgentActions extends BaseAgentActions implements AgentActionsInterface{
	
	public static final String linuxInstallationFile = ManagerActions.linuxInstallationFile;
    public static final String LinuxinstallationFolder = "/opt/tw-endpoint/";
    public static final String nepa_caLinuxDotPemPath = "/opt/tw-endpoint/bin/certs/nepa_ca.pem";
    public static final String linuxHostsFile = "/etc/hosts";
    public static final String dbJsonLinuxPath = "/opt/tw-endpoint/data/db.json";
    public static final String configJsonLinuxPath_1_2_gen = "/opt/tw-endpoint/data/General/";
    public static final String configJsonLinuxPath_1_2_new = "/opt/tw-endpoint/data/General/new";
    public static final String configJsonLinuxPath_1_1 = "/opt/tw-endpoint/data/config.json";
    public static final String configJsonLinuxPath_1_2_stable = "/opt/tw-endpoint/data/General/stable";
    public static final String versionJsonLinuxPath = "/opt/tw-endpoint/bin/version.json";
    private static final String linuxLog = "/opt/tw-endpoint/data/logs/tw-endpoint-agent_0.log";
    private static final String command_linuxSIEM = "cat " + linuxLog + " | grep -e \".zip was sent successfully\"";
    private static final String command_linuxLCA = "cat " + linuxLog + " | grep -e \".log-tag.log was sent\"";
    private static final String command_linuxLCA2 = "cat " + linuxLog + " | grep -e \".txt-tag.log was sent\"";
    
    private static final String startCommand = "systemctl start tw-endpoint";
    private static final String stopCommand = "systemctl stop tw-endpoint";
    
    Map<String, String> scriptNamesMap;
    
    public LinuxAgentActions(String epIp, String epUserName, String epPassword) {
    	super(epIp, epUserName, epPassword);
        scriptNamesMap = new HashMap<String, String>();
        scriptNamesMap.put("LFM_Create_Dir", "linuxDirscript");
        scriptNamesMap.put("LFM_Create_Log", "LinuxcreateLogs");
        scriptNamesMap.put("key3", "value3");
    }
    
    public String getInstallationFile() {
    	return linuxInstallationFile;
    }
    
    public String getRemoteCaFile() {
    	return nepa_caLinuxDotPemPath;
    }
    
    public String getHostsFile() {
    	return linuxHostsFile;
    }
    
    public String getDbJsonPath() {
    	return dbJsonLinuxPath;
    }
    
    public String getVersionJsonPath() {
    	return versionJsonLinuxPath;
    }
       
    public String getVerifySiemCommand() {
    	return command_linuxSIEM;
    }
    public String getVerifyLcaCommand() {
    	return command_linuxLCA;
    }
    public String getVerifyLca2Command() {
    	return command_linuxLCA2;
    }
    public String getVerifyLFMLca2Command() {return command_linuxLCA;}
    public String getAgentLogPath() {
    	return linuxLog;
    }
    
    public boolean endpointServiceRunning() {
    	try {
            String command = "systemctl status tw-endpoint";
            String result = connection.Execute(command);

            String proofStr = "tw-endpoint.service - Trustwave endpoint";

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
	
		
	public void uninstallEndpoint(int timeout){
        try {
            JLog.logger.info("Uninstalling EP if exists...");

            String linuxServiceFile = "/opt/tw-endpoint/bin/service_ctl";

            if (! connection.IsFileExists(linuxServiceFile)) {
                JLog.logger.info("Endpoint not installed, no need to remove");
                return;
            }

            String command = linuxServiceFile + " remove";

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

            JLog.logger.info("Linux EP uninstalled successfully...");
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not uninstall linux end point" + "\n" + e.toString());
        }

    }
	
	public void installEndpoint(int timeout) {
        try {
            JLog.logger.info("Installing EP...");
            String installerLocation = getDownloadFolder() + "/" + linuxInstallationFile;

            if (! connection.IsFileExists(installerLocation)){
                org.testng.Assert.fail("Could not find installation file at the following path: " + installerLocation + " At machine: "+ getEpIp());
            }

            JLog.logger.debug("installer location: " + installerLocation);

            String permCommand = "chmod +x " + installerLocation;
            connection.Execute(permCommand);

            connection.Execute(installerLocation);

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
        return PropertiesFile.readProperty("EPDownloadLinuxFolder");
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

                String statusCommand = "systemctl status tw-endpoint";
                String result = connection.Execute(statusCommand);

                //JLog.logger.debug("StartEPService: Status command result: " + result);

                String proofStr = "running";

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

            JLog.logger.info("Stopping Linux EP service...");

            connection.Execute(stopCommand);

            boolean active = true;
            String result = "";

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

                String statusCommand = "systemctl status tw-endpoint";
                result = connection.Execute(statusCommand);

                //JLog.logger.debug("Status command result: " + result);

                String proofStr = "Stopped";

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
        
        String remoteFilePath = LinuxinstallationFolder + "/createFoldersAndLogs.sh";
        connection.WriteTextToFile(text, remoteFilePath);
        String chmod = "chmod 755 " + remoteFilePath;
        connection.Execute(chmod);
        connection.Execute(remoteFilePath);
        connection.DeleteFile(remoteFilePath);
    }
	
	public String getClearFileCommand() {
		return "> ";
	}

    public String getScriptName(String scriptName) {
        String nameExcell = scriptNamesMap.get(scriptName);
        return nameExcell;
    }

    public String getConfigPath(boolean afterUpdate) {
        String conf_path;
        String new_or_stable;
            if (!connection.IsFileExists(configJsonLinuxPath_1_2_new))
                return configJsonLinuxPath_1_1;
            if (afterUpdate) {
                new_or_stable = connection.GetTextFromFile(configJsonLinuxPath_1_2_new);
            }
            else {
                new_or_stable = connection.GetTextFromFile(configJsonLinuxPath_1_2_stable);
            }
            conf_path = configJsonLinuxPath_1_2_gen + new_or_stable + "/config.json";
        return conf_path;
    }
}
