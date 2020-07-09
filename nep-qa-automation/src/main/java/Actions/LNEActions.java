package Actions;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.SSH.SSHManager;
import Utils.TestFiles;
import io.restassured.RestAssured;
import io.restassured.response.Response;
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

    public void InitCustomerSettings (String configJson) {
        try {
            Response r = given()
                    .contentType("application/json").
                            body(configJson).
                            when().
                            post("initCustomerSettings");

            int response = r.getStatusCode();

            if (response == 200)
                JLog.logger.info("Success. LNE InitCustomerSettings response: " + response);
            else
                org.testng.Assert.fail("Could not init customer settings. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not init customer settings with json: " + configJson  + "\n" + e.toString());
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

            if (response == 200)
                JLog.logger.info("Success. LNE certificate received");
            else
                org.testng.Assert.fail("Could not get LNE CA certificate. LNE response status code received is: " + response);

            String certificate = r.getBody().print();
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