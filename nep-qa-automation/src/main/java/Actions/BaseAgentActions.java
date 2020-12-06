package Actions;

import java.io.FileInputStream;

import Utils.TestFiles;
import Utils.Data.GlobalTools;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.Remote.SSHManager;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import DataModel.DbJson;

public abstract class BaseAgentActions implements AgentActionsInterface{
	
	private static final String configJsonReportInterval = "\"report_period\":";
    protected static final int checkInterval = 5000;
    public static final String nepa_caDotPemFileName = "nepa_ca.pem";
    public static final String nepa_caDotPemPath = "C:/Program Files/Trustwave/NEPAgent/certs/";
    public static final String hostsFileRedirection = "endpoint-protection-services.local.tw-test.net";
    public static final String hostsFileIngressRedirection = "siem-ingress.trustwave.com";
    public static final char hostsFileCommentChar = '#';

	protected String epIp, epUserName, epPassword, epName;
    protected SSHManager connection;
    public static final int connection_port =22;
    private static final String command_enable_proxy = " -proxy ";
    private static final String command_disable_proxy = " -reset_proxy";
    public static final String localFilePath = PropertiesFile.getManagerDownloadFolder() + "/" + "ConfigJsonCopy.txt";

    public String osName ="";

	public BaseAgentActions(String epIp, String epUserName, String epPassword) {
        this.epIp = epIp;
        this.epUserName = epUserName;
        this.epPassword = epPassword;
        connection = new SSHManager(epUserName, epPassword, epIp, connection_port );
	}
	
	/**
	 * This function performs the following actions:
	 * Uninstall EP
	 * Copy new installer
	 * Edit hosts file
	 * Run the installer
	 * Stop the service
	 * Add CA certificate
	 * Start the service
	 * 
	 * Consider to use the InstallEndpoint directly
	 * 
	 * @param installationTimeout
	 * @param epServiceTimeout
	 * @param dbJsonToShowActiveTimeout
	 */
	public void reinstallEndpoint(int installationTimeout, int epServiceTimeout){

	    try {

            uninstallEndpoint(installationTimeout);
            copyInstallerAndInstall(installationTimeout, epServiceTimeout);
        }
	    catch (Exception e) {
            org.testng.Assert.fail("Reinstall endpoint failed " + "\n" + e.toString());
	    }

	}
	
	public void copyInstallerAndInstall(int installationTimeout, int epServiceTimeout){

	    try {

            copyInstaller();
            if (GlobalTools.isLennyEnv()) {
            	appendToHostsFile();
            }
            installEndpoint(installationTimeout, epServiceTimeout);
        }
	    catch (Exception e) {
            org.testng.Assert.fail("copyInstallerAndInstall endpoint failed " + "\n" + e.toString());
	    }

	}
	
	/**
	 * Installs the endpoint w/o uninstalling it first. 
	 * In addition, it copies the CA certificate in case this is Lenny env
	 */
	public void installEndpoint(int installationTimeout, int epServiceTimeout) {

	    try {

            installEndpoint(installationTimeout);
            if (GlobalTools.isLennyEnv()) {
            	stopEPService(epServiceTimeout);
            	addCaCertificate();
            	startEPService(epServiceTimeout);
            }
        }
	    catch (Exception e) {
            org.testng.Assert.fail("Reinstall endpoint failed " + "\n" + e.toString());
	    }

	}
		
	//Waits until check updates will run, and uninstall will be done as a result
    public boolean checkDeleted(int timeout) {

        boolean deleted = false;

        try {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                if (!endpointServiceRunning()) {
                    deleted = true;
                    JLog.logger.info("Endpoint service was stopped!");
                    break;
                }

           }

            if (deleted) {
                JLog.logger.debug("Agent Service was not found after uninstall");
            }
            else {
                return false;
            }

            if (this instanceof  WinAgentActions) {

                boolean found = true;
                while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {

                    String result = connection.Execute("tasklist");
                    if (!result.contains(WinAgentActions.windowsInstallationFile)) {
                        found = false;
                        break;
                    }
                    Thread.sleep(checkInterval);
                    current = LocalDateTime.now();

                }

                if (found) {
                    return false;
                }
                else {
                    JLog.logger.debug("Agent process was not found after uninstall");
                }
            }

            return true;

        } catch (InterruptedException e) {
        	JLog.logger.info("Got interrupted exception: \n" + e.toString());
        	return false;
        }

    }

    public void copyInstaller(){

        JLog.logger.info("Copying installer to the EP...");

        String installerDestination=null;
        String installerSource = null;
        try {
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
            String epDownloadDirectory = getDownloadFolder();

            JLog.logger.debug("masterDownloadDirectory: " + masterDownloadDirectory);
            JLog.logger.debug("epDownloadDirectory: " + epDownloadDirectory);

            if(! connection.IsFileExists(epDownloadDirectory)) {
                connection.CreateDirectory(epDownloadDirectory);
            }

            installerDestination = epDownloadDirectory + "/" + getInstallationFile();
            installerSource =  masterDownloadDirectory + "/" + getInstallationFile();

            JLog.logger.debug("installerDestination: " + installerDestination);
            JLog.logger.debug("installerSource: " + installerSource);

            connection.CopyToRemote(installerSource, installerDestination);

        }
        catch ( Exception e) {
            org.testng.Assert.fail("Could not copy installer from: " + installerSource + " to " + installerDestination +" EP machine: " + epIp + "\n" + e.toString(), e);
        }

    }


    public void addCaCertificate()  {

        JLog.logger.info("Adding certificate on the EP...");

        String remoteCaFile=null;

        try {
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
            String localCaFile = masterDownloadDirectory + "/" + nepa_caDotPemFileName;

            remoteCaFile = getRemoteCaFile();
            String localCertificateDonwloadedFromLNE = masterDownloadDirectory + "/" + LNEActions.caCertificateFileName;
            connection.CopyToLocal(remoteCaFile, masterDownloadDirectory);


            FileInputStream inputStream = new FileInputStream(localCertificateDonwloadedFromLNE);
            String caCertificate = IOUtils.toString(inputStream, Charset.defaultCharset());
            inputStream.close();
            caCertificate = "\n" + caCertificate;
            TestFiles.AppendToFile(localCaFile, caCertificate, false);
            connection.CopyToRemote(localCaFile, remoteCaFile);
        }
        catch ( Exception e) {
            org.testng.Assert.fail("Could not add ca certificate to file: " + remoteCaFile +" EP machine: " + epIp + "\n" + e.toString());
        }

    }
 
    public void appendToHostsFile () {

        String pathToEPHostsFile = null;

        try {
        	if (!GlobalTools.isLennyEnv()) {
        		JLog.logger.warn("appendToHostsFile: not Lenny env, skipping");
        		return;
        	}
        	
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
            String localHostsCopy = masterDownloadDirectory + "/" + "HostsCopy";

            pathToEPHostsFile = getHostsFile();

            connection.CopyToLocal(pathToEPHostsFile,localHostsCopy);
            TestFiles.RemoveLines(localHostsCopy, hostsFileRedirection, hostsFileCommentChar);
            TestFiles.RemoveLines(localHostsCopy, hostsFileIngressRedirection, hostsFileCommentChar);
            String toAppend = GlobalTools.getClusterToTest() + " " + hostsFileRedirection;
            toAppend += "\n" + GlobalTools.getClusterToTest() + " " + hostsFileIngressRedirection;

            //To check why this not failed
            //TestFiles.AppendToFile(windowsHostsFile, toAppend, true);
            TestFiles.AppendToFile(localHostsCopy, toAppend, true);
            connection.CopyToRemote(localHostsCopy,pathToEPHostsFile);

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not change endpoint machine hosts file: " + pathToEPHostsFile  + "\n" + e.toString());
        }

    }

    

    public String getEpIdFromDbJson() {

        String endpointId = "";
        String dbJsonRemoteFile= getDbJsonPath();

        try {
            if (connection.IsFileExists(dbJsonRemoteFile)) {
                String dbJsonFileContent = connection.GetTextFromFile(dbJsonRemoteFile);
                JLog.logger.debug("db.json content: " +dbJsonFileContent);
                ObjectMapper objectMapper = new ObjectMapper();
                DbJson dbJson = objectMapper.readValue(dbJsonFileContent, DbJson.class);
                endpointId = dbJson.getEndpointId();
                JLog.logger.debug("Endpoint ID from db.json: " +endpointId);

            } else {
            	org.testng.Assert.fail("Could not find db.json file." + dbJsonRemoteFile);
            }
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not get endpoint id from db.json file: " + "\n" + e.toString(), e );
        }
        return endpointId;

    }


    //Example "EventCreate /t INFORMATION /id 123 /l APPLICATION /so AutomationTest /d \"Hello!! this is the test info\""
    public void writeEvent(LogEntry entry) {
        try {
            if (entry.addedTimeToDescription)
                entry.AddTimeToDescription(LocalDateTime.now().toString());
            entry.eventDescription = "\"" + entry.eventDescription + "\"";
            String eventCommand = "EventCreate /t " + entry.eventType + " /id " + entry.eventID + " /l " + entry.eventLog + " /so " + entry.eventSource + " /d " + entry.eventDescription;
            String result = connection.Execute(eventCommand);
            if (!result.contains("SUCCESS: An event of type"))
                org.testng.Assert.fail("Could no add log event.\nAdd event result: " + result + "\nCommand sent: " + eventCommand);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not write event to windows log." + "\n" + e.toString());
        }

    }

    public void changeReportInterval(String interval, int serviceStartStopTimeout) {
        try {

            stopEPService(serviceStartStopTimeout);
            String config_file = getConfigPath(false);
            if (!connection.IsFileExists(config_file)) {
                org.testng.Assert.fail("Could not find config.json; file was not found at: " + config_file);
            }

            String text = connection.GetTextFromFile(config_file);

            if (!text.contains(configJsonReportInterval)) {
                startEPService(serviceStartStopTimeout);
                org.testng.Assert.fail("Endpoint did not received expected configuration. Could not change the logs interval as " + configJsonReportInterval + " could not be found at: " + config_file);
            }

            int start = text.indexOf(configJsonReportInterval) + configJsonReportInterval.length();
            int end = text.indexOf(",", start);
            StringBuilder builder = new StringBuilder(text);
            builder.replace(start, end, interval);

            connection.WriteTextToFile(builder.toString(), config_file);

            startEPService(serviceStartStopTimeout);
        }

        catch (Exception e) {
            org.testng.Assert.fail("Could not change endpoint report interval." + "\n" + e.toString());
        }


    }
/*
    public void deleteLFMData() {
	    FileWriter file;
        try {

            String dbJsonRemoteFile = getDbJsonPath();
            if (!connection.IsFileExists(dbJsonRemoteFile)) {
                org.testng.Assert.fail("Could not find db.json; file was not found at: " + dbJsonRemoteFile);
            }

            String dbJsonContect = connection.GetTextFromFile(dbJsonRemoteFile);

            if (!dbJsonContect.contains(LfmData)) {
                return;
            }
            JSONObject dbFile = new JSONObject(dbJsonContect);
            dbFile.remove(LfmData);
            JSONObject newNode = new JSONObject();
            dbFile.append(LfmData, newNode);
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
            String localDbFile = masterDownloadDirectory +"/" + ManagerActions.customerDbFile;
            file = new FileWriter(localDbFile);
            file.write(dbFile.toString());
            file.flush();
            file.close();
            connection.CopyToRemote(localDbFile, dbJsonRemoteFile);
            File deleteLocal = new File(localDbFile);
            deleteLocal.delete();
        }

        catch (Exception e) {
            org.testng.Assert.fail("Could not remove LFM data from db.json." + "\n" + e.toString());
        }


    }*/

    /**
     * Verifies that output of command is not empty + contains the expectedStr (if not null)
     */
    public String verifyExpectedOnCommandResult(String command, String expectedStr, int timeout) {
    	
    	LocalDateTime start = LocalDateTime.now();
        LocalDateTime current = start;
        Duration durationTimeout = Duration.ofSeconds(timeout);
        String result = null;        

        try {
	        while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
	            Thread.sleep(25000);//25 seconds
	            current = LocalDateTime.now();
	            result = verifyExpectedOnCommandResult(command, expectedStr);
	            if (result != null) {                
	                return result;
	            }
	        }
        } catch (InterruptedException e) {
        	JLog.logger.info("Got interrupted exception");
        }
        return result;
    }
    
    private String  verifyExpectedOnCommandResult(String command, String expectedStr) {
    	
    	if (expectedStr == null) {
    		JLog.logger.info("Going to run command '{}' and expect non empty result", command);
    	}else {
    		JLog.logger.info("Going to run command '{}' and expect '{}' in result ", command, expectedStr);
    	}
        String result = connection.Execute(command);
        
        if(result == null || result.isEmpty()) {
        	JLog.logger.info("Result of command is still null/empty");
        	return null;
        }
        if (expectedStr == null || result.contains(expectedStr)) {
        	JLog.logger.info("Result as expected!");
        	return result;
        }
        JLog.logger.info("Result of command is still not satisfied: '{}'", result);
    	return null;
    	
    }

    public String findInText(String filePath, String pattern) {
        String result = connection.GetTextFromFile(filePath);
        if (result.contains(pattern)) {
            return result;
        }
        else {
            return null;
        }
    }

    public void clearFile(String fileName) {
        String ClearCmd = getClearFileCommand(fileName);
        JLog.logger.info("Going to clear file {}", fileName);
        connection.Execute(ClearCmd);
    }

    public String getEPBinaryVersion() {

        String epVersion = "";

        String versionRemoteFile = getVersionJsonPath();

        if (connection.IsFileExists(versionRemoteFile)) {
            String text = connection.GetTextFromFile(versionRemoteFile);

            JSONObject json = new JSONObject(text);
            epVersion = json.getString("BinVersion");
        }

        return epVersion;
    }
    
	public String getEpName() {
        if (epName != null) {
        	return epName;
        }
        String comm = "hostname";
        String result = connection.Execute(comm);
        
        if (null != result){
            epName = result.replaceAll("^\n+", "");
        }
        return epName;
    }



    public void close(){
        if (connection!=null) {
            connection.Close();
        }
    }

	public String getEpIp() {
		return epIp;
	}

	public String getEpUserName() {
		return epUserName;
	}

	public String getEpPassword() {
		return epPassword;
	}

    public String enableProxy(String host, String port) {
        String cmd = getServiceExecutablePath() + command_enable_proxy + host + " " + port;
        String ret = connection.Execute(cmd);
        return ret;
    }

    public String disableProxy() {
        String cmd = getServiceExecutablePath() + command_disable_proxy;
        String ret = connection.Execute(cmd);
        return ret;
    }
}
