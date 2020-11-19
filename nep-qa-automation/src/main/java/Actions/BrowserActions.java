package Actions;

import Applications.Application;
import Applications.SeleniumBrowser;
import Pages.Portal.*;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import Utils.TestFiles;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

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

    public void LaunchApplication(String ApplicationType) {
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
            login.Continue.click();
            login.Password.SetText(password);
            JLog.logger.debug("Waiting for login button to be clickable...");
            login.WaitUntilPageLoad();
            login.WaitUntilObjectClickable(login.loginButtonBy,60);
            login.LoginButton.click();
            JLog.logger.debug("Login button clicked successfully...");

            //wait until page load
            dash.WaitUntilObjectClickable(DashboardPage.dashboradByID,120);
        }
        catch (Exception e){
        	JLog.logger.error("Login exception.", e);
        }

    }



    //verify log entry message appears at the portal
    public void VerifyMessageExistsInPortal(LogEntry entry, String hostname, int timeoutForLogEntryToAppearInSeconds) {
        try {
            JLog.logger.debug("Starting VerifyMessageExistsInPortal with timeout: " +timeoutForLogEntryToAppearInSeconds + " seconds");
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

            //String fullMachineName = InetAddress.getLocalHost().getCanonicalHostName();
            String fullMachineName = hostname;
            eventPage.specifyAnOption.sendKeys(fullMachineName + "\n");

            eventPage.selectQueryOption.click();
            eventPage.searchButton.click();

            eventPage.WaitUntilObjectDisappear(eventPage.spinnerBy);


            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeoutForLogEntryToAppearInSeconds);

            JLog.logger.info("Waiting for event to appear at event explorer for: "+ timeoutForLogEntryToAppearInSeconds + " seconds");
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
        	JLog.logger.error("Could not verify log entry appear at portal.", e);
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

            //close alert button popup
            //Temporarily waiting for alert box to disappear until fixing this issue better
            Thread.sleep(10*1000);
            UpperMenu upper = new UpperMenu();
            if(upper.IsElementExist(upper.alretBoxCloseBy)){
                upper.alretBoxCloseButton.click();
            }


            detailsPage.openConfiguration_element.click();

            PublishConfiguration(waitForPublishInSec);
        }
        catch (Exception e) {
        	JLog.logger.error("Could not publish new downloads for customer: {}",customerName, e);
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
        	JLog.logger.error("Could not change configuration and publish for customer: {}",customerName, e);
            org.testng.Assert.fail("Could not change configuration and publish for customer: " + customerName + "\n" + e.toString());
        }

    }


    public void SelectCustomerAtCentComSearchPage(String customerName) {
        try {
            CentComSearchPage centSearch = new CentComSearchPage();
            centSearch.customersText_element.sendKeys(customerName);
            centSearch.searchButton_element.click();
            centSearch.WaitUntilPageLoad();
            centSearch.WaitUntilObjectDisappear(centSearch.spinnerBy);
            centSearch.WaitUntilObjectClickable(centSearch.GetCustomerRowBy(customerName));
            centSearch.WaitUntilPageLoad();
            centSearch.WaitUntilObjectDisappear(centSearch.spinnerBy);
            centSearch.WaitUntilObjectClickable(centSearch.GetCustomerRowBy(customerName));

            centSearch.GetCustomerRow(customerName).click();
        }
        catch (Exception e) {
        	JLog.logger.error("Could not select customer at CentCom search page. customer: {}",customerName, e);
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
        	JLog.logger.error("Could not publish configuration", e);
            org.testng.Assert.fail("Could not publish configuration" + "\n" + e.toString());
        }

    }


    public void GotoCentComEndpointsPage(String customerName) {
        try {
            CentComSearchPage centSearch = new CentComSearchPage();
            centSearch.customersText_element.sendKeys(customerName);
            centSearch.searchButton_element.click();
            centSearch.GetCustomerRow(customerName).click();
            centSearch.detailsButton_element.click();

            CentComSearchDetailsPage detailsPage = new CentComSearchDetailsPage();
            detailsPage.endPointTab_element.click();

        }
        catch (Exception e) {
        	JLog.logger.error("Could not check endpoint status at CentCom for customer: {}", customerName, e);
            org.testng.Assert.fail("Could not check endpoint status at CentCom for customer: " + customerName +  "\n" + e.toString());
        }

    }

    public void CheckEndPointOkInCentCom(String hostname,int timeout) {
        try {
            CentComSearchDetailsPage detailsPage = new CentComSearchDetailsPage();

            detailsPage.binocularsButton_element.click();

            detailsPage.WaitUntilPageLoad();
            detailsPage.WaitUntilObjectClickable(detailsPage.valueToSearchBy);
            detailsPage.valueToSearch_element.clear();
            detailsPage.valueToSearch_element.sendKeys(hostname + "\n");

            By epLine = detailsPage.GetHostNameRowBy(hostname);

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            boolean found = false;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                detailsPage.refreshButton_element.click();
                detailsPage.WaitUntilPageLoad();
                detailsPage.WaitUntilObjectDisappear(detailsPage.spinnerBy);

                Thread.sleep(checkInterval);
                current = LocalDateTime.now();
                if (detailsPage.IsElementExist(epLine)) {
                    found = true;
                    break;
                }
                JLog.logger.debug("Waiting for endpoint to appear OK at portal. EP name: "+ hostname);
            }

            if (!found) {
                org.testng.Assert.fail("Could not find endpoint name: " + hostname +" at CentCom after timeout(sec): " + timeout + " . See screenshot attached");
            }

            if (!detailsPage.IsElementExist(detailsPage.OkBy)) {
                org.testng.Assert.fail("Host: " + hostname + " Status is not Okay at CentCom. See screenshot.");
            }

            JLog.logger.info("Status OK found at CentCom for endpoint: " + hostname);

        }
        catch (Exception e) {
        	JLog.logger.error("Could not check endpoint status at CentCom for hostname: {}", hostname, e);
            org.testng.Assert.fail("Could not check endpoint status at CentCom for hostname: " + hostname +  "\n" + e.toString());
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

            JLog.logger.debug("customerSelector_element clicked");

            up.WaitUntilObjectDisappear(up.spinnerBy);
            up.WaitUntilPageLoad();
            up.WaitUntilObjectClickable(up.searchTextBy);
            up.searchText_element.clear();
            JLog.logger.debug("searchText_element cleared");

            up.WaitUntilObjectDisappear(up.spinnerBy);
            up.WaitUntilPageLoad();
            up.WaitUntilObjectClickable(up.searchTextBy);

            up.searchText_element.sendKeys(customerName);
            JLog.logger.debug("searchText_element keys sent: " + customerName);

            Thread.sleep(2000); // As there are failures with this click - after several tries got to rock bottom using sleep.
            up.WaitUntilObjectDisappear(up.spinnerBy);
            up.WaitUntilPageLoad();
            up.WaitUntilObjectDisappear(up.spinnerBy);
            up.WaitUntilPageLoad();
            JLog.logger.debug("customerNameBy - Before wait to be clickable ");
            up.WaitUntilObjectClickable(up.customerNameBy);
            JLog.logger.debug("customerNameBy - After wait to be clickable ");
            up.customerName.click();
            JLog.logger.debug("customerName - After click ");

        }
        catch (Exception e) {
        	JLog.logger.error("Could not select customer at portal. Customer: {}", customerName, e);
            org.testng.Assert.fail("Could not select customer at portal. Customer: " + customerName + "\n" + e.toString());
        }

    }

    public void DeleteAllDownloads() {
        try {
            FileCabinet fc = new FileCabinet();
            fc.WaitUntilObjectDisappear(fc.spinnerBy);
            fc.WaitUntilPageLoad();
            fc.WaitUntilObjectClickable(fc.trustwaveEndpointFolderBy);

            fc.WaitUntilObjectDisappear(fc.spinnerBy);
            fc.WaitUntilPageLoad();
            fc.WaitUntilObjectClickable(fc.trustwaveEndpointFolderBy);

            fc.trustwaveEndpointFolder_element.click();

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
        	JLog.logger.error("Could not delete all endpoint downloads at portal.", e);
            org.testng.Assert.fail("Could not delete all endpoint downloads at portal." + "\n" + e.toString());
        }


    }

    public void DownloadFilesFromTrustWaveEndPointFolder(String fileToAppearTimeoutString, String fileStoredAndVirusScanTimeoutString) {
        try {
            FileCabinet fc = new FileCabinet();

            fc.WaitUntilObjectDisappear(fc.spinnerBy);
            fc.WaitUntilPageLoad();
            fc.WaitUntilObjectClickable(fc.trustwaveEndpointFolderBy);

            fc.WaitUntilObjectDisappear(fc.spinnerBy);
            fc.WaitUntilPageLoad();
            fc.WaitUntilObjectClickable(fc.trustwaveEndpointFolderBy);


            fc.trustwaveEndpointFolder_element.click();


            LocalDateTime start = LocalDateTime.now();

            WaitForInstallerToAppearAtFileCabinet(fc.endPointLnxBy,start, fileToAppearTimeoutString);
            WaitForInstallerToAppearAtFileCabinet(fc.endPointExeBy,start, fileToAppearTimeoutString);

            DownloadInstaller(fc.TrustwaveEndpointLnx_element, fc.endPointLnxBy,fileStoredAndVirusScanTimeoutString);
            DownloadInstaller(fc.TrustwaveEndpointExe_element, fc.endPointExeBy,fileStoredAndVirusScanTimeoutString);

        }
        catch (Exception e) {
        	JLog.logger.error("Could not download endpoint installation files", e);
            org.testng.Assert.fail("Could not download endpoint installation files" + "\n" + e.toString());
        }

    }



    private void WaitForInstallerToAppearAtFileCabinet( By installerLinkBy, LocalDateTime start, String fileToAppearTimeoutString) {
        try {

            int fileToAppearTimeout = Integer.parseInt(fileToAppearTimeoutString);

            FileCabinet fc = new FileCabinet();

            boolean found = false;

            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(fileToAppearTimeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                fc.WaitUntilObjectClickable(fc.refreshButtonBy);
                fc.WaitUntilObjectDisappear(fc.spinnerBy);
                fc.WaitUntilObjectClickable(fc.refreshButtonBy);

                fc.refreshButton_element.click();

                Thread.sleep(checkInterval);
                current = LocalDateTime.now();


                if (fc.IsElementExist(installerLinkBy)) {
                    found = true;
                    break;
                }

            }

            if (!found) {
                org.testng.Assert.fail("Download failed. Installation file did not appeared at File Cabinet after timeout: " + fileToAppearTimeoutString + " seconds. See screenshot file.");
            }
        }
        catch (Exception e) {
        	JLog.logger.error("Could not download endpoint installation files", e);
            org.testng.Assert.fail("Could not download endpoint installation files" + "\n" + e.toString());
        }

    }



    private void DownloadInstaller(WebElement installerLink, By installerLinkBy, String fileStoredAndVirusScanTimeoutString) {
        try {

            int fileStoredAndVirusScanTimeout = Integer.parseInt(fileStoredAndVirusScanTimeoutString);

            boolean errorMessageAppear = true;
            FileCabinet fc = new FileCabinet();

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(fileStoredAndVirusScanTimeout);

            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {

                fc.WaitUntilObjectClickable(installerLinkBy);
                installerLink.click();
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

            if (errorMessageAppear) {
                org.testng.Assert.fail("Message appears: File is still being processed (virus scanned and stored). after timeout: " + fileStoredAndVirusScanTimeoutString + " seconds. Timeout reached test failed.");
            }
        }
        catch (Exception e) {
        	JLog.logger.error("Could not download endpoint installation files", e);
            org.testng.Assert.fail("Could not download endpoint installation files" + "\n" + e.toString());
        }

    }

    /**
     * Delete the endpoint with name 'hostname' from Centcom UI
     * Returns - true if an endpoint with this name was found, else otherwise 
     */
    public boolean DeleteEpFromCentCom(String hostname) {
        try {
            CentComSearchDetailsPage detailsPage = new CentComSearchDetailsPage();

            detailsPage.binocularsButton_element.click();

            detailsPage.WaitUntilPageLoad();
            detailsPage.WaitUntilObjectClickable(detailsPage.valueToSearchBy);
            detailsPage.valueToSearch_element.clear();
            detailsPage.valueToSearch_element.sendKeys(hostname + "\n");

            detailsPage.refreshButton_element.click();

            detailsPage.WaitUntilPageLoad();
            detailsPage.WaitUntilObjectDisappear(detailsPage.spinnerBy);
            detailsPage.WaitUntilPageLoad();
            detailsPage.WaitUntilObjectDisappear(detailsPage.spinnerBy);

            By epLineBy = detailsPage.GetHostNameRowBy(hostname);

            Thread.sleep(3000); //after all 3 wait above needs some more - to be investigated

            if (!detailsPage.IsElementExist(epLineBy)) {
                JLog.logger.warn("Could not find the following EP at CentCom: "+ hostname);
                return false;
            }

            WebElement epLineElement = detailsPage.GetHostNameRowWebElement(hostname);
            epLineElement.click();

            detailsPage.actionButton_element.click();

            detailsPage.WaitUntilPageLoad();
            detailsPage.WaitUntilObjectClickable(detailsPage.deleteEndpointsBy);

            detailsPage.deleteEndpointsMenu_element.click();

            detailsPage.WaitUntilPageLoad();
            detailsPage.WaitUntilObjectClickable(detailsPage.continueButtonConfirmDeleteBy);

            detailsPage.continueButtonConfirmDelete_element.click();

            detailsPage.WaitUntilPageLoad();
            detailsPage.WaitUntilObjectDisappear(epLineBy);

            if (detailsPage.IsElementExist(epLineBy)) {
                org.testng.Assert.fail("After deleting endpoint from CentCom the ep still appears at CentCom EP name: " + hostname );
            }

            JLog.logger.info("Successfully deleted endpoint: " + hostname);

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not delete from CentCom endpoint: " + hostname +  "\n" + e.toString());
        }
        return true;

    }




}

