package Tests.LNE;

import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.Data.GlobalTools;
import Utils.PropertiesFile.PropertiesFile;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.*;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;


// Currently when running this test, there should be 2 endpoints defined in data file
// First one is windows, the second is linux
// s3 bucket should be created ahead of test and defined in data file
// The test does the following:
// 1. Uninstalls the EPs
// 2. Populates the s3 bucket with files for update
// 3. Installs the EPs
// 4. Waits for update
// 5. Verifies update on both EPs
// 6. Cleans the bucket

public class BinaryUpdate extends GenericTest {

    private BaseAgentActions endpoint;

    private String s3Bucket;
    
    private static final LNEActions lneActions = GlobalTools.getLneActions();

    @Factory(dataProvider = "getData")
    public BinaryUpdate(Object dataToSet) {
        super(dataToSet);
    }
    
    //binary update should be last because it installs version that do not support proxy
    @Test(groups="Binary",priority=1001)
    public void TestBinaryUpdate() throws IOException {

        JLog.logger.info("Starting binary update test");

        // get bucket id from LNE
        s3Bucket = lneActions.getBucketId();
        JLog.logger.info("s3 bucket id is: " + s3Bucket);

        lneActions.disableGradualUpgrade();

        endpoint = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"),
                data.get("EP_UserName_1"), data.get("EP_Password_1"));

        if(data.get("EP_Type_1").equals("win")) {
            String windowsNewVer = data.get("update_win_ver");
            FetchFilesFromNexusToLocalForWindows(windowsNewVer);
            checkBinaryUpdateOnWindowsOnly(endpoint, windowsNewVer);
            verifyUpdatedVersion(windowsNewVer, endpoint);
        }
        else {
            String linuxNewVer = data.get("update_linux_ver");
            FetchFilesFromNexusToLocalForLinux(linuxNewVer);
            checkBinaryUpdateOnLinuxOnly(endpoint, linuxNewVer);
            verifyUpdatedVersion(linuxNewVer, endpoint);
        }

        cleanGlobalVersionsDB();
        //cleanBinVerEpRequest();
    }

    //binary update should be last because it installs version that do not support proxy
    @Test(groups="Binary",priority=1000)
    public void TestBinaryUpdateRollback() throws IOException {

        JLog.logger.info("Starting binary update rollback test");

        // get bucket id from LNE
        s3Bucket = lneActions.getBucketId();
        JLog.logger.info("s3 bucket id is: " + s3Bucket);

        lneActions.disableGradualUpgrade();

        endpoint = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"),
                data.get("EP_UserName_1"), data.get("EP_Password_1"));

        if(data.get("EP_Type_1").equals("win")) {
            String windowsNewVer = data.get("update_bad_win_ver");
            FetchFilesFromNexusToLocalForWindows(windowsNewVer);
            checkBinaryUpdateOnWindowsOnly(endpoint, windowsNewVer);
        }
        else {
            String linuxNewVer = data.get("update_bad_linux_ver");
            FetchFilesFromNexusToLocalForLinux(linuxNewVer);
            checkBinaryUpdateOnLinuxOnly(endpoint, linuxNewVer);
        }

        JLog.logger.info("Sleeping for 1 minute so endpoint would have time to report on an error...");
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            JLog.logger.error("Interrupted exception in Thread.sleep: ", e);
        }

        VerifySuccessfulRollback(endpoint);
    }


    @AfterTest
    public void  AfterBinaryUpdateTestCleanup(){
        JLog.logger.debug("BinaryUpdate test - @AfterTest Cleanup... ");
        cleanGlobalVersionsDB();
        cleanBinVerEpRequest();
    }


    private void cleanGlobalVersionsDB(){
        getDbConnector().cleanGlobalVersionsAfterBinaryUpdate();
    }

    private void cleanBinVerEpRequest(){
        getDbConnector().cleanEndpointBinVerEpRequest();
    }

    private void FetchFilesFromNexusToLocalForWindows(String windowsNewVer) throws IOException {

        //String windowsNewVer = data.get("update_win_ver");
        List<String> windowsFiles = WindowsFilesList(windowsNewVer);

        String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
        String localFolderForWinFiles = masterDownloadDirectory + "/windows/";

        String fullPathToNexusWinVer = "https://nexus01.trustwave.com/content/repositories/releases/com/trustwave/nep/tmp/" + windowsNewVer + "/windows/";
        FetchFiles(fullPathToNexusWinVer, windowsFiles, localFolderForWinFiles);
    }

    private void FetchFilesFromNexusToLocalForLinux(String linuxNewVer) throws IOException {
        //String linuxNewVer = data.get("update_linux_ver");
        List<String> linuxFiles = LinuxFilesList(linuxNewVer);

        String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
        String localFolderForLinuxFiles = masterDownloadDirectory + "/linux/";

        String fullPathToNexusLinuxVer = "https://nexus01.trustwave.com/content/repositories/releases/com/trustwave/nep/tmp/" + linuxNewVer + "/linux/";
        FetchFiles(fullPathToNexusLinuxVer, linuxFiles, localFolderForLinuxFiles);
    }

    //@BeforeTest
    private void AddS3FlagAndRestartLennyServices() {
        //TODO:Discuss how to do it
    	/*
    	
        JLog.logger.info("Adding s3 flag to LNE machine");
        String response = connection.Execute("touch /work/flag-s3-lifetime");
        JLog.logger.info("Response: " + response);

        response = connection.Execute("/work/tools/bin/s3-blocker /work/flag-s3-lifetime 12");
        JLog.logger.info("Response: " + response);
        // restart all services
        response = connection.Execute("nep_service all restart");
     // Sleep for 1 minute - so ds and ds-mgmt will start
        
        try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			
		}
		
		*/
    }
    /*
    @Test()
    public void CheckSuccessfulBinaryUpdate() throws IOException, InterruptedException {
        String windowsNewVer = data.get("update_win_ver");
        String linuxNewVer = data.get("update_linux_ver");
        try {
            BasicBinaryUpdateFlow(windowsNewVer, linuxNewVer);
            VerifySuccessfulUpdate(windowsNewVer, linuxNewVer);
            JLog.logger.info("Done...");
        } catch (Exception ex) {
            ex.printStackTrace();
            org.testng.Assert.fail("Failure in CheckSuccessfulBinaryUpdate" + "\n" + ex.toString());
        }finally {
            // Clean the bucket
            cleanLinuxBucket(linuxNewVer);
            cleanWindowsBucket(windowsNewVer);
            JLog.logger.info("s3bucket cleaned...");
        }
    }*/

    /**
     * This test uploads new binary update only for Linux. We expect the Linux endpoint to upgrade while the windows will not.
     *
     */
    public void checkBinaryUpdateOnLinuxOnly (BaseAgentActions endpointLinux, String linuxNewVer) {

        JLog.logger.info("Starting linux binary update test");

        //String linuxNewVer = data.get("update_linux_ver");

        try {
            lneActions.uploadLinuxFilesToBucket(s3Bucket, LinuxFilesList(linuxNewVer));

            // Restarting services so it will know new binary package is uploaded
            // (instead of waiting for 15 minutes)
            lneActions.restartStubService();
            lneActions.restartDsMgmtService();
            lneActions.restartDsService();
//            lneActions.restartServiceWaitForFinish(LNEActions.NepService.DS,120);
//            lneActions.restartServiceWaitForFinish(LNEActions.NepService.DS_MGMT,120);
            // Sleep for 1 minute - so all services will start
            try {
                JLog.logger.info("Sleeping for 1 minute so services restart will finish...");
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                JLog.logger.error("Error in checkBinaryUpdateOnLinuxOnly");
            }

            JLog.logger.info("Installing linux EP...");

	         endpointLinux.reinstallEndpoint(Integer.parseInt(getGeneralData().get("EP " +
	                         "Installation timeout")), Integer.parseInt(getGeneralData().get("EP Service Timeout"))
	                 );

            // Sleep for 2 minutes - so the EPs will check updates and be updated
            JLog.logger.info("Sleeping for 3 minutes so check updates would update the EPs...");
            Thread.sleep(180000);

            if(!endpointLinux.endpointServiceRunning())
                org.testng.Assert.fail("Binary update failed. Trustwave Endpoint Agent Service not running on the linux EP.");
            else
                JLog.logger.info("Trustwave Endpoint Agent Service is running on the linux machine");

        } catch (Exception ex) {
            ex.printStackTrace();
            org.testng.Assert.fail("Failure in CheckSuccessfulBinaryUpdate" + "\n" + ex.toString());
        }finally {
            // Clean the bucket
            lneActions.cleanLinuxBucket(s3Bucket, LinuxFilesList(linuxNewVer));
            JLog.logger.info("s3Bucket cleaned...");
        }
    }

    public void checkBinaryUpdateOnWindowsOnly(BaseAgentActions endpointWin, String windowsNewVer) {

        JLog.logger.info("Starting windows binary update test");
        //String windowsNewVer = data.get("update_win_ver");

        try {
            lneActions.uploadWindowsFilesToBucket(s3Bucket, WindowsFilesList(windowsNewVer));

            // Restarting services so it will know new binary package is uploaded
            // (instead of waiting for 15 minutes)
            lneActions.restartStubService();
            lneActions.restartDsMgmtService();
            lneActions.restartDsService();
//            lneActions.restartStubServiceWaitForFinish(120);
//            lneActions.restartServiceWaitForFinish(LNEActions.NepService.DS_MGMT,120);
//            lneActions.restartServiceWaitForFinish(LNEActions.NepService.DS,120);
            // Sleep for 1 minute - so all services will start
            try {
                JLog.logger.info("Sleeping for 1 minute so services restart will finish...");
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                JLog.logger.error("Error in checkBinaryUpdateOnWindowsOnly");
            }

            JLog.logger.info("Installing windows EP...");

            endpointWin.reinstallEndpoint(Integer.parseInt(getGeneralData().get("EP " +
                            "Installation timeout")), Integer.parseInt(getGeneralData().get("EP Service Timeout")));

            // Sleep for 2 minutes - so the EPs will check updates and be updated
            JLog.logger.info("Sleeping for 3 minutes so check updates would update the EPs...");
            Thread.sleep(180000);
            //Thread.sleep(240000);

            if(!endpointWin.endpointServiceRunning())
                org.testng.Assert.fail("Binary update failed. Trustwave Endpoint Agent Service not running on the windows EP.");
            else
                JLog.logger.info("Trustwave Endpoint Agent Service is running on the windows machine");

        } catch (Exception ex) {
            ex.printStackTrace();
            org.testng.Assert.fail("Failure in CheckSuccessfulBinaryUpdate" + "\n" + ex.toString());
        }finally {
            // Clean the bucket
            lneActions.cleanWindowsBucket(s3Bucket, WindowsFilesList(windowsNewVer));
            JLog.logger.info("s3Bucket cleaned...");
        }
    }

    private void verifyUpdatedVersion(String newVer, BaseAgentActions endpoint) {
        String epVer = endpoint.getEPBinaryVersion();

        JLog.logger.info("Endpoint agent version is: " + epVer);

        org.testng.Assert.assertEquals(epVer, newVer, "Versions don't match");
    }
    
/*
   
    private void BasicBinaryUpdateFlow(String windowsNewVer, String linuxNewVer) throws InterruptedException, IOException {



        endpointWin = new AgentActions(data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        endpointLinux = new AgentActions(data.get("EP_HostName_2"), data.get("EP_UserName_2"), data.get(
                "EP_Password_2"));


        //TODO: Do we want to do it as part of the test?
        // Upload all files to s3 bucket
        String s3Bucket = data.get("s3Bucket");
        uploadLinuxFilesToBucket(s3Bucket, linuxNewVer);
        uploadLinuxFilesToBucket(s3Bucket, windowsNewVer);
       

        //TODO do we actually need to install the EP or can we just use existing ones

        JLog.logger.info("Installing windows EP...");
        // TODO RBRB make order of EPs irrelevant

        endpointWin.InstallEPIncludingRequisites(AgentActions.EP_OS.WINDOWS, Integer.parseInt(getGeneralData().get("EP " +
                        "Installation timeout")), Integer.parseInt(getGeneralData().get("EP Service Timeout")),
                Integer.parseInt(getGeneralData().get("From EP service start until logs show EP active timeout")));

        JLog.logger.info("Installing linux EP...");

        endpointLinux.InstallEPIncludingRequisites(AgentActions.EP_OS.LINUX, Integer.parseInt(getGeneralData().get("EP " +
                        "Installation timeout")), Integer.parseInt(getGeneralData().get("EP Service Timeout")),
                Integer.parseInt(getGeneralData().get("From EP service start until logs show EP active timeout")));

        // Sleep for 2 minutes - so the EPs will check updates and be updated
        JLog.logger.info("Sleeping for 2 minutes so check updates would update the EPs...");
        Thread.sleep(120000);

        // Verify endpoint service is running
        if(!endpointWin.EndPointServiceExist(AgentActions.EP_OS.WINDOWS))
            org.testng.Assert.fail("Binary update failed. Trustwave Endpoint Agent Service not running on the windows EP.");
        else
            JLog.logger.info("Trustwave Endpoint Agent Service is running on the windows machine");

        if(!endpointLinux.EndPointServiceExist(AgentActions.EP_OS.LINUX))
            org.testng.Assert.fail("Binary update failed. Trustwave Endpoint Agent Service not running on the linux EP.");
        else
            JLog.logger.info("Trustwave Endpoint Agent Service is running on the linux machine");
    }

*/


    // String s3Bucket = data.get("s3Bucket");
    private void uploadWindowsFilesToBucket(String s3Bucket, String version) {
/*
        List<String> windowsFiles = WindowsFilesList(version);

        
        String winUpdFolderLNE = "/home/binaries/windows/";

        String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
        String localFolderForWinFiles = masterDownloadDirectory + "/windows/";

        // Create folders on LNE machine
        if (!connection.IsFileExists("/home/binaries/")) {
            connection.CreateDirectory("/home/binaries/");
        }
        if (!connection.IsFileExists(winUpdFolderLNE)) {
            connection.CreateDirectory(winUpdFolderLNE);
        }

        // Copy all files from local folders to LNE
        for (String file : windowsFiles) {
            connection.CopyToRemote(localFolderForWinFiles + file, winUpdFolderLNE);
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
        } */
    }

    //TODO do we want to use the existing jenkins job?
    private void cleanWindowsBucket(String windowsNewVer) {
/*
        String s3Bucket = data.get("s3Bucket");
        //String windowsNewVer = data.get("update_win_ver");
        //String linuxNewVer = data.get("update_linux_ver");

        List<String> windowsFiles = WindowsFilesList(windowsNewVer);
        

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
        */
    }
    /*
        private void VerifySuccessfulUpdate(String windowsNewVer, String linuxNewVer) {
            // Verify that the EPs have the correct new versions
            String linuxBinVer = endpointLinux.getEPBinaryVersion(AgentActions.EP_OS.LINUX);
            String winBinVer = endpointWin.getEPBinaryVersion(AgentActions.EP_OS.WINDOWS);

            JLog.logger.info("Linux version is: " + linuxBinVer);
            JLog.logger.info("Windows version is: " + winBinVer);

            org.testng.Assert.assertEquals(linuxBinVer, linuxNewVer, "Versions don't match");
            org.testng.Assert.assertEquals(winBinVer, windowsNewVer, "Versions don't match");
        }
    */
    private void VerifySuccessfulRollback(BaseAgentActions endpoint) {

        String errorMsg =  getDbConnector().getEpErrorMsg(endpoint.getEpName());

        JLog.logger.debug("EP " + endpoint.getEpName() + " error message: " + errorMsg);

        org.testng.Assert.assertTrue(errorMsg.contains("rollback"), "The error message doesn't contain rollback: " + errorMsg);
        JLog.logger.debug("Rollback succeeded");

    }

    private void FetchFiles(String nexusPath, List<String> files, String localFolder) throws IOException {

        for (String file : files) {
            String fileUrl = nexusPath + file;
            String localFile = localFolder + file;
            JLog.logger.info("Copying file " + fileUrl + " to " + localFile);
            FileUtils.copyURLToFile(new URL(fileUrl), new File(localFile));
        }
    }

    private List<String> WindowsFilesList(String windowsNewVer) {
        List<String> files = new LinkedList<>();
        String winZipFile = "tw-endpoint-wnd-upd-" + windowsNewVer + ".zip";
        files.add(winZipFile);
        files.add(winZipFile + ".md5");
        files.add(winZipFile + ".sha1");
        files.add("version.txt");
        return files;
    }

    private List<String> LinuxFilesList(String linuxNewVer) {
        List<String> files = new LinkedList<>();
        String linuxZipFile = "tw-endpoint-lnx-upd-" + linuxNewVer + ".zip";
        files.add(linuxZipFile);
        files.add(linuxZipFile + ".md5");
        files.add(linuxZipFile + ".sha1");
        files.add("version.txt");
        return files;
    }



    @AfterTest
    public void Close() {
        if (endpoint != null) {
            JLog.logger.info("Closing endpoint");
            endpoint.close();
        }
    }


    //@Test()
    public void CheckBinaryUpdateFailureCorruptedExecutables() throws IOException, InterruptedException {
/*
        String windowsNewVer = data.get("update_bad_win_ver");
        String linuxNewVer = data.get("update_bad_linux_ver");

        // restart all services
        connection = new SSHManager(getGeneralData().get("LNE User Name"), getGeneralData().get("LNE Password"),
                GlobalTools.getClusterToTest(), Integer.parseInt(getGeneralData().get("LNE SSH port")));

        JLog.logger.info("Restarting all services on LNE machine");
        String response = connection.Execute("nep_service all restart");
        JLog.logger.info("Response: " + response);

        // Sleep for 1 minute - so ds and ds-mgmt will start
        JLog.logger.info("Waiting 1 minute for ds and ds-mgmt to start...");
        Thread.sleep(60000);

        JLog.logger.info("Uninstalling EPs...");

        endpointWin = new AgentActions(data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        endpointLinux = new AgentActions(data.get("EP_HostName_2"), data.get("EP_UserName_2"), data.get(
                "EP_Password_2"));

        endpointWin.UninstallEndpoint(AgentActions.EP_OS.WINDOWS,Integer.parseInt(getGeneralData().get("EP Installation timeout")));
        endpointLinux.UninstallEndpoint(AgentActions.EP_OS.LINUX, Integer.parseInt(getGeneralData().get("EP Installation timeout")));

        // Get newer binaries
        // FetchFilesFromNexusToLocal(windowsNewVer, linuxNewVer);

        // Upload all files to s3 bucket
        // UploadFilesToBucket(windowsNewVer, linuxNewVer);

        // -----------------------------------------------------------------
        // TEMPORARY SECTION

        String s3Bucket = data.get("s3Bucket");
        //String windowsNewVer = data.get("update_win_ver");
        //String linuxNewVer = data.get("update_linux_ver");

        List<String> windowsFiles = WindowsFilesList(windowsNewVer);
        List<String> linuxFiles = LinuxFilesList(linuxNewVer);

        // Create folders on LNE machine
        String winUpdFolderLNE = "/home/binaries/windows/";
        String linuxUpdFolderLNE = "/home/binaries/linux/";

        //String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
        //String localFolderForWinFiles = masterDownloadDirectory + "/windows/";
        //String localFolderForLinuxFiles = masterDownloadDirectory + "/linux/";

        String localFolderForWinFiles = "C:\\home\\Packages\\Win0Bytes\\";
        String localFolderForLinuxFiles = "C:\\home\\Packages\\Linux0Bytes\\";

        if (!connection.IsFileExists("/home/binaries/")) {
            connection.CreateDirectory("/home/binaries/");
        }
        if (!connection.IsFileExists(winUpdFolderLNE)) {
            connection.CreateDirectory(winUpdFolderLNE);
        }
        if (!connection.IsFileExists(linuxUpdFolderLNE)) {
            connection.CreateDirectory(linuxUpdFolderLNE);
        }

        // Copy all files from local folders to LNE
        for (String file : windowsFiles) {
            connection.CopyToRemote(localFolderForWinFiles + file, winUpdFolderLNE);
        }
        for (String file : linuxFiles) {
            connection.CopyToRemote(localFolderForLinuxFiles + file, linuxUpdFolderLNE);
        }

        for (String file : windowsFiles) {
            response = connection.Execute("export AWS_ACCESS_KEY_ID=`grep aws-access-key-id " + "/work" +
                    "/services/stub-srv/etc/nep-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " +
                    "AWS_SECRET_ACCESS_KEY=`grep aws-secret-access-key " + "/work/services/stub-srv/etc/nep" +
                    "-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " + "AWS_REGION=`grep aws" +
                    ".updates.bucket-region /work/services/stub-srv/etc/nep-properties/nepa-dserver" + ".properties |" +
                    " cut -d '=' -f 2` ; cd /home/; s3_cmd upload_file " + s3Bucket + " binaries" +
                    "/windows/" + file);
            JLog.logger.info("Response: " + response);
        }
        for (String file : linuxFiles) {
            response = connection.Execute("export AWS_ACCESS_KEY_ID=`grep aws-access-key-id " + "/work" +
                    "/services/stub-srv/etc/nep-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " +
                    "AWS_SECRET_ACCESS_KEY=`grep aws-secret-access-key " + "/work/services/stub-srv/etc/nep" +
                    "-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " + "AWS_REGION=`grep aws" +
                    ".updates.bucket-region /work/services/stub-srv/etc/nep-properties/nepa-dserver" + ".properties |" +
                    " cut -d '=' -f 2` ; cd /home/; s3_cmd upload_file " + s3Bucket + " binaries" +
                    "/linux/" + file);
            JLog.logger.info("Response: " + response);
        }

        // -----------------------------------------------------------------
        // -----------------------------------------------------------------

        // Add flag for s3 bucket and restart all services
        AddS3FlagAndRestartLennyServices();

        // Sleep for 1 minute - so ds and ds-mgmt will start
        JLog.logger.info("Waiting 1 minute for ds and ds-mgmt to start...");
        Thread.sleep(60000);

        JLog.logger.info("Installing windows EP...");
        // TODO RBRB make order of EPs irrelevant

        endpointWin.InstallEPIncludingRequisites(AgentActions.EP_OS.WINDOWS, Integer.parseInt(getGeneralData().get("EP " +
                        "Installation timeout")), Integer.parseInt(getGeneralData().get("EP Service Timeout")),
                Integer.parseInt(getGeneralData().get("From EP service start until logs show EP active timeout")));

        JLog.logger.info("Installing linux EP...");

        endpointLinux.InstallEPIncludingRequisites(AgentActions.EP_OS.LINUX, Integer.parseInt(getGeneralData().get("EP " +
                        "Installation timeout")), Integer.parseInt(getGeneralData().get("EP Service Timeout")),
                Integer.parseInt(getGeneralData().get("From EP service start until logs show EP active timeout")));

        // Sleep for 2 minutes - so the EPs will check updates and be updated
        JLog.logger.info("Sleeping for 2 minutes so check updates would update the EPs...");
        Thread.sleep(120000);

        // Verify endpoint service is running
        if(!endpointWin.EndPointServiceExist(AgentActions.EP_OS.WINDOWS))
            org.testng.Assert.fail("Binary update failed. Trustwave Endpoint Agent Service not running on the windows EP.");
        else
            JLog.logger.info("Trustwave Endpoint Agent Service is running on the windows machine");

        if(!endpointLinux.EndPointServiceExist(AgentActions.EP_OS.LINUX))
            org.testng.Assert.fail("Binary update failed. Trustwave Endpoint Agent Service not running on the linux EP.");
        else
            JLog.logger.info("Trustwave Endpoint Agent Service is running on the linux machine");

        // -----------------------------------------------------------------------------------
        //BasicBinaryUpdateFlow();
*/
        String windowsNewVer = data.get("update_bad_win_ver");
        String linuxNewVer = data.get("update_bad_linux_ver");
        try {
            // BasicBinaryUpdateFlow(windowsNewVer, linuxNewVer);

            // VerifySuccessfulRollback();

            JLog.logger.info("Done...");
        } catch (Exception ex) {
            ex.printStackTrace();
            org.testng.Assert.fail("Failure in CheckBinaryUpdateFailureCorruptedExecutables" + "\n" + ex.toString());
        }finally {
            // Clean the bucket
            // CleanBucket(windowsNewVer, linuxNewVer);
            //JLog.logger.info("s3bucket cleaned...");
        }
    }


}
