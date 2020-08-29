package Tests.LNE;

import Actions.AgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.Remote.SSHManager;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import static java.lang.System.exit;

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

    private AgentActions endpointWin;
    private AgentActions endpointLinux;

    //private LNEActions manager;
    private SSHManager connection;

    @Factory(dataProvider = "getData")
    public BinaryUpdate(Object dataToSet) {
        super(dataToSet);
    }

    @Test()
    public void CheckBinaryUpdate() throws IOException, InterruptedException {

        // restart all services
        connection = new SSHManager(general.get("LNE User Name"), general.get("LNE Password"),
                PropertiesFile.readProperty("ClusterToTest"), Integer.parseInt(general.get("LNE SSH port")));

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

        endpointWin.UnInstallEndPoint(Integer.parseInt(general.get("EP Installation timeout")));
        endpointLinux.UnInstallLinuxEndPoint(Integer.parseInt(general.get("EP Installation timeout")));

        // Get newer binaries
        String windowsNewVer = data.get("update_win_ver");
        String linuxNewVer = data.get("update_linux_ver");

        List<String> windowsFiles = WindowsFilesList(windowsNewVer);
        List<String> linuxFiles = LinuxFilesList(linuxNewVer);

        String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();
        String localFolderForWinFiles = masterDownloadDirectory + "/windows/";
        String localFolderForLinuxFiles = masterDownloadDirectory + "/linux/";

        String fullPathToNexusWinVer = "https://nexus01.trustwave.com/content/repositories/releases/com/trustwave/nep/tmp/" + windowsNewVer + "/windows/";
        String fullPathToNexusLinuxVer = "https://nexus01.trustwave.com/content/repositories/releases/com/trustwave/nep/tmp/" + linuxNewVer + "/linux/";

        FetchFiles(fullPathToNexusWinVer, windowsFiles, localFolderForWinFiles);
        FetchFiles(fullPathToNexusLinuxVer, linuxFiles, localFolderForLinuxFiles);

        // Create folders on LNE machine
        String winUpdFolderLNE = "/home/binaries/windows/";
        String linuxUpdFolderLNE = "/home/binaries/linux/";

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

        String s3Bucket = data.get("s3Bucket");

        // Upload all files to s3 bucket
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

        // Add flag for s3 bucket and restart all services
        AddS3FlagAndRestartLennyServices();
        // Sleep for 1 minute - so ds and ds-mgmt will start
        JLog.logger.info("Waiting 1 minute for ds and ds-mgmt to start...");
        Thread.sleep(60000);

        JLog.logger.info("Installing windows EP...");
        // TODO RBRB make order of EPs irrelevant

        endpointWin.InstallEPIncludingRequisites(AgentActions.EP_OS.WINDOWS, Integer.parseInt(general.get("EP " +
                "Installation timeout")), Integer.parseInt(general.get("EP Service Timeout")),
                Integer.parseInt(general.get("From EP service start until logs show EP active timeout")));

        JLog.logger.info("Installing linux EP...");

        endpointLinux.InstallEPIncludingRequisites(AgentActions.EP_OS.LINUX, Integer.parseInt(general.get("EP " +
                "Installation timeout")), Integer.parseInt(general.get("EP Service Timeout")),
                Integer.parseInt(general.get("From EP service start until logs show EP active timeout")));

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

        // Verify that the EPs have the correct new versions
        String linuxBinVer = endpointLinux.getEPBinaryVersion(AgentActions.EP_OS.LINUX);
        String winBinVer = endpointWin.getEPBinaryVersion(AgentActions.EP_OS.WINDOWS);

        JLog.logger.info("Linux version is: " + linuxBinVer);
        JLog.logger.info("Windows version is: " + winBinVer);

        org.testng.Assert.assertEquals(linuxBinVer, linuxNewVer, "Versions don't match");
        org.testng.Assert.assertEquals(winBinVer, windowsNewVer, "Versions don't match");

        // Clean the bucket
        JLog.logger.info("Cleaning the bucket now...");
        for (String file : windowsFiles) {
            response = connection.Execute("export AWS_ACCESS_KEY_ID=`grep aws-access-key-id " + "/work" +
                    "/services/stub-srv/etc/nep-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " +
                    "AWS_SECRET_ACCESS_KEY=`grep aws-secret-access-key " + "/work/services/stub-srv/etc/nep" +
                    "-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " + "AWS_REGION=`grep aws" +
                    ".updates.bucket-region /work/services/stub-srv/etc/nep-properties/nepa-dserver" + ".properties |" +
                    " cut -d '=' -f 2` ; cd /home/; s3_cmd delete_file " + s3Bucket + " binaries" +
                    "/windows/" + file);
            JLog.logger.info("Response: " + response);
        }
        for (String file : linuxFiles) {
            response = connection.Execute("export AWS_ACCESS_KEY_ID=`grep aws-access-key-id " + "/work" +
                    "/services/stub-srv/etc/nep-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " +
                    "AWS_SECRET_ACCESS_KEY=`grep aws-secret-access-key " + "/work/services/stub-srv/etc/nep" +
                    "-properties/nepa-dserver.properties | cut -d '=' -f 2` ; export " + "AWS_REGION=`grep aws" +
                    ".updates.bucket-region /work/services/stub-srv/etc/nep-properties/nepa-dserver" + ".properties |" +
                    " cut -d '=' -f 2` ; cd /home/; s3_cmd delete_file " + s3Bucket + " binaries" +
                    "/linux/" + file);
            JLog.logger.info("Response: " + response);
        }

        JLog.logger.info("Done...");


    }

    private void FetchFiles(String nexusPath, List<String> files, String localFolder) throws IOException {
        String masterDownloadDirectory = PropertiesFile.getManagerDownloadFolder();

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

    private void AddS3FlagAndRestartLennyServices() {
        JLog.logger.info("Adding s3 flag to LNE machine");
        String response = connection.Execute("touch /work/flag-s3-lifetime");
        JLog.logger.info("Response: " + response);

        response = connection.Execute("/work/tools/bin/s3-blocker /work/flag-s3-lifetime 12");
        JLog.logger.info("Response: " + response);

        // restart all services
        JLog.logger.info("Restarting all services on LNE machine");
        response = connection.Execute("nep_service all restart");
        JLog.logger.info("Response: " + response);

    }

    @AfterMethod
    public void Close() {
        if (endpointWin != null) {
            JLog.logger.info("Closing windows EP");
            endpointWin.Close();
        }
        if (endpointLinux != null) {
            JLog.logger.info("Closing linux EP");
            endpointLinux.Close();
        }
    }

}
