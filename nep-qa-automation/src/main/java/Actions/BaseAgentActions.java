package Actions;

import java.io.FileInputStream;

import Utils.TestFiles;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.Remote.SSHManager;

import java.io.*;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import DataModel.DbJson;

public abstract class BaseAgentActions implements AgentActionsInterface{
	
	private static final String configJsonReportInterval = "\"report_period\":";
    private static final String LfmData = "LfmData";
    protected static final int checkInterval = 5000;
    public static final String nepa_caDotPemFileName = "nepa_ca.pem";
    public static final String nepa_caDotPemPath = "C:/Program Files/Trustwave/NEPAgent/certs/";
    public static final String hostsFileRedirection = "endpoint-protection-services.local.tw-test.net";
    public static final String hostsFileIngressRedirection = "siem-ingress.trustwave.com";
    public static final char hostsFileCommentChar = '#';
	
	private String epIp, epUserName, epPassword, epName;
    protected SSHManager connection;
    public static final int connection_port =22;

    public static final String localFilePath = PropertiesFile.getManagerDownloadFolder() + "/" + "ConfigJsonCopy.txt"; 
	
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
	public void reinstallEndpoint(int installationTimeout, int epServiceTimeout, int dbJsonToShowActiveTimeout){     
	        
		uninstallEndpoint(installationTimeout);                                                                      
	    copyInstaller();                                                                                           
	    appendToHostsFile();                                                                                                
	    installEndpoint(installationTimeout);                                                             
	    stopEPService(epServiceTimeout);
	    addCaCertificate();                                                                                                 
	    startEPService(epServiceTimeout);                                                                                 
	} 
		
	//Waits until check updates will run, and uninstall will be done as a result
    public void checkDeleted(int timeout) {

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
        } catch (InterruptedException e) {
        	JLog.logger.info("Got interrupted exception");
        }

        if(!deleted){
            org.testng.Assert.fail("Endpoint deleted verification failed, the endpoint service still running.");
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
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
            String localHostsCopy = masterDownloadDirectory + "/" + "HostsCopy";

            pathToEPHostsFile = getHostsFile();

            connection.CopyToLocal(pathToEPHostsFile,localHostsCopy);
            TestFiles.RemoveLines(localHostsCopy, hostsFileRedirection, hostsFileCommentChar);
            TestFiles.RemoveLines(localHostsCopy, hostsFileIngressRedirection, hostsFileCommentChar);
            String toAppend = PropertiesFile.readProperty("ClusterToTest") + " " + hostsFileRedirection;
            toAppend += "\n" + PropertiesFile.readProperty("ClusterToTest") + " " + hostsFileIngressRedirection;

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

        try {
            String dbJsonRemoteFile = getDbJsonPath();
            if (connection.IsFileExists(dbJsonRemoteFile)) {
                String dbJsonFileContent = connection.GetTextFromFile(dbJsonRemoteFile);
                ObjectMapper objectMapper = new ObjectMapper();
                DbJson dbJson = objectMapper.readValue(dbJsonFileContent, DbJson.class);
                endpointId = dbJson.getEndpointId();
            }
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not get endpoint id from db.json file." + "\n" + e.toString(), e);
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

    public void deleteLFMData() {
	    FileWriter file;
        try {

            String dbJsonRemoteFile = getDbJsonPath();
            if (!connection.IsFileExists(dbJsonRemoteFile)) {
                org.testng.Assert.fail("Could not find db.json; file was not found at: " + dbJsonRemoteFile);
            }

            String text = connection.GetTextFromFile(dbJsonRemoteFile);

            if (!text.contains(LfmData)) {
                return;
            }
            JSONObject dbFile = new JSONObject(text);
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


    }

    public String findPattern(String comm, String pattern) {
        String result = connection.Execute(comm);
        if (result.contains(pattern)) {
            return result;
        }
        else {
            return null;
        }
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
        String ClearCmd = getClearFileCommand();
        connection.Execute(ClearCmd + fileName);
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


}
