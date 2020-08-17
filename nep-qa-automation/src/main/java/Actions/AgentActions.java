package Actions;

import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.Remote.SSHManager;
import Utils.TestFiles;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.*;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;

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
    public static final String configJsonWindowsPath = "/C:/ProgramData/Trustwave/NEPAgent/config.json";
    public static final String configJsonLinuxPath = "/opt/tw-endpoint/data/config.json";

    private static final String configJsonReportInterval = "\"report_period\":";

    protected static final int checkInterval = 5000;


    private String epIp, epUserName, epPassword;
    private SSHManager connection;
    public static final int connection_port =22;

    public AgentActions(String epIp, String epUserName, String epPassword) {
        this.epIp = epIp;
        this.epUserName = epUserName;
        this.epPassword = epPassword;
        connection = new SSHManager(epUserName,epPassword,epIp, connection_port );
    }

    public void Close(){
        if (connection!=null) {
            connection.Close();
        }
    }

    public void InstallEPIncludingRequisites(EP_OS epOs, int installationTimeout, int epServiceTimeout, int dbJsonToShowActiveTimeout){
        if ((epOs == EP_OS.WINDOWS)) {
            UnInstallEndPoint(installationTimeout);
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


    public void InstallEndPointWithoutAdditions(EP_OS epOs, int timeout) {
        try {
            JLog.logger.info("Installing EP...");
            String installerLocation = PropertiesFile.readProperty("EPDownloadFolder");

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

    public void UnInstallEndPoint(int timeout) {
        try {
            JLog.logger.info("Uninstalling EP if exists...");

            String DownloadFolder = PropertiesFile.readProperty("EPDownloadFolder");
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
                connection.DeleteFile(installationFolder);
                durationTimeout = durationTimeout.plusMinutes(1);
            }

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

    public void UnInstallLinuxEndPoint(int timeout) {
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


    public void CopyInstaller(EP_OS epOs){

        JLog.logger.info("Copying installer to the EP...");

        String installerDestination=null;
        String installerSource = null;
        try {
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
            String epDownloadDirectory = PropertiesFile.readProperty("EPDownloadFolder");

            JLog.logger.debug("masterDownloadDirectory: " + masterDownloadDirectory);
            JLog.logger.debug("epDownloadDirectory: " + epDownloadDirectory);

            if(! connection.IsFileExists(epDownloadDirectory)) {
                if (SystemUtils.IS_OS_WINDOWS && !connection.IsFileExists("/C:/home")) {
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

    public void CompareConfigurationToEPConfiguration (String sentConfiguration, EP_OS epOs){
        String configJson=null;
        try {
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();

            String localFilePath = masterDownloadDirectory + "/" + "ConfigJsonCopy.txt";
            String configJsonRemotePath = (epOs == EP_OS.WINDOWS) ? configJsonWindowsPath : configJsonLinuxPath;
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
            if (!connection.IsFileExists(configJsonWindowsPath)) {
                org.testng.Assert.fail("Could not find config.json; file was not found at: " + configJsonWindowsPath);
            }

            String text = connection.GetTextFromFile(configJsonWindowsPath);

            if (!text.contains(configJsonReportInterval)) {
                StartEPService(serviceStartStopTimeout, EP_OS.WINDOWS);
                org.testng.Assert.fail("Endpoint did not received expected configuration. Could not change the logs interval as " + configJsonReportInterval + " could not be found at: " + configJsonWindowsPath);
            }

            int start = text.indexOf(configJsonReportInterval) + configJsonReportInterval.length();
            int end = text.indexOf(",", start);
            StringBuilder builder = new StringBuilder(text);
            builder.replace(start, end, interval);

            connection.WriteTextToFile(builder.toString(), configJsonWindowsPath);

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
}

