package Actions;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.SSH.SSHManager;
import Utils.TestFiles;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;

public class LNEActions extends NepActions {
    public static final String basePrefix = "http://";
    public static final String baseSuffix = ":9091/nep-centcom-client/";
    public static final String winInstallerName = "TrustwaveEndpoint.exe";
    public static final String backupIdentifier = "backup";
    public static final String windowsHostsFile = "C:\\Windows\\System32\\drivers\\etc\\hosts";
    public static final String hostsFileRedirection = "endpoint-protection-services.local.tw-test.net";
    public static final String nepa_caDotPemPath = "C:\\Program Files\\Trustwave\\NEPAgent\\certs\\nepa_ca.pem";
    private static final String copyWinInstallerLocation = "C:\\SeleniumDownloads\\TrustwaveEndpoint.exe";

    private String LNE_IP, userNameLNE, passwordLNE;
    int LNE_SSH_port;

    public LNEActions (String LNE_IP, String userNameLNE, String passwordLNE, int LNE_SSH_port) {
        this.LNE_IP = LNE_IP;
        this.userNameLNE = userNameLNE;
        this.passwordLNE = passwordLNE;
        this.LNE_SSH_port =LNE_SSH_port;

        SetLNEBaseURI(LNE_IP);
    }

    public LNEActions ()
    {
        LNE_IP = PropertiesFile.readProperty("ClusterToTest");
        SetLNEBaseURI(LNE_IP);
    }

    public void SetLNEBaseURI(String LNE_IP){
        try {
            RestAssured.baseURI = basePrefix + LNE_IP + baseSuffix;
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not set RestAssured.baseURI for machine: " + LNE_IP  + "\n" + e.toString());
        }

    }

    public void DownloadInstaller(String lneFileCabinetPath, long  customerId, int timeout){
        try {
            SSHManager ssh = new SSHManager(userNameLNE, passwordLNE, LNE_IP, LNE_SSH_port);

            //deleting previous installer
            String clientFolder = lneFileCabinetPath + customerId;
            if (ssh.IsFileExists(clientFolder)){
                List<String> list = ssh.ListOfFiles(clientFolder);
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).contains(winInstallerName) && !list.get(i).contains(backupIdentifier)) {
                        String currentInstaller = clientFolder + "/" + list.get(i);
                        ssh.DeleteFile(currentInstaller);
                    }
                }
            }

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);
            boolean found = false;

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                if (ssh.IsFileExists(lneFileCabinetPath)) {
                    found = true;
                    break;
                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
            }

            if (!found)
                org.testng.Assert.fail("Could not find FileCabinet folder: " + lneFileCabinetPath + " at LNE machine: " + LNE_IP);

            found = false;

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                if (ssh.IsFileExists(lneFileCabinetPath)) {
                    found = true;
                    break;
                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
            }


            if (!found) {
                org.testng.Assert.fail("Could not find client FileCabinet folder: " + clientFolder + " at LNE machine: " + LNE_IP + " for customer: " + customerId);
            }

            List<String> list = null;
            found = false;
            //Get the list of files for the first time may fail because the folder was just created. Therefore wait until list of files is received with not exception for the first time.
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                try {
                    list = ssh.ListOfFilesWithoutExceptionProtection(clientFolder);
                    if(list!=null) {
                        found=true;
                        break;
                    }
                }
                catch (Exception e){
                    //if execption happened do nothing just try again
                }

                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

            }

            if (! found) {
                org.testng.Assert.fail("Could get the list of files of folder: " + clientFolder + " LNE machine: " + LNE_IP);
            }


            found = false;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).contains(winInstallerName) && !list.get(i).contains(backupIdentifier)) {
                        String source = clientFolder + "/" + list.get(i);
                        String destination = copyWinInstallerLocation;
                        ssh.CopyToLocal(source, destination);
                        found = true;
                    }
                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                list = ssh.ListOfFiles(clientFolder);

            }

            if (!found) {
                org.testng.Assert.fail("Could find file contains: " + winInstallerName + " at LNE folder: " + clientFolder + " Folder content is: " + Arrays.toString(list.toArray()) + "  LNE machine: " + LNE_IP);
            }
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not download installer contains: " + winInstallerName + " for customer: " + customerId + " from: " + lneFileCabinetPath +" machine: " + LNE_IP   + "\n" + e.toString());
        }


    }

    public void AppendToHostsFile () {
        try {
            String toAppend = "\n" + LNE_IP + " " + hostsFileRedirection;
            TestFiles.AppendToFile(windowsHostsFile, toAppend, true);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not change endpoint machine hosts file: " + windowsHostsFile  + "\n" + e.toString());
        }
    }

    public void AddCACertificate(){
        try {
            String certificate = GetCACertificate();
            TestFiles.AppendToFile(nepa_caDotPemPath, "\n\n" + certificate, false);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not add CA certificate to file: " + nepa_caDotPemPath  + "\n" + e.toString());
        }

    }

    public void InitCustomerSettings (String configJson, int fromLNEStartUntilLNEResponseOKTimeout) {
        try {

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(fromLNEStartUntilLNEResponseOKTimeout);
            int response = -1;
            boolean exception = false;
            String saveException = "";
            //From all LNE machine services are up and running until response OK there is timeout therefore put InitCustomerSettings in a wait loop
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {

                exception = false;
                try {
                    response = InitCustomerSettings(configJson);
                }
                catch (Exception e){
                    JLog.logger.info("Retrying... LNE InitCustomerSettings response exception: " + e.toString());
                    exception = true;
                    saveException = e.toString();
                }

                if (response == 200 ) {
                    break;
                }
                else if (!exception){
                    JLog.logger.info("LNE InitCustomerSettings response: " + response + " Retrying....");
                }

                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
            }
            if( exception)
                org.testng.Assert.fail("Could not init customer settings: " + saveException  +"\nLNE machine: " + LNE_IP + "\njson sent: " + configJson + "\n" );

            if (response != 200 )
                org.testng.Assert.fail("Could not init customer settings. LNE response status code received is: " + response + " after several retries during: " + fromLNEStartUntilLNEResponseOKTimeout + " Seconds. LNE machine: " + LNE_IP);
            else
                JLog.logger.info("Success. LNE InitCustomerSettings response: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not init customer settings. LNE machine: " + LNE_IP + " json sent: " + configJson + "\n" + e.toString());
        }

    }

    private int InitCustomerSettings (String configJson ) {

               Response r = given()
                       .contentType("application/json").
                             body(configJson).
                             when().
                             post("initCustomerSettings");

                return r.getStatusCode();

    }



    public void SetCustomerConfiguration (String configJson) {
        try {
            Response r = given()
                    .contentType("application/json").
                            body(configJson).
                            when().
                            post("setConfig");

            int response = r.getStatusCode();

            if (response == 200)
                JLog.logger.info("Success. LNE setConfig response: " + response);
            else
                org.testng.Assert.fail("Could not set customer configuration. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not set customer configuration. LNE machine: " + LNE_IP + " json sent: " + configJson  + "\n" + e.toString());
        }

    }

    public void CompareConfigurationToEPConfiguration (String sentConfiguration){
        String configJson=null;
        try {
            FileInputStream inputStream = new FileInputStream(configJsonPath);
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

    public String GetCACertificate () {
        try {
            RestAssured.baseURI = basePrefix + LNE_IP + ":8000/ca/CA_get.sh?ca";
            Response r = given()
                    .contentType("text/plain").
                            when().
                            post("CA_get.sh?ca");

            int response = r.getStatusCode();
            String certificate = r.getBody().print();

            //restoring base uri to default
            SetLNEBaseURI(LNE_IP);

            if (response == 200)
                JLog.logger.info("Success. LNE certificate received");
            else
                org.testng.Assert.fail("Could not get LNE CA certificate. LNE response status code received is: " + response);

            String firstLine = "";
            int indexOfFirstNewLine = -1;
            if (certificate != null && (indexOfFirstNewLine = certificate.indexOf("\n")) > 0) {
                firstLine = certificate.substring(0, indexOfFirstNewLine);
            } else {
                org.testng.Assert.fail("Could not get LNE CA certificate. LNE CA certificate received is : " + certificate);
            }

            if (firstLine != null && !(firstLine.trim().compareToIgnoreCase("success") == 0)) {
                org.testng.Assert.fail("Could not get LNE CA certificate as expected. Expected first line of LNE certificate is \"success\". LNE CA certificate received is : " + certificate);
            }

            //check if there at least one character after the first \n
            String certificateWithoutFirstLine = "";
            if (indexOfFirstNewLine + 3 < certificate.length()) {
                certificateWithoutFirstLine = certificate.substring(certificate.indexOf("\n") + 1);
            } else {
                org.testng.Assert.fail("Could not get LNE CA certificate as expected. Expected certificate to be longer than one character after success line. LNE CA certificate received is : " + certificate);

            }

            return certificateWithoutFirstLine;

        } catch (Exception e) {
            org.testng.Assert.fail("Could not add CA certificate to file: " + nepa_caDotPemPath + "\n" + e.toString());
            return null;
        }
    }



}
