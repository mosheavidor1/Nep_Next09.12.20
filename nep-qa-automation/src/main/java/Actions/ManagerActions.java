package Actions;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.TestFiles;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class ManagerActions {

    public static final String windowsInstallationFile = "TrustwaveEndpoint.exe";
    public static final String linuxInstallationFile = "TrustwaveEndpoint.lnx";
    public static final String linuxSha256InstallationFile = "TrustwaveEndpoint.lnx.sha256";
    public static final String archiveFolderName = "archive";
    public static final String utilsFolderName = "utils";
    private static final String uninstallFolderName = "uninstall";
    private static final String  sigcheckFile = "sigcheck.exe";
    private static final String  utilsFolderLocation = "C:/home/NepManagerDownloads/Utils/";
    public static final String customerDbFile = "customer_db_file.json";
    
    public static final int checkInterval = 5000;



    //This method execute commands on the manager locally. This is needed for sigcheck operation
    //For running commands on the agent use AgentActions -> connection.Execute
    public static String execCmd(String cmd) throws java.io.IOException {

        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public void VerifyLinuxInstallerHash() {
        try {
            if (! SystemUtils.IS_OS_WINDOWS) {
                JLog.logger.warn("Signature of the linux installer is currently not checked under linux manager");
                return;
            }

            JLog.logger.info("Verifying linux installer...");

            String pathToLinuxInstaller = PropertiesFile.getManagerDownloadFolder()+ "/" + linuxInstallationFile;
            String pathToSHA256File = PropertiesFile.getManagerDownloadFolder()+ "/" + linuxSha256InstallationFile;
            String fileContent = FileUtils.readFileToString(new File(pathToSHA256File), StandardCharsets.UTF_8).replaceAll("\n", "");
            JLog.logger.debug("Hash is: " + fileContent);

            String command = "certutil -hashfile ";
            command += pathToLinuxInstaller;
            command += " SHA256";

            String result = execCmd(command);
            result = result.replaceAll("\\s+",""); //some operating systems include spaces

            if (!(result.contains(fileContent))) {
                org.testng.Assert.fail("Failed to verify hash of file: " + pathToLinuxInstaller + "\nCertutil output:\n" + result
                        + "\nExpected check hash result should have included:\n" + fileContent);
            }

            JLog.logger.info("Linux installer hash verified successfully.");

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not verify linux installer hash." + "\n" + e.toString(), e);
        }
    }

    public void VerifyInstallerSignature() {
        try {
            if (! SystemUtils.IS_OS_WINDOWS) {
                JLog.logger.warn("Signature of exe file is currently not checked under linux manager");
                return;
            }

            final String expectedVerified1 = "Verified:\tA certificate chain processed, but terminated in a root certificate which is not trusted by the trust provider";
            final String expectedVerified2 = "Verified:\tSigned ";

            String sigcheckPath = utilsFolderLocation + sigcheckFile;

            File file = new File(sigcheckPath);
            if (!file.exists())
                org.testng.Assert.fail("Signature check failed. Could not find signature check utility at: " + sigcheckPath);

            String command = sigcheckPath + " -nobanner -a ";
            String installerLocation = PropertiesFile.getManagerDownloadFolder();
            installerLocation += "/" + windowsInstallationFile;
            command += installerLocation;
            String result = execCmd(command);
            if (!(result.contains(expectedVerified1) || result.contains(expectedVerified2)))
                org.testng.Assert.fail("Failed to verify siganture of file: " + installerLocation + "\nCheck Signature output:\n" + result
                        + "\nExpected check signature result could be one of the following:\n" + expectedVerified1 + "\nOr:\n" + expectedVerified2);

            int startLine = result.indexOf("Binary Version");
            int endLine = result.indexOf("\n", startLine);
            String version = result.substring(startLine, endLine);
            JLog.logger.info("End Point Agent " + version);

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not verify installer signature." + "\n" + e.toString());
        }

    }


    public void VerifyFilesExist(int timeoutSeconds) {
        try {

            File nepFolder = new File(PropertiesFile.getManagerDownloadFolder());
            String[] expectedArr = {archiveFolderName, windowsInstallationFile, uninstallFolderName,utilsFolderName};
            List expectedList = Arrays.asList(expectedArr);

            boolean foundFiles = false;
            String[] filesArr = nepFolder.list();

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeoutSeconds);
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                filesArr = nepFolder.list();
                if (Arrays.asList(filesArr).containsAll(expectedList)) {
                    foundFiles = true;
                    break;
                }
            }
            if (foundFiles == false)
                org.testng.Assert.fail("Could not find expected installation files at folder: " + nepFolder + "\n" + "Found files:   " + Arrays.toString(filesArr)
                        + "\n" + "Expected files:" + Arrays.toString(expectedArr));

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not verify installation files exist." + "\n" + e.toString());
        }
    }


    public void CreateAndCleanDownloadFolder() {
        try {
            String managerDownloadFolder = PropertiesFile.getManagerDownloadFolder();
            String oldLocation = managerDownloadFolder + "/" + archiveFolderName;
            String installerLocation = managerDownloadFolder + "/" + windowsInstallationFile;
            String uninstallFolder = managerDownloadFolder + "/" + uninstallFolderName;
            String installerAtFolderUninstall = uninstallFolder + "/" + windowsInstallationFile;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-YY--HH-mm-ss");

            String oldInstallerLocation = oldLocation + "/" + windowsInstallationFile.substring(0, windowsInstallationFile.indexOf(".")) + LocalDateTime.now().format(formatter) + windowsInstallationFile.substring(windowsInstallationFile.indexOf("."));

            TestFiles.CreateFolder(managerDownloadFolder);
            TestFiles.CreateFolder(oldLocation);
            TestFiles.CreateFolder(uninstallFolder);

            if (TestFiles.Exists(installerLocation)) {
                TestFiles.Copy(installerLocation, oldInstallerLocation);
                if (!TestFiles.Exists(installerAtFolderUninstall))
                    TestFiles.Copy(installerLocation, installerAtFolderUninstall);

                //TestFiles.DeleteFile(installerLocation);
            }
            TestFiles.DeleteAllFiles(managerDownloadFolder);

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not create test download folder and clean it" + "\n" + e.toString());

        }
    }


}
