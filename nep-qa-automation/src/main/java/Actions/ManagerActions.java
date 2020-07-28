package Actions;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.TestFiles;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class ManagerActions {

    public static final String windowsInstallationFile = "TrustwaveEndpoint.exe";
    public static final String archiveFolderName = "archive";
    private static final String uninstallFolderName = "uninstall";

    public static final int checkInterval = 5000;

    public static String execCmd(String cmd, boolean runAsAdmin) throws java.io.IOException {
        final String elevatePath = "C:\\Selenium\\Utils\\Elevate.exe";

        if (runAsAdmin) {
            File file = new File(elevatePath);
            if( ! file.exists())
                org.testng.Assert.fail("Could not run commands as administrator. Please copy missing file: " + elevatePath);
        }

        if (runAsAdmin)
            cmd = elevatePath + " " + cmd;

        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }


    public void VerifyInstallerSignature() {
        try {
            if (! SystemUtils.IS_OS_WINDOWS) {
                JLog.logger.warn("Signature of exe file is currently not checked under linux manager");
                return;
            }

            final String expectedVerified1 = "Verified:\tA certificate chain processed, but terminated in a root certificate which is not trusted by the trust provider";
            final String expectedVerified2 = "Verified:\tSigned ";

            String sigcheckPath = "c:\\Selenium\\Utils\\sigcheck.exe";

            File file = new File(sigcheckPath);
            if (!file.exists())
                org.testng.Assert.fail("Signature check failed. Could not find signature check utility at: " + sigcheckPath);

            String command = sigcheckPath + " -nobanner -a ";
            String installerLocation = PropertiesFile.getManagerDownloadFolder();
            installerLocation += "\\" + windowsInstallationFile;
            command += installerLocation;
            String result = execCmd(command, false);
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
            //String [] expected = {"client.pem" , "client_key.pem" , "TrustwaveEndpoint.exe"};
            String[] expected = {archiveFolderName, windowsInstallationFile, uninstallFolderName};

            boolean foundFiles = false;
            String[] filesArr = nepFolder.list();

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeoutSeconds);
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                filesArr = nepFolder.list();
                if (Arrays.equals(nepFolder.list(), expected)) {
                    foundFiles = true;
                    break;
                }
            }
            if (foundFiles == false)
                org.testng.Assert.fail("Could not find expected installation files at folder: " + nepFolder + "\n" + "Found files:   " + Arrays.toString(filesArr)
                        + "\n" + "Expected files:" + Arrays.toString(expected));

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

            if ( SystemUtils.IS_OS_WINDOWS) {
                TestFiles.CreateFolder("C:/home");
            }
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