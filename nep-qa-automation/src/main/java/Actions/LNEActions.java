package Actions;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.Remote.SSHManager;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static io.restassured.RestAssured.given;

public class LNEActions extends ManagerActions  {

    public static final String basePrefix = "http://";
    public static final String baseSuffix = ":9091/nep-centcom-client/";
    public static final String backupIdentifier = "backup";
    public static final String hashIdentifier = "sha256";
    public static final String nepa_caDotPemPath = "C:/Program Files/Trustwave/NEPAgent/certs/nepa_ca.pem";
    public static final String lneFileCabinetPath = "/work/services/stub-srv/var/file_cabinet/";
    public static final String caCertificateFileName = "cacertificate.txt";
    RequestSpecification requestSpecification;// = new RequestSpecBuilder().setBaseUri(url).build();

    
    //scan the document from the end and stop after 1 match
    public static final String verifyCallToCentcomCommand = "tac /work/services/stub-srv/var/log/nep/nep_dummy_services.log | grep -m 1 \"Method '%s'\"";
    
    public static ObjectMapper objectMapper = new ObjectMapper();
    
    public enum CentcomMethods{
    	REGISTER("registerEndpoint"),
    	REQUEST_UPGRADE("requestUpgrade"),
    	UPDATE_ENDPOINT("updateEndpoint"),
    	REVOKE_ENDPOINT("revokeEndpoint"),
    	RENAME_ENDPOINT("renameEndpoint"),
    	UPDATE_ENDPOINT_STATE("updateEndpointState");
    	
    	private String methodName;
    	
    	private CentcomMethods(String methodName){
    		this.methodName = methodName;
    	}

		public String getMethodName() {
			return methodName;
		}
    	
    	
    	
    }

    public LNEActions ()
    {
        LNE_IP = PropertiesFile.readProperty("ClusterToTest");
        SetLNEBaseURI(LNE_IP);
    }

    private String LNE_IP;
    int LNE_SSH_port;
    private SSHManager connection;


    public LNEActions (String LNE_IP, String userNameLNE, String passwordLNE, int LNE_SSH_port) {
        this.LNE_IP = LNE_IP;
       // this.userNameLNE = userNameLNE;
       // this.passwordLNE = passwordLNE;
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

    public void SetLNEBaseURI(String LNE_IP){
        try {
            RestAssured.baseURI = basePrefix + LNE_IP + baseSuffix;
            requestSpecification = new RequestSpecBuilder().setBaseUri(basePrefix + LNE_IP + baseSuffix).build();

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
                    if (list.get(i).contains(linuxInstallationFile) && !list.get(i).contains(backupIdentifier) && !list.get(i).contains(hashIdentifier)) {
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
                    if ( !list.get(i).contains(backupIdentifier)) {
                        if (list.get(i).contains(windowsInstallationFile) || list.get(i).contains(linuxInstallationFile) || list.get(i).contains(linuxSha256InstallationFile) ) {
                            String currentInstaller = clientFolder + "/" + list.get(i);
                            connection.DeleteFile(currentInstaller);
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not delete current installers for customer: " + customerId + " from: " + lneFileCabinetPath +" machine: " + LNE_IP   + "\n" + e.toString());
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
    
    public void InitCustomerSettingsWithDuration(String customerId, String configJson, int fromLNEStartUntilLNEResponseOKTimeout) {
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
                    response = sendInitCustomerSettings(customerId, configJson);
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
    
    private String buildJsonBody(List<String> paramNames, List<String> paramValues) {
    
    	JsonNode jsonNode = objectMapper.createObjectNode();
    	for (int i = 0; i < paramNames.size(); i++) {
    		((ObjectNode) jsonNode).put(paramNames.get(i), paramValues.get(i));
    	}

    	return jsonNode.toPrettyString();
    }



    public void InitCustomerSettings(String customerId, String configJson) {

        try {
            int response = sendInitCustomerSettings(customerId, configJson);
            if (response == 200 ) {
                JLog.logger.info("Success. LNE InitCustomerSettings response: " + response);
                return;
            }
            JLog.logger.error("LNE InitCustomerSettings response: " + response );
            org.testng.Assert.fail("Could not init customer settings. LNE response status code received is: " + response + ". LNE machine: " + LNE_IP);
        }
        catch (Exception e){
            JLog.logger.error("Could not init customer setting.", e);
            org.testng.Assert.fail("Could not init customer settings. Json sent:  {}", e );
        }

    }

    private int sendInitCustomerSettings (String customerId, String configJson ) {

    	String body = buildJsonBody( Arrays.asList("customerId", "configuration"), Arrays.asList(customerId, configJson));
    	
        Response r = given().spec(requestSpecification)
                		.contentType("application/json")
                        .body(body)
                        .when()
                        .post("initCustomerSettings");

        return r.getStatusCode();

    }
    
    public void setCustomerConfig(String customerId, String configuration) {
    	
    	String body = buildJsonBody( Arrays.asList("customerId", "configuration"), Arrays.asList(customerId, configuration));
    	
        try {
            Response r = given().spec(requestSpecification)
                    		.contentType("application/json")
                            .body(body)
                            .when()
                            .post("setConfig");

            int response = r.getStatusCode();

            if (response == 200) {
                JLog.logger.info("setCustomerConfig response: " + response);
            }
            else
                org.testng.Assert.fail("Could not set customer configuration. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not set customer config.", e);
        }

    }

    public void setClusterConfig(String customerId, String clusterName, String configJson) {
     
        String body = buildJsonBody( Arrays.asList("customerId", "clusterName", "config"), Arrays.asList(customerId, clusterName, configJson));

        try {
            Response r = given().spec(requestSpecification)
                    		.contentType("application/json")
                            .body(body)
                            .when()
                            .post("setClusterConfig");

            int response = r.getStatusCode();

            if (response == 200) {
                JLog.logger.info("setClusterConfig response: " + response);
            }
            else
                org.testng.Assert.fail("Could not set cluster configuration. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not set cluster config.", e);
        }

    }
    
    public void setEndpointConfig(String customerId, String endpointName, String configuration) {
    	
    	String body = buildJsonBody( Arrays.asList("customerId", "name", "configuration"), Arrays.asList(customerId, endpointName, configuration));
    	
        try {
            Response r = given().spec(requestSpecification)
                    		.contentType("application/json")
                            .body(body)
                            .when()
                            .post("setConfig");

            int response = r.getStatusCode();

            if (response == 200) {
                JLog.logger.info("setEndpointConfig response: " + response);
            }
            else
                org.testng.Assert.fail("Could not set endpoint configuration. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not set endpoint config.", e);
        }

    }
   
    public void updateClusterMap(Long customerId, Map<String, List<String>> assignments) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("customerId",customerId);
        JSONArray assignmentsArr = new JSONArray();
        for(String clusterName : assignments.keySet()){
            List<String> eps = assignments.get(clusterName);
            JSONObject assignmentObject = new JSONObject();
            assignmentObject.put("clusterName",clusterName);
            assignmentObject.put("endpointName",eps);
            assignmentsArr.put(assignmentObject);
        }
        jsonObject.put("assignments",assignmentsArr);
        try {
            Response r = given().spec(requestSpecification)
                    .contentType("application/json").
                            body(jsonObject.toString()).
                            when().
                            post("updateClusterMap");

            int response = r.getStatusCode();

            if (response == 200) {
                JLog.logger.info("Success. LNE updateClusterMap response: " + response);
            }
            else
                org.testng.Assert.fail("Could not update Cluster Map configuration. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not update Cluster Map configuration.", e);
        }

    }
    public void SetCustomerConfiguration (String customerId, String configuration) {
    	
    	String body = buildJsonBody( Arrays.asList("customerId", "configuration"), Arrays.asList(customerId, configuration));
        
    	try {
            Response r = given().spec(requestSpecification)
                    		.contentType("application/json")
                            .body(body)
                            .when()
                            .post("setConfig");

            int response = r.getStatusCode();

            if (response == 200) {
                JLog.logger.info("SetCustomerConfiguration response: " + response);
            }
            else
                org.testng.Assert.fail("Could not set customer configuration. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not set customer configuration.", e);
        }

    }

    public void revokeEpConfiguration (String customerId, String epName) {
    	
    	String body = buildJsonBody( Arrays.asList("customerId", "epName"), Arrays.asList(customerId, epName));
    	
        try {
            Response r = given().spec(requestSpecification)
                    		.contentType("application/json")
                            .body(body)
                            .when()
                            .post("revokeEpConfiguration");

            int response = r.getStatusCode();

            if (response == 200)
                JLog.logger.info("Success. LNE revokeEpConfiguration response: " + response);
            else
                org.testng.Assert.fail("Could not revokeEpConfiguration  . LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not revokeEpConfiguration.", e);
        }

    }

    public void revoke(String customerId, String epName) {
    	
    	String body = buildJsonBody( Arrays.asList("customerId", "epName"), Arrays.asList(customerId, epName));
    	
        try {
            Response r = given().spec(requestSpecification)
                    		.contentType("application/json")
                            .body(body)
                            .when()
                            .post("revoke");

            int response = r.getStatusCode();

            if (response == 200)
                JLog.logger.info("Success. LNE revoke response: " + response);
            else
                org.testng.Assert.fail("Failure. LNE revoke failed with response status code: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Revoke action failed.", e);
        }

    }
    
    public void deleteWithoutVerify(String customerId, String epName) {
    	
    	JLog.logger.info("Going to send delete endpoint request to Centcom client, customer: {}, ep: {}", customerId, epName);
    	
    	sendDelete(customerId, epName);
    	
    }

    public void deleteAndVerifyResponse(String customerId, String epName) {
    	
    	int response = sendDelete(customerId, epName);
    	
    	if (response == 200)
            JLog.logger.info("Success. LNE delete response: " + response);
        else
            org.testng.Assert.fail("Failure. LNE delete failed with response status code: " + response);

    }
    
    private int sendDelete(String customerId, String epName) {
    	
    	String body = buildJsonBody( Arrays.asList("customerId", "epName"), Arrays.asList(customerId, epName));
    	
        try {
            Response r = given().spec(requestSpecification)
                    		.contentType("application/json")
                            .body(body)
                            .when()
                            .post("delete");
            
            return r.getStatusCode();

            	
        }
        catch (Exception e) {
            org.testng.Assert.fail("Delete action failed.", e);
            return 500;
        }
    }

    public String GetCACertificate () {
        try {
//            RestAssured.baseURI = basePrefix + LNE_IP + ":8000/ca/CA_get.sh?ca";
            RequestSpecification requestSpecification = new RequestSpecBuilder().setBaseUri(basePrefix + LNE_IP + ":8000/ca/CA_get.sh?ca").build();
            Response r = given().spec(requestSpecification)
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

    public Map<String,String> getInstallersDocIds(long  customerId, int timeout){
        Map<String,String> installerDocIds = new HashMap<>();

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
                    if (list != null) {
                        found = true;
                        break;
                    }
                } catch (Exception e) {
                    //if exception happened do nothing just try again
                    //Get the list of files for the first time may fail because the folder was just created. Therefore wait until list of files is received with not exception for the first time.

                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

            }

            if (!found) {
                org.testng.Assert.fail("Could not get the list of files of folder: " + clientFolder + " LNE machine: " + LNE_IP);
            }

            JLog.logger.info("Waiting for windows installer file to be ready... LNE machine:" + LNE_IP);

            String copyWinInstallerLocation = PropertiesFile.getManagerDownloadFolder() + "/" + windowsInstallationFile;

            found = false;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).contains(windowsInstallationFile) && !list.get(i).contains(backupIdentifier)) {
                        String source = clientFolder + "/" + list.get(i);
                        String destination = copyWinInstallerLocation;
                        //                        connection.CopyToLocal(source, destination);
                        String[] fileNameParts = list.get(i).split("__");
                        installerDocIds.put(windowsInstallationFile,fileNameParts[fileNameParts.length-1]);
                        found = true;
                        break;
                    }
                }
                if (found) {
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
                    if (list.get(i).contains(linuxInstallationFile) && !list.get(i).contains(backupIdentifier) && !list.get(i).contains(hashIdentifier)) {
                        String source = clientFolder + "/" + list.get(i);
                        String destination = copyLinuxInstallerLocation;
                        //                        connection.CopyToLocal(source, destination);
                        String[] fileNameParts = list.get(i).split("__");
                        installerDocIds.put(linuxInstallationFile,fileNameParts[fileNameParts.length-1]);
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
                        //                        connection.CopyToLocal(source, destination);
                        String[] fileNameParts = list.get(i).split("__");
                        installerDocIds.put(linuxSha256InstallationFile,fileNameParts[fileNameParts.length-1]);
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
        return installerDocIds;
    }

    public void VerifyInstallerBackup(long customerId, Map<String, String> docIds, int timeout) {

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
                    if (list != null) {
                        found = true;
                        break;
                    }
                } catch (Exception e) {
                    //if exception happened do nothing just try again
                    //Get the list of files for the first time may fail because the folder was just created. Therefore wait until list of files is received with not exception for the first time.

                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

            }

            if (!found) {
                org.testng.Assert.fail("Could not get the list of files of folder: " + clientFolder + " LNE machine: " + LNE_IP);
            }

            JLog.logger.info("Waiting for windows installer file to be ready... LNE machine:" + LNE_IP);

            String copyWinInstallerLocation = PropertiesFile.getManagerDownloadFolder() + "/" + windowsInstallationFile;

            String docId = docIds.get(windowsInstallationFile);
            found = false;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).contains(windowsInstallationFile) && list.get(i).contains(backupIdentifier)) {
                        String source = clientFolder + "/" + list.get(i);
                        String destination = copyWinInstallerLocation;
                        //                        connection.CopyToLocal(source, destination);
                        String[] fileNameParts = list.get(i).split("__");
                        if(docId.equals(fileNameParts[fileNameParts.length-1])){
                            found = true;
                            break;
                        }
                    }
                }
                if (found) {
                    break;
                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                list = connection.ListOfFiles(clientFolder);
                //System.out.println(Arrays.toString(list.toArray()));
            }

            if (!found) {
                org.testng.Assert.fail("Could not find backup file: " + windowsInstallationFile + "with document id "+docId+" at LNE folder: " + clientFolder + " Folder content is: " + Arrays.toString(list.toArray()) + "  LNE machine: " + LNE_IP);
            }

            JLog.logger.info("Waiting for linux installer file to be ready... LNE machine:" + LNE_IP);

            String copyLinuxInstallerLocation = PropertiesFile.getManagerDownloadFolder()+ "/" + linuxInstallationFile;

            found = false;
            docId = docIds.get(linuxInstallationFile);
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).contains(linuxInstallationFile) && list.get(i).contains(backupIdentifier) && !list.get(i).contains(hashIdentifier)) {
                        String source = clientFolder + "/" + list.get(i);
                        String destination = copyLinuxInstallerLocation;
                        //                        connection.CopyToLocal(source, destination);
                        String[] fileNameParts = list.get(i).split("__");
                        if(docId.equals(fileNameParts[fileNameParts.length-1])){
                            found = true;
                            break;
                        }
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
            docId = docIds.get(linuxSha256InstallationFile);
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).contains(linuxSha256InstallationFile) && list.get(i).contains(backupIdentifier)) {
                        String source = clientFolder + "/" + list.get(i);
                        String destination = copyLinuxSha256InstallerLocation;
                        //                        connection.CopyToLocal(source, destination);
                        String[] fileNameParts = list.get(i).split("__");
                        if(docId.equals(fileNameParts[fileNameParts.length-1])){
                            found = true;
                            break;
                        }
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

    //DO we want to use the jenkins job?
    public void uploadLinuxFilesToBucket(String s3Bucket, List<String> linuxFiles) {

        String linuxUpdFolderLNE = "/home/binaries/linux/";

        String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
        String localFolderForLinuxFiles = masterDownloadDirectory + "/linux/";

        // Create folders on LNE machine
        if (!connection.IsFileExists("/home/binaries/")) {
            connection.CreateDirectory("/home/binaries/");
        }
        if (!connection.IsFileExists(linuxUpdFolderLNE)) {
            connection.CreateDirectory(linuxUpdFolderLNE);
        }

        // Copy all files from local folders to LNE
        for (String file : linuxFiles) {
            connection.CopyToRemote(localFolderForLinuxFiles + file, linuxUpdFolderLNE);
        }

        for (String file : linuxFiles) {
            String response = connection.Execute("export AWS_ACCESS_KEY_ID=`grep aws-access-key-id " + "/work" +
                    "/services/stub-srv/etc/nep-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " +
                    "AWS_SECRET_ACCESS_KEY=`grep aws-secret-access-key " + "/work/services/stub-srv/etc/nep" +
                    "-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " + "AWS_REGION=`grep aws" +
                    ".updates.bucket-region /work/services/stub-srv/etc/nep-properties/nepa-dserver" + ".properties |" +
                    " cut -d '=' -f 2` ; cd /home/; s3_cmd upload_file " + s3Bucket + " binaries" +
                    "/linux/" + file);
            JLog.logger.info("Response: " + response);
        }
    }

    //DO we want to use the jenkins job?
    public void uploadWindowsFilesToBucket(String s3Bucket, List<String> windowsFiles) {

        String windowsUpdFolderLNE = "/home/binaries/windows/";

        String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
        String localFolderForWindowsFiles = masterDownloadDirectory + "/windows/";

        // Create folders on LNE machine
        if (!connection.IsFileExists("/home/binaries/")) {
            connection.CreateDirectory("/home/binaries/");
        }
        if (!connection.IsFileExists(windowsUpdFolderLNE)) {
            connection.CreateDirectory(windowsUpdFolderLNE);
        }

        // Copy all files from local folders to LNE
        for (String file : windowsFiles) {
            connection.CopyToRemote(localFolderForWindowsFiles + file, windowsUpdFolderLNE);
        }

        for (String file : windowsFiles) {
            String response = connection.Execute("export AWS_ACCESS_KEY_ID=`grep aws-access-key-id " + "/work" +
                    "/services/stub-srv/etc/nep-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " +
                    "AWS_SECRET_ACCESS_KEY=`grep aws-secret-access-key " + "/work/services/stub-srv/etc/nep" +
                    "-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " + "AWS_REGION=`grep aws" +
                    ".updates.bucket-region /work/services/stub-srv/etc/nep-properties/nepa-dserver" + ".properties |" +
                    " cut -d '=' -f 2` ; cd /home/; s3_cmd upload_file " + s3Bucket + " binaries" +
                    "/windows/" + file);
            JLog.logger.info("Response: " + response);
        }
    }

    public void restartDsService() {
        JLog.logger.info("Restarting ds...");
        String response = connection.Execute("nep_service ds restart");
        JLog.logger.debug("Response: " + response);
    }

    public void restartStubService() {
        JLog.logger.info("Restarting stub-srv...");
        String response = connection.Execute("nep_service stub-srv restart");
        JLog.logger.debug("Response: " + response);
    }

    public void restartDsMgmtService() {
        JLog.logger.info("Restarting ds-mgmt...");
        String response = connection.Execute("nep_service ds-mgmt restart");
        JLog.logger.debug("Response: " + response);
    }
    
    public void verifyCallToCentcom(CentcomMethods method, String... params) {
    	String response = connection.Execute(String.format(verifyCallToCentcomCommand,  method.getMethodName()));
    	if (response == null) {
    		return;
    	}
    	if (response.isEmpty()) {
    		 org.testng.Assert.fail("Failed to found Centcom call for method " + method.getMethodName() + ". Response is empty");
    	}
    	for (String param : params) {
    		org.testng.Assert.assertTrue(response.contains(param), String.format("Param '%s' wasn't found in the dummy services log for method '%s'", param, method.getMethodName()));
    	}
    	JLog.logger.info("Found centcom call for method '{}' in the dummy services log!", method.getMethodName());
    }

    //TODO do we want to use the existing jenkins job?
    public void cleanLinuxBucket(String s3Bucket, List<String> linuxFiles) {

    	JLog.logger.info("Cleaning the bucket now...");

    	for (String file : linuxFiles) {
            String response = connection.Execute("export AWS_ACCESS_KEY_ID=`grep aws-access-key-id " + "/work" +
                    "/services/stub-srv/etc/nep-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " +
                    "AWS_SECRET_ACCESS_KEY=`grep aws-secret-access-key " + "/work/services/stub-srv/etc/nep" +
                    "-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " + "AWS_REGION=`grep aws" +
                    ".updates.bucket-region /work/services/stub-srv/etc/nep-properties/nepa-dserver" + ".properties |" +
                    " cut -d '=' -f 2` ; cd /home/; s3_cmd delete_file " + s3Bucket + " binaries" +
                    "/linux/" + file);
            JLog.logger.info("Response: " + response);
        }
    }

    //TODO do we want to use the existing jenkins job?
    public void cleanWindowsBucket(String s3Bucket, List<String> windowsFiles) {

    	JLog.logger.info("Cleaning the bucket now...");

    	for (String file : windowsFiles) {
            String response = connection.Execute("export AWS_ACCESS_KEY_ID=`grep aws-access-key-id " + "/work" +
                    "/services/stub-srv/etc/nep-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " +
                    "AWS_SECRET_ACCESS_KEY=`grep aws-secret-access-key " + "/work/services/stub-srv/etc/nep" +
                    "-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " + "AWS_REGION=`grep aws" +
                    ".updates.bucket-region /work/services/stub-srv/etc/nep-properties/nepa-dserver" + ".properties |" +
                    " cut -d '=' -f 2` ; cd /home/; s3_cmd delete_file " + s3Bucket + " binaries" +
                    "/windows/" + file);
            JLog.logger.info("Response: " + response);
        }
    }

    public void disableGradualUpgrade() {

        JLog.logger.info("Disabling gradual upgrade on LNE");

        String response = connection.Execute("echo \"gradual.upgrade.enable = false\" >> " +
                "/work/services/stub-srv/etc/nep-properties/nepa-dserver.properties");
    }

    public String getBucketId() {
        String response = connection.Execute("grep aws.updates.bucket-id " +
                "/work/services/stub-srv/etc/nep-properties/nepa-dserver.properties | cut -d '=' -f 2");

        return response.trim();
    }

    public void cleanGlobalVersionsTable() {
        JLog.logger.debug("Cleaning global_versions table");

        String prevWinVer = connection.Execute("echo 'select version from nep_data.global_versions where " +
                "component=\"win_binary_update_prev\"' | nep_exec mysql mysql -s -ptrustwave");
        JLog.logger.debug("windows prev version: " + prevWinVer);

        String response = connection.Execute("echo 'update nep_data.global_versions set version=\"" + prevWinVer + "\" " +
                "where component=\"win_binary_update\"' | nep_exec mysql mysql -s -ptrustwave");
        JLog.logger.debug("Reverted win version");

        String prevLinuxVer = connection.Execute("echo 'select version from nep_data.global_versions where " +
                "component=\"linux_binary_update_prev\"' | nep_exec mysql mysql -s -ptrustwave");
        JLog.logger.debug("linux prev version: " + prevLinuxVer);

        response = connection.Execute("echo 'update nep_data.global_versions set version=\"" + prevLinuxVer + "\" " +
                "where component=\"linux_binary_update\"' | nep_exec mysql mysql -s -ptrustwave");
        JLog.logger.debug("Reverted linux version");

    }
}
