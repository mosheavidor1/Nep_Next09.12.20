package Actions;

import DataModel.DbJson;
import Utils.EventsLog.LogEntry;
import Utils.JsonUtil;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.Remote.SSHManager;
import Utils.TestFiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.*;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentActions  {

    public enum EP_OS {
        WINDOWS,
        LINUX,
        UNKNOWN
    }

    public static final String windowsInstallationFile = ManagerActions.windowsInstallationFile;
    public static final String linuxInstallationFile = ManagerActions.linuxInstallationFile;
    public static final String installationFolder = "/C:/Program Files/Trustwave/NEPAgent";
    public static final String LinuxinstallationFolder = "/opt/tw-endpoint/";
    public static final String nepa_caDotPemFileName = "nepa_ca.pem";
    public static final String nepa_caDotPemPath = "C:/Program Files/Trustwave/NEPAgent/certs/";
    public static final String nepa_caLinuxDotPemPath = "/opt/tw-endpoint/bin/certs/nepa_ca.pem";

    public static final String windowsHostsFile = "/C:/Windows/System32/drivers/etc/hosts";
    public static final String linuxHostsFile = "/etc/hosts";
    public static final String hostsFileRedirection = "endpoint-protection-services.local.tw-test.net";
    public static final String hostsFileIngressRedirection = "siem-ingress.trustwave.com";
    public static final char hostsFileCommentChar = '#';
    public static final String exexInstPath = "C:\\Program Files\\Trustwave\\NEPAgent";

    public static final String dbJsonPath = "/C:/ProgramData/Trustwave/NEPAgent/db.json";
    public static final String dbJsonLinuxPath = "/opt/tw-endpoint/data/db.json";
    public static final String configJsonWindowsPath_1_1 = "/C:/ProgramData/Trustwave/NEPAgent/config.json";
    public static final String configJsonWindowsPath_1_2 = "/C:/ProgramData/Trustwave/NEPAgent/General/2/config.json";
    public static final String configJsonLinuxPath_1_1 = "/opt/tw-endpoint/data/config.json";
    public static final String configJsonLinuxPath_1_2 = "/opt/tw-endpoint/data/General/2/config.json";
    public static final String versionJsonWindowsPath = "/C:/Program Files/Trustwave/NEPAgent/version.json";
    public static final String versionJsonLinuxPath = "/opt/tw-endpoint/bin/version.json";

    private static final String configJsonReportInterval = "\"report_period\":";

    protected static final int checkInterval = 5000;


    private String epIp, epUserName, epPassword, epName;
    private EP_OS epOs;
    private SSHManager connection;
    public static final int connection_port =22;

    public AgentActions(String epIp, String epUserName, String epPassword) {
        this.epIp = epIp;
        this.epUserName = epUserName;
        this.epPassword = epPassword;
        connection = new SSHManager(epUserName,epPassword,epIp, connection_port );
    }

    public AgentActions(String epIp, String epUserName, String epPassword, String epType) {
        this.epIp = epIp;
        this.epUserName = epUserName;
        this.epPassword = epPassword;
        connection = new SSHManager(epUserName,epPassword,epIp, connection_port );

        this.epOs = epType.contains("win") ? AgentActions.EP_OS.WINDOWS : AgentActions.EP_OS.LINUX;

        String rawEpName = getEPName();
        if (null != rawEpName){
            this.epName = rawEpName.replaceAll("^\n+", "");
        }
    }

    public String getEpName() {
        return epName;
    }

    public void Close(){
        if (connection!=null) {
            connection.Close();
        }
    }

    public void InstallEPIncludingRequisites(int installationTimeout, int epServiceTimeout, int dbJsonToShowActiveTimeout){
        InstallEPIncludingRequisites(this.epOs, installationTimeout, epServiceTimeout, dbJsonToShowActiveTimeout);
    }

    public void InstallEPIncludingRequisites(EP_OS epOs, int installationTimeout, int epServiceTimeout, int dbJsonToShowActiveTimeout){
        if ((epOs == EP_OS.WINDOWS)) {
            UnInstallWindowsEndPoint(installationTimeout);
        } else {
            UnInstallLinuxEndPoint(installationTimeout);
        }
        CopyInstaller(epOs);
        AppendToHostsFile(epOs);
        InstallEndPointWithoutAdditions(epOs, installationTimeout);
        StopEPService(epServiceTimeout, epOs);
        AddCaCertificate(epOs);
        StartEPService(epServiceTimeout, epOs);
        CheckEndPointActiveByDbJson(dbJsonToShowActiveTimeout, epOs);
    }

    public void UninstallEndpoint(EP_OS epOs, int installationTimeout) {
        if ((epOs == EP_OS.WINDOWS)) {
            UnInstallWindowsEndPoint(installationTimeout);
        } else {
            UnInstallLinuxEndPoint(installationTimeout);
        }
    }


    public void InstallEndPointWithoutAdditions(EP_OS epOs, int timeout) {
        try {
            JLog.logger.info("Installing EP...");
            String installerLocation = GetDownloadFolder(epOs);

            installerLocation += (epOs == EP_OS.WINDOWS) ? "/" + windowsInstallationFile : "/" + linuxInstallationFile;

            if (! connection.IsFileExists(installerLocation)){
                org.testng.Assert.fail("Could not find installation file at the following path: " + installerLocation + " At machine: "+ epIp);
            }

            if(epOs == EP_OS.WINDOWS) {
                // if windows remove the leading "/"
                installerLocation = installerLocation.substring(1);
            }

            JLog.logger.debug("installer location: " + installerLocation);

            if(epOs == EP_OS.LINUX){
                // if linux set to executable
                String permCommand = "chmod +x " + installerLocation;
                connection.Execute(permCommand);
            }

            String command = (epOs == EP_OS.WINDOWS) ? installerLocation + " /q "/*+ host*/ : installerLocation;
            connection.Execute(command);

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);
            boolean found = false;

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                if (EndPointServiceExist(epOs)) {
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

    private String GetDownloadFolder(EP_OS epOs) {

        if( epOs == EP_OS.WINDOWS ) {
            return PropertiesFile.readProperty("EPDownloadWindowsFolder");
        } else {
            return PropertiesFile.readProperty("EPDownloadLinuxFolder");
        }
    }

    private void UnInstallWindowsEndPoint(int timeout) {
        try {
            JLog.logger.info("Uninstalling EP if exists...");

            String DownloadFolder = GetDownloadFolder(EP_OS.WINDOWS);
            String installerLocation = DownloadFolder + "/" + windowsInstallationFile;
            //boolean toDeleteInstaller = true;

            if (! connection.IsFileExists(installerLocation)) {
                installerLocation = DownloadFolder + "/" + windowsInstallationFile;
                //toDeleteInstaller = false;
            }
            installerLocation=installerLocation.substring(1);
            String command = installerLocation + " /q /uninstall";

            //wmic is not working because EP bootstrap has missing data
            //execCmd("wmic product where \"description='Trustwave Endpoint Agent (64bit)' \" uninstall",true);
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            connection.Execute(command);
            boolean found = true;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                if (!EndPointServiceExist(EP_OS.WINDOWS)) {
                    found = false;
                    break;
                }
            }

            if (found)
                org.testng.Assert.fail("Uninstall failed. Trustwave Endpoint Agent Service still found after timeout(sec): " + Integer.toString(timeout));

            //If agent uninstall did remove installation folder do not remove the folder as this is fail intermittently- not clear why needs investigation
            // maybe because a recursive solution to delete all files is needed. Needs a check

            /*
            //File installationFolderFile = new File(installationFolder);
            found = true;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                if (! connection.IsFileExists(installationFolder)) {
                    found = false;
                    break;
                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                //JLog.logger.debug("Found Folder!!!");

            }

            if (found) {
                connection.DeleteFile(installationFolder); //delete is commented - see above

                durationTimeout = durationTimeout.plusMinutes(1);
            }*/

            found = true;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {

                String result = connection.Execute("tasklist");
                if (!result.contains(windowsInstallationFile)) {
                    found = false;
                    break;
                }
                //JLog.logger.debug("Found Process!!!");
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

            }

            if (found)
                org.testng.Assert.fail("Uninstall failed. Trustwave installation process is still active after timeout (sec): " + Integer.toString(timeout) + "   Installation process: " + windowsInstallationFile);

            //if (toDeleteInstaller)
            //    TestFiles.DeleteFile(installerLocation);

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not uninstall end point" + "\n" + e.toString());
        }

    }

    private void UnInstallLinuxEndPoint(int timeout) {
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
                if (!EndPointServiceExist(EP_OS.LINUX)) {
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
    public void StopEPService(int timeout, EP_OS epOs) {
        try {

            JLog.logger.info("Stopping EP service...");

            String stopCommand = (epOs == EP_OS.WINDOWS) ? "Net stop NepaService" : "systemctl stop tw-endpoint";

            connection.Execute(stopCommand);

            boolean active = true;
            String result = "";

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

                String statusCommand = (epOs == EP_OS.WINDOWS) ? "sc query \"NepaService\"" : "systemctl status tw-endpoint";
                result = connection.Execute(statusCommand);

                //JLog.logger.debug("Status command result: " + result);

                String proofStr = (epOs == EP_OS.WINDOWS) ? "STOPPED" : "Stopped";

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

    public void StartEPService(int timeout, EP_OS epOs) {
        try {
            JLog.logger.info("Starting EP service...");

            String startCommand = (epOs == EP_OS.WINDOWS) ? "Net start NepaService" : "systemctl start tw-endpoint";
            connection.Execute(startCommand);

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            boolean active = false;

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

                String statusCommand = (epOs == EP_OS.WINDOWS) ? "sc query \"NepaService\"" : "systemctl status tw-endpoint";
                String result = connection.Execute(statusCommand);

                //JLog.logger.debug("StartEPService: Status command result: " + result);

                String proofStr = (epOs == EP_OS.WINDOWS) ? "RUNNING" : "running";

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

    public void CheckRevoked(int timeout) {

        boolean revoked = false;
        JLog.logger.info("Starting CheckRevoked verification ...");

        try {
            Thread.sleep(checkInterval);

            // Restart the endpoint to initiate the config update to perform the revoke action
            StopEPService(timeout, epOs);
            // Start without verifying the start
            String startCommand = (epOs == EP_OS.WINDOWS) ? "Net start NepaService" : "systemctl start tw-endpoint";
            connection.Execute(startCommand);

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                if (!EndPointServiceExist(epOs)) {
                    revoked = true;
                    JLog.logger.info("The endpoint was revoked correctly");
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(!revoked){
            org.testng.Assert.fail("Endpoint revoked verification failed, the endpoint service still installed.");
        }
    }

    public void CheckNotRevoked() {
        if(!EndPointServiceExist(epOs)){
            org.testng.Assert.fail("Endpoint not revoked verification failed, the endpoint service is not installed.");
        }
    }

    public void CheckDeleted(int timeout) {

        boolean deleted = false;

        // Restart the endpoint to initiate the config update to perform the revoke action
        StopEPService(timeout, epOs);
        // Start without verifying the start
        String startCommand = (epOs == EP_OS.WINDOWS) ? "Net start NepaService" : "systemctl start tw-endpoint";
        connection.Execute(startCommand);

        try {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                if (!EndPointServiceExist(epOs)) {
                    deleted = true;
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(!deleted){
            org.testng.Assert.fail("Endpoint deleted verification failed, the endpoint service still installed.");
        }
    }

    public void CheckNotDeleted() {
        if(!EndPointServiceExist(epOs)){
            org.testng.Assert.fail("Endpoint not deleted verification failed, the endpoint service is not installed.");
        }
    }


    public void CopyInstaller(EP_OS epOs){

        JLog.logger.info("Copying installer to the EP...");

        String installerDestination=null;
        String installerSource = null;
        try {
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
            String epDownloadDirectory = GetDownloadFolder(epOs);

            JLog.logger.debug("masterDownloadDirectory: " + masterDownloadDirectory);
            JLog.logger.debug("epDownloadDirectory: " + epDownloadDirectory);

            if(! connection.IsFileExists(epDownloadDirectory)) {
                if (epOs== EP_OS.WINDOWS && !connection.IsFileExists("/C:/home")) {
                    connection.CreateDirectory("/C:/home");
                }
                connection.CreateDirectory(epDownloadDirectory);
            }

            installerDestination = (epOs == EP_OS.WINDOWS) ? epDownloadDirectory + "/" + windowsInstallationFile : epDownloadDirectory + "/" + linuxInstallationFile;
            installerSource = (epOs == EP_OS.WINDOWS) ? masterDownloadDirectory + "/" + windowsInstallationFile : masterDownloadDirectory + "/" + linuxInstallationFile;

            JLog.logger.debug("installerDestination: " + installerDestination);
            JLog.logger.debug("installerSource: " + installerSource);

            connection.CopyToRemote(installerSource, installerDestination);

        }
        catch ( Exception e) {
            org.testng.Assert.fail("Could not copy installer from: " + installerSource + " to " + installerDestination +" EP machine: " + epIp + "\n" + e.toString(), e);
        }

    }


    public void AddCaCertificate(EP_OS epOs)  {

        JLog.logger.info("Adding certificate on the EP...");

        String remoteCaFile=null;

        try {
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
            String localCaFile = masterDownloadDirectory + "/" + nepa_caDotPemFileName;

            remoteCaFile = (epOs == EP_OS.WINDOWS) ? "/" + nepa_caDotPemPath + nepa_caDotPemFileName : nepa_caLinuxDotPemPath;
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

    public void CompareConfigurationToEPConfiguration (EP_OS epOs){
        String configJson=null;
        String sentConfiguration = null;
        try {
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();

            String confFile = masterDownloadDirectory+"/" + ManagerActions.customerConfigurationSentSuccessfullyFile;
            sentConfiguration = FileUtils.readFileToString(new File(confFile),Charset.defaultCharset());

            String localFilePath = masterDownloadDirectory + "/" + "ConfigJsonCopy.txt";
            String configJsonRemotePath = getConfigPath(epOs);
            connection.CopyToLocal(configJsonRemotePath,localFilePath);

            FileInputStream inputStream = new FileInputStream(localFilePath);
            configJson = IOUtils.toString(inputStream, Charset.defaultCharset());
            inputStream.close();

            JSONObject configSent = new JSONObject(sentConfiguration);
            long sentCustomerID = configSent.getLong("customerId");

            //compare only the configuration part of the json sent. Customer ID is checked separately
            JSONObject configurationObjectSent = configSent.getJSONObject("configuration");

            //add schema version to corresponds config.json schema version location at the json checked
            String schemaVersionSent = configurationObjectSent.getJSONObject("centcom_meta").getString("schema_version");
            configurationObjectSent.optJSONObject("global_conf").put("schema_version", schemaVersionSent);

            //remove centcom_meta from compared json as it is not part of client's config.json
            configurationObjectSent.remove("centcom_meta");

            JSONObject configReceived = new JSONObject(configJson);
            String receivedCustomerID = configReceived.getJSONObject("global_conf").getString("customer_id");

            org.testng.Assert.assertEquals(sentCustomerID, Long.parseLong(receivedCustomerID), "Customer ID sent is not identical to customer ID appears at config.json file: ");
            JSONAssert.assertEquals("Configuration set is not identical to configuration received. See differences at the following lines:\n ", configurationObjectSent.toString(), configReceived.toString(), JSONCompareMode.LENIENT);
        }
        catch ( Exception e){
            org.testng.Assert.fail("Could not compare configuration sent to configuration received by endpoint:\n" + e.toString() + "\n Configuration sent:  " + sentConfiguration.replaceAll("\n", "") + "\nConfiguration received: " + configJson );

        }
    }

    public void AppendToHostsFile (EP_OS epOs) {

        String pathToEPHostsFile = null;

        try {
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
            String localHostsCopy = masterDownloadDirectory + "/" + "HostsCopy";

            pathToEPHostsFile = (epOs == EP_OS.WINDOWS) ? windowsHostsFile : linuxHostsFile;

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

    public void CheckEndPointActiveByDbJson(int timeout, EP_OS epOs) {
        try {

            JLog.logger.info("Checking if db.json file is present...");

            String text = "";
            boolean active = false;
            //File file = new File(dbJsonPath);

            String dbJsonRemoteFile = null;

            dbJsonRemoteFile = (epOs == EP_OS.WINDOWS) ? dbJsonPath : dbJsonLinuxPath;

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                if (connection.IsFileExists(dbJsonRemoteFile)) {
                    text =connection.GetTextFromFile(dbJsonRemoteFile);
                    if (text.contains("\"EndpointId\": \"") && text.contains("\"DsInitialHost\": ")) {
                        active = true;
                        break;
                    }
                }

                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

            }

            if (! connection.IsFileExists(dbJsonRemoteFile))
                org.testng.Assert.fail("Endpoint is not connected - db.json file was not found at: " + dbJsonRemoteFile + " after timeout(sec): " + timeout);

            if (!active)
                org.testng.Assert.fail("Endpoint is not connected according to db.json file after timeout(sec): " + timeout + ". Failed to find End Point ID  Or Host in db.json: " + dbJsonRemoteFile + "\ndb.json file content:\n" + text);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not check if endpoint is active by db.json file." + "\n" + e.toString(), e);
        }


    }

    public String GetEpIdFromDbJson() {

        String endpointId = "";

        try {
            String dbJsonRemoteFile = (epOs == EP_OS.WINDOWS) ? dbJsonPath : dbJsonLinuxPath;
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

    public boolean EndPointServiceExist(EP_OS epOs) {
        try {
            String command = (epOs == EP_OS.WINDOWS) ? "net start" : "systemctl status tw-endpoint";
            String result = connection.Execute(command);

            String proofStr = (epOs == EP_OS.WINDOWS) ? "Trustwave Endpoint Agent Service" : "tw-endpoint.service - Trustwave endpoint";

            if (result.contains(proofStr))
                return true;
            else
                return false;

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not check if endpoint service exist." + "\n" + e.toString(), e);
            return false;
        }
    }


    //Example "EventCreate /t INFORMATION /id 123 /l APPLICATION /so AutomationTest /d \"Hello!! this is the test info\""
    public void WriteEvent(LogEntry entry) {
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

    public void ChangeReportInterval(String interval, int serviceStartStopTimeout) {
        try {

            StopEPService(serviceStartStopTimeout, EP_OS.WINDOWS);
            String config_file = getConfigPath(EP_OS.WINDOWS);
            if (!connection.IsFileExists(config_file)) {
                org.testng.Assert.fail("Could not find config.json; file was not found at: " + config_file);
            }

            String text = connection.GetTextFromFile(config_file);

            if (!text.contains(configJsonReportInterval)) {
                StartEPService(serviceStartStopTimeout, EP_OS.WINDOWS);
                org.testng.Assert.fail("Endpoint did not received expected configuration. Could not change the logs interval as " + configJsonReportInterval + " could not be found at: " + config_file);
            }

            int start = text.indexOf(configJsonReportInterval) + configJsonReportInterval.length();
            int end = text.indexOf(",", start);
            StringBuilder builder = new StringBuilder(text);
            builder.replace(start, end, interval);

            connection.WriteTextToFile(builder.toString(), config_file);

            StartEPService(serviceStartStopTimeout, EP_OS.WINDOWS);
        }

        catch (Exception e) {
            org.testng.Assert.fail("Could not change endpoint report interval." + "\n" + e.toString());
        }


    }

    public void writeAndExecute(String text, EP_OS os) {
        String remoteFilePath;
        String execPath = exexInstPath + "\\createFoldersAndLogs.bat";
        if (os == EP_OS.WINDOWS) {
            remoteFilePath = installationFolder + "/createFoldersAndLogs.bat";
            connection.WriteTextToFile(text, remoteFilePath);
            execPath = exexInstPath + "\\createFoldersAndLogs.bat";
        } else {
            remoteFilePath = LinuxinstallationFolder + "/createFoldersAndLogs.sh";
            connection.WriteTextToFile(text, remoteFilePath);
            String chmod = "chmod 755 " + remoteFilePath;
            connection.Execute(chmod);
            execPath = remoteFilePath;
        }
        connection.Execute(execPath);
        connection.DeleteFile(remoteFilePath);
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

    public void clearFile(String fileName, EP_OS os) {
        String ClearCmd;
        if (os == EP_OS.WINDOWS) {
            ClearCmd = "type nul > " + fileName;
        } else {
            ClearCmd = "> " + fileName;
        }
        connection.Execute(ClearCmd);
    }

    public String getEPName() {
        String comm = "hostname";
        String result = connection.Execute(comm);
        return result;
    }

    public String getEPBinaryVersion(EP_OS epOs) {

        String epVersion = "";

        String versionRemoteFile = (epOs == EP_OS.WINDOWS) ? versionJsonWindowsPath : versionJsonLinuxPath;

        if (connection.IsFileExists(versionRemoteFile)) {
            String text = connection.GetTextFromFile(versionRemoteFile);

            JSONObject json = new JSONObject(text);
            epVersion = json.getString("BinVersion");
        }

        return epVersion;
    }
    public String getConfigPath(EP_OS os) {
        String conf_path;
        if (os == EP_OS.WINDOWS) {
            if (connection.IsFileExists(configJsonWindowsPath_1_2)) {
                conf_path = configJsonWindowsPath_1_2;
            } else {
                conf_path = configJsonWindowsPath_1_1;
            }
        } else {
            if (connection.IsFileExists(configJsonLinuxPath_1_2)) {
                conf_path = configJsonLinuxPath_1_2;
            } else {
                conf_path = configJsonLinuxPath_1_1;
            }
        }
        return conf_path;
    }
}

