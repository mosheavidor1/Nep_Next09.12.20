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


    public static final String windowsInstallationFile = ManagerActions.windowsInstallationFile;
    public static final String installationFolder = "/C:/Program Files/Trustwave/NEPAgent";
    public static final String nepa_caDotPemFileName = "nepa_ca.pem";
    public static final String nepa_caDotPemPath = "C:/Program Files/Trustwave/NEPAgent/certs/";

    public static final String windowsHostsFile = "/C:/Windows/System32/drivers/etc/hosts";
    public static final String hostsFileRedirection = "endpoint-protection-services.local.tw-test.net";
    public static final char hostsFileCommentChar = '#';


    public static final String dbJsonPath = "/C:/ProgramData/Trustwave/NEPAgent/db.json";
    public static final String configJsonPath = "/C:/ProgramData/Trustwave/NEPAgent/config.json";

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

    public void InstallEPIncludingRequisites(int installationTimeout, int epServiceTimeout, int dbJsonToShowActiveTimeout){
        UnInstallEndPoint(installationTimeout);
        CopyInstaller();
        AppendToHostsFile();
        InstallEndPointWithoutAdditions(installationTimeout);
        StopEPService(epServiceTimeout);
        AddCaCertificate();
        StartEPService(epServiceTimeout);
        CheckEndPointActiveByDbJson(dbJsonToShowActiveTimeout);
    }


    public void InstallEndPointWithoutAdditions(int timeout) {
        try {
            JLog.logger.info("Installing EP...");
            String installerLocation = PropertiesFile.readProperty("EPDownloadFolder");
            installerLocation +=  "/" + windowsInstallationFile;

            if (! connection.IsFileExists(installerLocation)){
                org.testng.Assert.fail("Could not find installation file at the following path: " + installerLocation + " At machine: "+ epIp);
            }

            installerLocation = installerLocation.substring(1);
            String command = installerLocation + " /q ";//+ host;
            connection.Execute(command);

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);
            boolean found = false;

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                if (EndPointServiceExist()) {
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
                if (!EndPointServiceExist()) {
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


    public void StopEPService(int timeout) {
        try {

            connection.Execute("Net stop NepaService");

            boolean active = true;
            String result = "";

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                result = connection.Execute("sc query \"NepaService\"");
                if (result.contains("STOPPED")) {
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

    public void StartEPService(int timeout) {
        try {
            connection.Execute("Net start NepaService");

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            boolean active = false;

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

                String result = connection.Execute("sc query \"NepaService\"");
                if (result.contains("RUNNING")) {
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


    public void CopyInstaller(){
        String installerDestination=null;
        try {
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
            String epDownloadDirectory = PropertiesFile.readProperty("EPDownloadFolder");
            if(! connection.IsFileExists(epDownloadDirectory)) {
                if (SystemUtils.IS_OS_WINDOWS && !connection.IsFileExists("/C:/home")) {
                    connection.CreateDirectory("/C:/home");
                }
                connection.CreateDirectory(epDownloadDirectory);
            }
            installerDestination =  epDownloadDirectory + "/" + windowsInstallationFile;
            String installerSource = masterDownloadDirectory + "/" + windowsInstallationFile;
            connection.CopyToRemote(installerSource, installerDestination);

        }
        catch ( Exception e) {
            org.testng.Assert.fail("Could not add ca certificate to file: " + installerDestination +" EP machine: " + epIp + "\n" + e.toString());
        }

    }


    public void AddCaCertificate()  {
        String remoteCaFile=null;
        try {
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();

            remoteCaFile = "/" + nepa_caDotPemPath + nepa_caDotPemFileName;
            String localCaFile = masterDownloadDirectory + "/" + nepa_caDotPemFileName;
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

    public void CompareConfigurationToEPConfiguration (String sentConfiguration){
        String configJson=null;
        try {
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();

            String localFilePath = masterDownloadDirectory + "/" + "ConfigJsonCopy.txt";
            connection.CopyToLocal(configJsonPath,localFilePath);

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

    public void AppendToHostsFile () {
        try {
            String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();

            String localHostsCopy = masterDownloadDirectory + "/" + "HostsCopy";
            connection.CopyToLocal(windowsHostsFile,localHostsCopy);
            TestFiles.RemoveLines(localHostsCopy, hostsFileRedirection, hostsFileCommentChar);
            String toAppend = "\n" + PropertiesFile.readProperty("ClusterToTest") + " " + hostsFileRedirection;
            //To check why this not failed
            //TestFiles.AppendToFile(windowsHostsFile, toAppend, true);
            TestFiles.AppendToFile(localHostsCopy, toAppend, true);
            connection.CopyToRemote(localHostsCopy,windowsHostsFile);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not change endpoint machine hosts file: " + windowsHostsFile  + "\n" + e.toString());
        }

    }



    public void CheckEndPointActiveByDbJson(int timeout) {
        try {

            String text = "";
            boolean active = false;
            //File file = new File(dbJsonPath);

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                if (connection.IsFileExists(dbJsonPath)) {
                    text =connection.GetTextFromFile(dbJsonPath);
                    if (text.contains("\"EndpointId\": \"") && text.contains("\"DsInitialHost\": ")) {
                        active = true;
                        break;
                    }
                }

                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

            }

            if (! connection.IsFileExists(dbJsonPath))
                org.testng.Assert.fail("Endpoint is not connected - db.json file was not found at: " + dbJsonPath + " after timeout(sec): " + timeout);

            if (!active)
                org.testng.Assert.fail("Endpoint is not connected according to db.json file after timeout(sec): " + timeout + ". Failed to find End Point ID  Or Host in db.json: " + dbJsonPath + "\ndb.json file content:\n" + text);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not check if endpoint is active by db.json file." + "\n" + e.toString());
        }


    }


    public boolean EndPointServiceExist() {
        try {
            //String result = execCmd("net start | find \"Trustwave Endpoint Agent Service\"");
            String result = connection.Execute("net start");
            if (result.contains("Trustwave Endpoint Agent Service"))
                return true;
            else
                return false;

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not check if endpoint service exist." + "\n" + e.toString());
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

            StopEPService(serviceStartStopTimeout);
            if (!connection.IsFileExists(configJsonPath)) {
                org.testng.Assert.fail("Could not find config.json; file was not found at: " + configJsonPath);
            }

            String text = connection.GetTextFromFile(configJsonPath);

            if (!text.contains(configJsonReportInterval)) {
                StartEPService(serviceStartStopTimeout);
                org.testng.Assert.fail("Endpoint did not received expected configuration. Could not change the logs interval as " + configJsonReportInterval + " could not be found at: " + configJsonPath);
            }

            int start = text.indexOf(configJsonReportInterval) + configJsonReportInterval.length();
            int end = text.indexOf(",", start);
            StringBuilder builder = new StringBuilder(text);
            builder.replace(start, end, interval);

            connection.WriteTextToFile(builder.toString(),configJsonPath);

            StartEPService(serviceStartStopTimeout);
        }

        catch (Exception e) {
            org.testng.Assert.fail("Could not change endpoint report interval." + "\n" + e.toString());
        }


    }



}

