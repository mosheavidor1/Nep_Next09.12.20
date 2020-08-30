package Actions;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.Remote.SSHManager;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;

public class LNEActions extends ManagerActions  {

    public static final String basePrefix = "http://";
    public static final String baseSuffix = ":9091/nep-centcom-client/";
    public static final String backupIdentifier = "backup";
    public static final String nepa_caDotPemPath = "C:/Program Files/Trustwave/NEPAgent/certs/nepa_ca.pem";
    public static final String lneFileCabinetPath = "/work/services/stub-srv/var/file_cabinet/";
    public static final String caCertificateFileName = "cacertificate.txt";


    private String LNE_IP, userNameLNE, passwordLNE;
    int LNE_SSH_port;
    private SSHManager connection;


    public LNEActions (String LNE_IP, String userNameLNE, String passwordLNE, int LNE_SSH_port) {
        this.LNE_IP = LNE_IP;
        this.userNameLNE = userNameLNE;
        this.passwordLNE = passwordLNE;
        this.LNE_SSH_port =LNE_SSH_port;
        SetLNEBaseURI(LNE_IP);
        connection = new SSHManager(userNameLNE,passwordLNE,LNE_IP, LNE_SSH_port );
    }

    public void clearFile(String fileName) {
        String ClearSyslogCmd = "> " + fileName;
        connection.Execute(ClearSyslogCmd);
    }

    public String numLinesinFile(String fileName, String pattern) {
        String gz = "";
        String res = null;
        String gz_comm;
        try {
            if (!connection.IsFileExists(fileName)) {
                org.testng.Assert.fail("file is not found on LNE: " + fileName + " LNE: " + LNE_IP + "\n");
            }
            if (fileName.contains((".zip"))) {
                String unzip = "unzip -o " + fileName + " -d /tmp";
                String res_unzip = connection.Execute(unzip);
                JLog.logger.info("res_unzip: " + res_unzip);
                if ((!res_unzip.contains("extracting:")) && (!res_unzip.contains("inflating:")))
                    return null;
                int start = res_unzip.lastIndexOf("/tmp/");
                int suffix = res_unzip.lastIndexOf(".gz");
                JLog.logger.info("suffix: " + suffix + " start: " + start);
                gz = res_unzip.substring(start, suffix) + ".gz";
                JLog.logger.info("gz: " + gz);
                if (null == pattern) {
                    gz_comm = "cat " + gz + " | gzip -d | wc -l";
                    res = connection.Execute(gz_comm);
                }
                else {
                    gz_comm = "cat " + gz + " | gzip -d";
                    res = connection.Execute(gz_comm);
                    int num_of_patterns = res.split(pattern,-1).length - 1;
                    res = Integer.toString(num_of_patterns);
                }
            }
            else {
                if (null == pattern) {
                    String wc_comm = "cat " + fileName + " | wc -l";
                    res = connection.Execute(wc_comm);
                }
                else {
                    String wc_comm = "cat " + fileName;
                    res = connection.Execute(wc_comm);
                    int num_of_patterns = res.split(pattern,-1).length - 1;
                    res = Integer.toString(num_of_patterns);
                }
            }
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not check SIEM logs on LNE: " + LNE_IP + "\n" + e.toString());
        }
        finally {
            if (!gz.isEmpty())
                connection.DeleteFile(gz);
        }
        return res;
    }

    public void Close(){
        if (connection!=null) {
            connection.Close();
        }
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

    public void DownloadInstallerIncludingRequisites(long customerId, int downloadTimeout){
        CreateAndCleanDownloadFolder();
        DownloadInstallerWithoutAdditions( customerId , downloadTimeout );
        VerifyInstallerSignature();
        VerifyLinuxInstallerHash();
        WriteCACertificateToFile();

    }

    public void DownloadInstallerWithoutAdditions(long  customerId, int timeout){
        try {
            String clientFolder = lneFileCabinetPath + customerId;

            JLog.logger.info("Waiting file cabinet folder to appear... LNE machine:" + LNE_IP);
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);
            boolean found = false;

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                if (connection.IsFileExists(lneFileCabinetPath)) {
                    found = true;
                    break;
                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
            }

            if (!found) {
                org.testng.Assert.fail("Could not find FileCabinet folder: " + lneFileCabinetPath + " at LNE machine: " + LNE_IP);
            }

            JLog.logger.info("Waiting for customer folder to appear at file cabinet... LNE machine:" + LNE_IP);
            found = false;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                if (connection.IsFileExists(clientFolder)) {
                    found = true;
                    break;
                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
            }
            if (!found) {
                org.testng.Assert.fail("Could not find client FileCabinet folder: " + clientFolder + " at LNE machine: " + LNE_IP + " for customer: " + customerId);
            }

            JLog.logger.info("Getting the list of files at customer folder... LNE machine:" + LNE_IP);
            List<String> list = null;
            found = false;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                try {
                    list = connection.ListOfFilesWithoutExceptionProtection(clientFolder);
                    //System.out.println(Arrays.toString(list.toArray()));
                    if(list!=null) {
                        found=true;
                        break;
                    }
                }
                catch (Exception e){
                    //if exception happened do nothing just try again
                    //Get the list of files for the first time may fail because the folder was just created. Therefore wait until list of files is received with not exception for the first time.

                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

            }

            if (! found) {
                org.testng.Assert.fail("Could not get the list of files of folder: " + clientFolder + " LNE machine: " + LNE_IP);
            }

            JLog.logger.info("Waiting for windows installer file to be ready... LNE machine:" + LNE_IP);

            String copyWinInstallerLocation = PropertiesFile.getManagerDownloadFolder()+ "/" + windowsInstallationFile;

            found = false;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).contains(windowsInstallationFile) && !list.get(i).contains(backupIdentifier)) {
                        String source = clientFolder + "/" + list.get(i);
                        String destination = copyWinInstallerLocation;
                        connection.CopyToLocal(source, destination);
                        found = true;
                        break;
                    }
                }
                if (found ) {
                    break;
                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                list = connection.ListOfFiles(clientFolder);
                //System.out.println(Arrays.toString(list.toArray()));
            }

            if (!found) {
                org.testng.Assert.fail("Could not find file: " + windowsInstallationFile + " at LNE folder: " + clientFolder + " Folder content is: " + Arrays.toString(list.toArray()) + "  LNE machine: " + LNE_IP);
            }

            JLog.logger.info("Waiting for linux installer file to be ready... LNE machine:" + LNE_IP);

            String copyLinuxInstallerLocation = PropertiesFile.getManagerDownloadFolder()+ "/" + linuxInstallationFile;

            found = false;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).contains(linuxInstallationFile) && !list.get(i).contains(backupIdentifier)) {
                        String source = clientFolder + "/" + list.get(i);
                        String destination = copyLinuxInstallerLocation;
                        connection.CopyToLocal(source, destination);
                        found = true;
                        break;
                    }
                }
                if (found ) {
                    break;
                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                list = connection.ListOfFiles(clientFolder);
                //System.out.println(Arrays.toString(list.toArray()));
            }

            if (!found) {
                org.testng.Assert.fail("Could not find file: " + linuxInstallationFile + " at LNE folder: " + clientFolder + " Folder content is: " + Arrays.toString(list.toArray()) + "  LNE machine: " + LNE_IP);
            }

            JLog.logger.info("Waiting for installer sha file to be ready... LNE machine:" + LNE_IP);

            String copyLinuxSha256InstallerLocation = PropertiesFile.getManagerDownloadFolder()+ "/" + linuxSha256InstallationFile;

            found = false;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).contains(linuxSha256InstallationFile) && !list.get(i).contains(backupIdentifier)) {
                        String source = clientFolder + "/" + list.get(i);
                        String destination = copyLinuxSha256InstallerLocation;
                        connection.CopyToLocal(source, destination);
                        found = true;
                        break;
                    }
                }
                if (found ) {
                    break;
                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
            }

            if (!found) {
                org.testng.Assert.fail("Could not find file: " + linuxSha256InstallationFile + " at LNE folder: " + clientFolder + " Folder content is: " + Arrays.toString(list.toArray()) + "  LNE machine: " + LNE_IP);
            }
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not download installer for customer: " + customerId + " from: " + lneFileCabinetPath +" machine: " + LNE_IP   + "\n" + e.toString(), e);
        }

    }


    public void DeleteCurrentInstallerFromLNE(long  customerId) {
        try {
            JLog.logger.info("Checking if current installer exist. If yes deleting it... LNE machine:" + LNE_IP);

            //deleting previous installer
            String clientFolder = lneFileCabinetPath + customerId;

            if (connection.IsFileExists(clientFolder)) {
                List<String> list = connection.ListOfFiles(clientFolder);
                //System.out.println(Arrays.toString(list.toArray()));
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).contains(windowsInstallationFile) && !list.get(i).contains(backupIdentifier)) {
                        String currentInstaller = clientFolder + "/" + list.get(i);
                        connection.DeleteFile(currentInstaller);
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not delete current installer contains: " + windowsInstallationFile + " for customer: " + customerId + " from: " + lneFileCabinetPath +" machine: " + LNE_IP   + "\n" + e.toString());
        }
    }


    public void WriteCACertificateToFile(){
        try {
            String caFilePath = PropertiesFile.getManagerDownloadFolder() + "/" + caCertificateFileName;
            String certificate = GetCACertificate();

            PrintWriter out = new PrintWriter(caFilePath);
            out.print(certificate);
            out.close();
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not write CA certificate to file. "  + "\n" + e.toString());
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
                    String confFile = PropertiesFile.getManagerDownloadFolder()+"/" + customerConfigurationSentSuccessfullyFile;
                    FileUtils.writeStringToFile(new File(confFile),configJson, Charset.defaultCharset());
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

            if (response == 200) {
                JLog.logger.info("Success. LNE setConfig response: " + response);
                String confFile = PropertiesFile.getManagerDownloadFolder()+"/" + customerConfigurationSentSuccessfullyFile;
                FileUtils.writeStringToFile(new File(confFile),configJson, Charset.defaultCharset());
            }
            else
                org.testng.Assert.fail("Could not set customer configuration. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not set customer configuration. LNE machine: " + LNE_IP + " json sent: " + configJson  + "\n" + e.toString());
        }

    }

    public void revokeEpConfiguration (String configJson) {
        try {
            Response r = given()
                    .contentType("application/json").
                            body(configJson).
                            when().
                            post("revokeEpConfiguration");

            int response = r.getStatusCode();

            if (response == 200)
                JLog.logger.info("Success. LNE revokeEpConfiguration response: " + response);
            else
                org.testng.Assert.fail("Could not revokeEpConfiguration  . LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not revokeEpConfiguration. LNE machine: " + LNE_IP + " json sent: " + configJson  + "\n" + e.toString());
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
