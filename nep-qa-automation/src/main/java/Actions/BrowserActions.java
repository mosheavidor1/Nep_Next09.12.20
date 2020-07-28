package Actions;

import Applications.Application;
import Applications.SeleniumBrowser;
import Pages.Portal.*;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.TestFiles;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;

public class BrowserActions extends ManagerActions {

    private Application application;

    private static final String PortalManagmentURL = "/#/operations?menuKey=cua-search&stackKey=search-home";
    private static final String FileCabinetURL = "/#/support?menuKey=file-cabinet&stackKey=file-cabinet-files";
    private static final String centComSearchURL = "/#/operations?menuKey=centcom-devices&stackKey=entity-search&types=EntityType.AGENT";
    private static final String eventExplorerURL = "/#/dataexplorer?menuKey=log-management-search&stackKey=log-event-search";

    private static final String uninstallFolderName = "uninstall";

    private static final String dbJsonPath = "C:\\ProgramData\\Trustwave\\NEPAgent\\db.json";
    public static final String configJsonPath = "C:\\ProgramData\\Trustwave\\NEPAgent\\config.json";
    private static final String configJsonReportInterval = "\"report_period\":";

    private static final int ServiceStartStopTimeout = 180;



    public BrowserActions() {
        application = SeleniumBrowser.GetInstance();
    }

    public void CloseApplication() {
        application.Close();
    }

    public void LaunchApplication(String ApplicationType) throws IOException {
        application.Launch(ApplicationType);
    }


    public void SetApplicationUrl(String Url)
    {
        application.LoadUrl(Url);
    }

    public void GotoCentComSearch(String UrlPrefix) {

        this.SetApplicationUrl(UrlPrefix + centComSearchURL);
    }

    public void GotoEventExplorer(String UrlPrefix) {

        this.SetApplicationUrl(UrlPrefix + eventExplorerURL);
    }


    public void GotoPortalManagmentPage(String UrlPrefix) {

        this.SetApplicationUrl(UrlPrefix + PortalManagmentURL);
    }

    public void GotoPortalFileCabinetPage(String UrlPrefix) {

        this.SetApplicationUrl(UrlPrefix + FileCabinetURL);
    }

    public void Login(String userName, String password) {
        try {
            LoginPage login = new LoginPage();
            DashboardPage dash = new DashboardPage();

            login.WaitUntilTitleAppearAndPageLoad("Trustwave Fusion");
            login.UserName.SetText(userName);
            login.Password.SetText(password);
            login.LoginButton.click();

            //wait until page load
            dash.WaitUntilObjectClickable(DashboardPage.dashboradByID,120);
        }
        catch (Exception e){

        }

    }



    //verify log entry message appears at the portal
    public void VerifyMessageExistsInPortal(LogEntry entry, int timeoutForLogEntryToAppearInSeconds) {
        try {
            EventExplorerPage eventPage = new EventExplorerPage();
            final String filteredItemsText = "Filtered Items";

            boolean found = false;

            eventPage.timeRangeBox.click();
            eventPage.WaitUntilObjectClickable(EventExplorerPage.last24HoursBy);
            eventPage.WaitUntilPageLoad();
            eventPage.WaitUntilObjectClickable(EventExplorerPage.last24HoursBy);
            eventPage.last24Hours.click();
            eventPage.applyTimeButton.click();

            eventPage.addQuery.click();

            eventPage.WaitUntilObjectClickable(eventPage.detectorHostQueryBy);
            eventPage.WaitUntilPageLoad();
            eventPage.WaitUntilObjectClickable(eventPage.detectorHostQueryBy);

            eventPage.detectorHostQuery_element.click();
            eventPage.detectorHostAll.click();

            String fullMachineName = InetAddress.getLocalHost().getCanonicalHostName();
            eventPage.specifyAnOption.sendKeys(fullMachineName + "\n");

            eventPage.selectQueryOption.click();
            eventPage.searchButton.click();

            eventPage.WaitUntilObjectDisappear(eventPage.spinnerBy);


            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeoutForLogEntryToAppearInSeconds);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                eventPage.searchBox_element.clear();

                eventPage.refreshButton_element.click();
                eventPage.searchBox_element.sendKeys(entry.stampAdded + "\n");

                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

                String filteredString = eventPage.filteredItems_element.getText();

                if (!filteredString.contains(filteredItemsText))
                    continue;
                filteredString = filteredString.substring(filteredString.indexOf(filteredItemsText) + filteredItemsText.length()).trim();

                int filteredItems = 0;
                filteredItems = Integer.parseInt(filteredString.trim());

                if (filteredItems == 1) {
                    found = true;
                    break;
                }
                if (filteredItems > 1)
                    org.testng.Assert.fail("Found too many results for the unique time stamp: " + entry.stampAdded + " See screenshot/video links below");

                eventPage.timeRangeBox.click();
                eventPage.WaitUntilObjectClickable(EventExplorerPage.last24HoursBy);
                eventPage.WaitUntilPageLoad();
                eventPage.WaitUntilObjectClickable(EventExplorerPage.last24HoursBy);
                eventPage.last24Hours.click();
                eventPage.applyTimeButton.click();
                eventPage.WaitUntilObjectDisappear(eventPage.spinnerBy);


            }

            if (!found)
                org.testng.Assert.fail("Relevant log entry do not appear at portal after timeout: " + timeoutForLogEntryToAppearInSeconds + " seconds. See screenshot or video links below.\nExpected stamp: " + entry.stampAdded);

            eventPage.openRowButton_element.click();

            String logEntryFoundAtPortal = eventPage.entryMessage_element.getText();
            if (!logEntryFoundAtPortal.contains(entry.stampAdded))
                org.testng.Assert.fail("Event entry description found at the portal do not contain expected stamp: " + entry.stampAdded);
            final String eventIdLocator = "EventID:";
            int startOfEventID = logEntryFoundAtPortal.indexOf(eventIdLocator) + eventIdLocator.length();
            int end = logEntryFoundAtPortal.indexOf("|", startOfEventID);
            String foundID = logEntryFoundAtPortal.substring(startOfEventID, end);
            if (foundID.compareToIgnoreCase(entry.eventID) != 0)
                org.testng.Assert.fail("Event written to log: " + entry.eventID + " Is not matched to the event ID found at portal: " + foundID);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not verify log entry appear at portal. " + "\n" + e.toString());
        }

    }

    public void PublishNewDownloads(String customerName, int waitForPublishInSec) {
        try {

            SelectCustomerAtCentComSearchPage(customerName);

            CentComSearchPage centSearch = new CentComSearchPage();
            centSearch.detailsButton_element.click();

            CentComSearchDetailsPage detailsPage = new CentComSearchDetailsPage();
            detailsPage.resetInstaller_element.click();
            detailsPage.continueButton_element.click();
            detailsPage.openConfiguration_element.click();

            PublishConfiguration(waitForPublishInSec);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not publish new downloads for customer: " + customerName + "\n" + e.toString());
        }

    }


    public void ChangeConfigurationAndPublish(String customerName, int waitForPublishInSec) {
        try {
            SelectCustomerAtCentComSearchPage(customerName);
            CentComSearchPage centSearch = new CentComSearchPage();
            centSearch.openConfigurationButton_element.click();

            //Put here code for changing configuration

            PublishConfiguration(waitForPublishInSec);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not change configuration and publish for customer: " + customerName + "\n" + e.toString());
        }

    }


    public void SelectCustomerAtCentComSearchPage(String customerName) {
        try {
            CentComSearchPage centSearch = new CentComSearchPage();
            centSearch.customersText_element.sendKeys(customerName);
            centSearch.searchButton_element.click();
            centSearch.GetCustomerRow(customerName).click();
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not select customer at CentCom search page. customer: " + customerName + "\n" + e.toString());
        }

    }


    public void PublishConfiguration(int waitForPublishInSec) {
        try {

            CentComConfigurationPage conf = new CentComConfigurationPage();
            conf.WaitUntilObjectDisappear(conf.spinnerBy);
            conf.WaitUntilObjectClickable(conf.publishBy);
            conf.publishButton_element.click();
            conf.continueButton_element.click();
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(waitForPublishInSec);
            boolean found = false;

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(5000);
                current = LocalDateTime.now();

                if(conf.IsElementExist(conf.refreshBy)) {
                    try {
                        conf.refreshButton_element.click();
                        JLog.logger.info("Refresh button of publish configuration clicked successfully: "+ conf.refreshBy.toString() );
                    }
                    catch (Exception e) {
                        JLog.logger.warn("Could not click the refresh button of publish configuration: " + e.toString() +"\nRefresh button:"+ conf.refreshBy.toString() );
                    }
                }

                if (conf.IsElementExist(conf.percent100By)) {
                    found = true;
                    break;
                }
            }

            if (!found)
                org.testng.Assert.fail("Publish configuration not completed successfully. Could not find publish completed \"100%\" indication after timeout of: " + waitForPublishInSec + "(sec) See screenshot/video links below");
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not publish configuration" + "\n" + e.toString());
        }

    }


    public void CheckEndPointOkInCentCom(String customerName) {
        try {
            CentComSearchPage centSearch = new CentComSearchPage();
            centSearch.customersText_element.sendKeys(customerName);
            centSearch.searchButton_element.click();
            centSearch.GetCustomerRow(customerName).click();
            centSearch.detailsButton_element.click();

            CentComSearchDetailsPage detailsPage = new CentComSearchDetailsPage();
            detailsPage.endPointTab_element.click();

            String host = InetAddress.getLocalHost().getHostName();

            //detailsPage.endPointSearchBox_element.sendKeys(host + "\n");
            detailsPage.binocularsButton_element.click();

            detailsPage.WaitUntilPageLoad();
            detailsPage.WaitUntilObjectClickable(detailsPage.valueToSearchBy);
            detailsPage.valueToSearch_element.sendKeys(host + "\n");

            detailsPage.refreshButton_element.click();

            detailsPage.WaitUntilPageLoad();
            detailsPage.WaitUntilObjectDisappear(detailsPage.spinnerBy);
            detailsPage.WaitUntilObjectClickable(detailsPage.rowBy);

            Thread.sleep(5000); //after all 3 wait above needs some more - to be investigated

            if (!detailsPage.IsElementExist(detailsPage.GetHostNameRowBy(host))) {
                org.testng.Assert.fail("Could not find hostname: " + host);
            }

            if (!detailsPage.IsElementExist(detailsPage.OkBy)) {
                org.testng.Assert.fail("Host: " + host + " Status is not Okay. See screenshot/video.");
            }
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not check endpoint status at CentCom for customer: " + customerName + "\n" + e.toString());
        }

    }


    public void SelectCustomer(String customerName) {
        try {
            UpperMenu up = new UpperMenu();

            up.WaitUntilObjectDisappear(up.spinnerBy);
            up.WaitUntilPageLoad();
            up.WaitUntilObjectDisappear(up.spinnerBy);
            up.WaitUntilPageLoad();

            up.WaitUntilObjectClickable(up.customerSelectorBy);
            up.customerSelector_element.click();

            up.WaitUntilObjectClickable(up.searchTextBy);
            up.searchText_element.clear();
            up.searchText_element.sendKeys(customerName);

            up.WaitUntilObjectClickable(up.customerNameBy);
            up.customerName.click();
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not select customer at portal. Customer: " + customerName + "\n" + e.toString());
        }

    }

    public void DeleteAllDownloads() {
        try {
            FileCabinet fc = new FileCabinet();
            fc.TrustwaveEndpointFolder_element.click();

            fc.WaitUntilObjectDisappear(fc.spinnerBy);
            fc.WaitUntilObjectClickable(fc.refreshButtonBy);
            fc.WaitUntilPageLoad();


            while (fc.GetFirstThreeDotsIcon().size() > 0) {
                fc.WaitUntilObjectClickable(fc.threeDotsIconBy);
                fc.WaitUntilPageLoad();
                fc.threeDotsIcon_element.click();
                fc.WaitUntilObjectClickable(fc.removeMenuItemBy);
                fc.WaitUntilPageLoad();
                fc.WaitUntilObjectDisappear(fc.spinnerBy);
                fc.WaitUntilObjectClickable(fc.removeMenuItemBy);
                fc.removeMenuItem_element.click();
                fc.WaitUntilObjectClickable(fc.removeButtonConfirmBy);
                fc.removeButtonConfirm_element.click();
            }
        } catch (Exception e) {
            org.testng.Assert.fail("Could not delete all endpoint downloads at portal." + "\n" + e.toString());
        }


    }

    public void DownloadFilesFromTrustWaveEndPointFolder(String fileToAppearTimeoutString, String fileStoredAndVirusScanTimeoutString) {
        try {

            int fileToAppearTimeout = Integer.parseInt(fileToAppearTimeoutString);
            int fileStoredAndVirusScanTimeout = Integer.parseInt(fileStoredAndVirusScanTimeoutString);

            FileCabinet fc = new FileCabinet();

            fc.TrustwaveEndpointFolder_element.click();

            boolean found = false;


            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(fileToAppearTimeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                fc.WaitUntilObjectClickable(fc.refreshButtonBy);
                fc.WaitUntilObjectDisappear(fc.spinnerBy);
                fc.WaitUntilObjectClickable(fc.refreshButtonBy);

                fc.refreshButton_element.click();

                Thread.sleep(checkInterval);
                current = LocalDateTime.now();


                if (fc.IsElementExist(FileCabinet.endPointExeBy)) {
                    found = true;
                    break;
                }

            }

            if (!found)
                org.testng.Assert.fail("Download failed. Installation file did not appeared at File Cabinet after timeout: " + fileToAppearTimeoutString + " seconds. See screenshot or video links below");


            boolean errorMessageAppear = true;


            start = LocalDateTime.now();
            current = start;
            durationTimeout = Duration.ofSeconds(fileStoredAndVirusScanTimeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {

                fc.WaitUntilObjectClickable(fc.endPointExeBy);
                fc.TrustwaveEndpointExe_element.click();
                if (!fc.IsElementExist(fc.fileUnableToBeDownloadedBy)) {
                    errorMessageAppear = false;
                    break;
                }

                fc.errorMessageOKButton_element.click();

                fc.refreshButton_element.click();

                Thread.sleep(checkInterval);
                current = LocalDateTime.now();


                fc.WaitUntilObjectClickable(fc.refreshButtonBy);
                fc.WaitUntilObjectDisappear(fc.spinnerBy);
                fc.WaitUntilObjectClickable(fc.refreshButtonBy);


            }

            if (errorMessageAppear)
                org.testng.Assert.fail("Message appears: File is still being processed (virus scanned and stored). after timeout: " + fileStoredAndVirusScanTimeoutString + " seconds. See video link below");

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not download endpoint installation files" + "\n" + e.toString());
        }

    }



    public void InstallEndPoint(int timeout) {
        try {
            JLog.logger.info("Installing EP...");
            String installerLocation = PropertiesFile.getManagerDownloadFolder();
            installerLocation += "\\" + windowsInstallationFile;
            //String host = "DS_HOST_NAME=" + PropertiesFile.getCurrentClusterNepHost() ;
            String command = installerLocation + " /q ";//+ host;
            String result = ManagerActions.execCmd(command, false);
            boolean found = false;

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

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

            String MasterDownloadFolder = PropertiesFile.getManagerDownloadFolder();
            String installerLocation = MasterDownloadFolder + "\\" + uninstallFolderName + "\\" + windowsInstallationFile;
            boolean toDeleteInstaller = true;

            if (!TestFiles.Exists(installerLocation)) {
                installerLocation = MasterDownloadFolder + "\\" + windowsInstallationFile;
                toDeleteInstaller = false;
            }
            String command = installerLocation + " /q /uninstall";

            //wmic is not working because EP bootstrap has missing data
            //execCmd("wmic product where \"description='Trustwave Endpoint Agent (64bit)' \" uninstall",true);
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            ManagerActions.execCmd(command, false);
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


            final String installationFolder = "C:\\Program Files\\Trustwave\\NEPAgent";
            File installationFolderFile = new File(installationFolder);
            found = true;

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                if (!installationFolderFile.exists()) {
                    found = false;
                    break;
                }
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                //JLog.logger.debug("Found Folder!!!");

            }

            if (found) {
                TestFiles.DeleteDirectory(installationFolder);
                durationTimeout = durationTimeout.plusMinutes(1);
            }

            //org.testng.Assert.fail("Uninstall failed. Trustwave Endpoint installation folder not deleted  after timeout(sec): " + Integer.toString(timeout) + "   Installation folder: " + installationFolder);

            found = true;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {

                String result = ManagerActions.execCmd("tasklist", false);
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

            if (toDeleteInstaller)
                TestFiles.DeleteFile(installerLocation);

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not uninstall end point" + "\n" + e.toString());
        }

    }


    public void StopEPService(int timeout) {
        try {

            ManagerActions.execCmd("Net stop NepaService", false);

            boolean active = true;
            String result = "";

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                result = ManagerActions.execCmd("sc query \"NepaService\"", false);
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
            ManagerActions.execCmd("Net start NepaService", false);

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            boolean active = false;

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

                String result = ManagerActions.execCmd("sc query \"NepaService\"", false);
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

    public void ChangeReportInterval(String interval) {
        try {
            StopEPService(ServiceStartStopTimeout);
            File file = new File(configJsonPath);
            if (!file.exists())
                org.testng.Assert.fail("Could not find config.json file was not found at: " + dbJsonPath);

            FileInputStream inputStream = new FileInputStream(configJsonPath);
            String text = IOUtils.toString(inputStream, Charset.defaultCharset());
            inputStream.close();

            if (!text.contains(configJsonReportInterval)) {
                StartEPService(ServiceStartStopTimeout);
                org.testng.Assert.fail("Endpoint did not received expected configuration. Could not change the logs interval as " + configJsonReportInterval + " could not be found at: " + configJsonPath);
            }

            int start = text.indexOf(configJsonReportInterval) + configJsonReportInterval.length();
            int end = text.indexOf(",", start);
            StringBuilder builder = new StringBuilder(text);
            builder.replace(start, end, interval);

            PrintWriter out = new PrintWriter(configJsonPath);
            out.print(builder.toString());
            out.close();

            //verifying the value is written correctly
            inputStream = new FileInputStream(configJsonPath);
            text = IOUtils.toString(inputStream, Charset.defaultCharset());
            inputStream.close();
            end = text.indexOf(",", start);
            String foundInFile = text.substring(start, end);
            if (foundInFile.compareTo(interval) != 0)
                org.testng.Assert.fail("Could not change value of report interval at at the file: " + configJsonPath);

            StartEPService(ServiceStartStopTimeout);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not change endpoint report interval." + "\n" + e.toString());
        }


    }

    //Example "EventCreate /t INFORMATION /id 123 /l APPLICATION /so AutomationTest /d \"Hello!! this is the test info\""
    public void WriteEvent(LogEntry entry) {
        try {
            if (entry.addedTimeToDescription)
                entry.AddTimeToDescription(LocalDateTime.now().toString());
            entry.eventDescription = "\"" + entry.eventDescription + "\"";
            String eventCommand = "EventCreate /t " + entry.eventType + " /id " + entry.eventID + " /l " + entry.eventLog + " /so " + entry.eventSource + " /d " + entry.eventDescription;
            String result = ManagerActions.execCmd(eventCommand, false);
            if (!result.contains("SUCCESS: An event of type"))
                org.testng.Assert.fail("Could no add log event.\nAdd event result: " + result + "\nCommand sent: " + eventCommand);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not write event to windows log." + "\n" + e.toString());
        }

    }

    public void CheckEndPointActiveByDbJson(int timeout) {
        try {

            String text = "";
            boolean active = false;
            File file = new File(dbJsonPath);

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                if (file.exists()) {
                    FileInputStream inputStream = new FileInputStream(dbJsonPath);
                    text = IOUtils.toString(inputStream, Charset.defaultCharset());
                    inputStream.close();
                    if (text.contains("\"EndpointId\": \"") && text.contains("\"DsInitialHost\": ")) {
                        active = true;
                        break;
                    }
                }

                Thread.sleep(checkInterval);
                current = LocalDateTime.now();

            }

            if (!file.exists())
                org.testng.Assert.fail("Endpoint is not connected - db.json file was not found at: " + dbJsonPath + " after timeout(sec): " + timeout);

            if (!active)
                org.testng.Assert.fail("Endpoint is not connected according to db.json file after timeout(sec): " + timeout + ". Failed to find in db.json: End Point ID  Or Host.\n" + "db.json file content:\n" + text);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not check if endpoint is active by db.json file." + "\n" + e.toString());
        }


    }


    public boolean EndPointServiceExist() {
        try {
            //String result = execCmd("net start | find \"Trustwave Endpoint Agent Service\"");
            String result = ManagerActions.execCmd("net start", false);
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


}

