package Actions;

import Pages.NEP.*;
import Pages.Portal.UpperMenu;
import Utils.EventsLog.LogEntry;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

public class NepActions extends CloudActions{
    private static final String PortalManagmentURL = "/#/operations?menuKey=cua-search&stackKey=search-home";
    private static final String FileCabinetURL = "/#/support?menuKey=file-cabinet&stackKey=file-cabinet-files";
    private static final String centComSearchURL = "/#/operations?menuKey=centcom-devices&stackKey=entity-search";
    private static final String eventExplorerURL = "/#/dataexplorer?menuKey=log-management-search&stackKey=log-event-search";


    private static final String destinationFolder = "C:\\PROGRA~1\\Trustwave\\NEPAgent\\certs";
    private static final String clientKeyPem = "\\client_key.pem";
    private static final String clientPem = "\\client.pem";
    private static final String windowsInstallationFile = "TrustwaveEndpoint.exe";

    private static final String dbJsonPath = "C:\\ProgramData\\Trustwave\\NEPAgent\\db.json";
    private static final String configJsonPath = "C:\\ProgramData\\Trustwave\\NEPAgent\\config.json";
    private static final String configJsonReportInterval = "\"report_period\":";
    private static final int checkInterval = 5000;

    private static final int ServiceStartStopTimeout = 180;


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

    //verify log entry message appears at the portal
    public void VerifyMessageExistsInPortal(LogEntry entry, int timeoutForLogEntryToAppearInSeconds) throws InterruptedException {
        EventExplorerPage eventPage = new EventExplorerPage();
        final String filteredItemsText= "Filtered Items";

        boolean found = false;

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime current = start;
        Duration durationTimeout = Duration.ofSeconds(timeoutForLogEntryToAppearInSeconds);

        while ( durationTimeout.compareTo( Duration.between(start,current) ) > 0 ) {
            eventPage.searchBox_element.clear();

            eventPage.refreshButton_element.click();
            eventPage.searchBox_element.sendKeys(entry.stampAdded);

            Thread.sleep(checkInterval);
            current = LocalDateTime.now();

            String filteredString = eventPage.filteredItems_element.getText();

            if( ! filteredString.contains(filteredItemsText))
                continue;
            filteredString = filteredString.substring(filteredString.indexOf(filteredItemsText)+filteredItemsText.length()).trim();

            int filteredItems =0;
            filteredItems = Integer.parseInt(filteredString.trim());

            if (filteredItems == 1) {
                found = true;
                break;
            }
            if (filteredItems>1)
                org.testng.Assert.fail("Found too many results for the unique time stamp: " + entry.stampAdded + " See screenshot/video links below");

        }

        if(!found)
            org.testng.Assert.fail("Relevant log entry do not appea after timeout: " + timeoutForLogEntryToAppearInSeconds+ " seconds. See screenshot or video links below.\nExpected stamp: " + entry.stampAdded );

        eventPage.openRowButton_element.click();

        String logEntryFoundAtPortal = eventPage.entryMessage_element.getText();
        if(!logEntryFoundAtPortal.contains(entry.stampAdded))
            org.testng.Assert.fail("Event entry description found at the portal do not contain expected stamp: " + entry.stampAdded);
        final String  eventIdLocator = "EventID:";
        int startOfEventID = logEntryFoundAtPortal.indexOf(eventIdLocator)+eventIdLocator.length();
        int end = logEntryFoundAtPortal.indexOf("|",startOfEventID);
        String foundID = logEntryFoundAtPortal.substring(startOfEventID,end );
        if(foundID.compareToIgnoreCase(entry.eventID) !=0 )
            org.testng.Assert.fail("Event written to log: "+ entry.eventID + " Is not matched to the event ID found at portal: " +foundID);

    }

    public void PublishNewDownloads(String customerName, int waitForPublishInSec) throws IOException, InterruptedException {
        CentComSearchPage centSearch = new CentComSearchPage();
        centSearch.customersText_element.sendKeys(customerName);
        centSearch.searchButton_element.click();
        //centSearch.row_element.click();
        centSearch.GetCustomerRow(customerName).click();
        centSearch.detailsButton_element.click();

        CentComSearchDetailsPage detailsPage = new CentComSearchDetailsPage();
        detailsPage.resetInstaller_element.click();
        detailsPage.continueButton_element.click();

        this.GotoCentComSearch(PropertiesFile.getCurrentClusterLink());

        centSearch.openConfigurationButton_element.click();

        CentComConfigurationPage conf = new CentComConfigurationPage();
        conf.WaitUntilObjectDisappear(conf.spinnerBy);
        conf.WaitUntilObjectClickable(conf.publishBy);
        conf.publishButton_element.click();
        conf.continueButton_element.click();

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime current = start;
        Duration durationTimeout = Duration.ofSeconds(waitForPublishInSec);
        boolean found =false;

        while ( durationTimeout.compareTo( Duration.between(start,current) ) > 0 ) {
            Thread.sleep(5000);
            current = LocalDateTime.now();
            conf.refreshButton_element.click();
            if( conf.IsElementExist(conf.percent100By) ){
                found = true;
                break;
            }
        }

        if(!found)
            org.testng.Assert.fail("Publish configuration not completed successfully. Could not find publish completed \"100%\" indication after timeout of: " + waitForPublishInSec + "(sec) See screenshot/video links below");


    }

    public void CheckEndPointOkInCentCom(String customerName) throws IOException, InterruptedException {
        CentComSearchPage centSearch = new CentComSearchPage();
        centSearch.customersText_element.sendKeys(customerName);
        centSearch.searchButton_element.click();
        centSearch.GetCustomerRow(customerName).click();
        centSearch.detailsButton_element.click();

        CentComSearchDetailsPage detailsPage = new CentComSearchDetailsPage();
        detailsPage.endPointTab_element.click();

        String host = InetAddress.getLocalHost().getHostName();

        detailsPage.endPointSearchBox_element.sendKeys(host+"\n");
        detailsPage.refreshButton_element.click();

        detailsPage.WaitUntilPageLoad();
        detailsPage.WaitUntilObjectDisappear(detailsPage.spinnerBy);
        detailsPage.WaitUntilObjectClickable(detailsPage.rowBy);

        Thread.sleep(5000); //after all 3 wait above needs some more - to be investigated

        if(!detailsPage.IsElementExist(detailsPage.GetHostNameRowBy(host))) {
            org.testng.Assert.fail("Could not find hostname: " + host);
        }

        if(!detailsPage.IsElementExist(detailsPage.OkBy)) {
            org.testng.Assert.fail("Host: " + host + " Status is not Okay. See screenshot/video.");
        }

    }



    public void SelectCustomer(String customerName){

        UpperMenu up = new UpperMenu();
        up.WaitUntilObjectClickable(up.customerSelectorBy);
        up.customerSelector_element.click();

        up.WaitUntilObjectClickable(up.searchTextBy);
        up.searchText_element.clear();
        up.searchText_element.sendKeys(customerName);

        up.WaitUntilObjectClickable(up.customerNameBy);
        up.customerName.click();

    }

    public void DeleteAllDownloads(){


        FileCabinet fc = new FileCabinet();
        fc.TrustwaveEndpointFolder_element.click();

        fc.WaitUntilObjectDisappear(fc.spinnerBy);
        fc.WaitUntilObjectClickable(fc.refreshButtonBy);
        fc.WaitUntilPageLoad();


        while (fc.GetFirstThreeDotsIcon().size()>0 ) {
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

    }

    public void DownloadFilesFromTrustWaveEndPointFolder(String fileToAppearTimeoutString, String fileStoredAndVirusScanTimeoutString) throws InterruptedException {
        int fileToAppearTimeout = Integer.parseInt(fileToAppearTimeoutString);
        int fileStoredAndVirusScanTimeout = Integer.parseInt(fileStoredAndVirusScanTimeoutString);

        FileCabinet fc = new FileCabinet();

        fc.TrustwaveEndpointFolder_element.click();

        boolean found = false;


        LocalDateTime start = LocalDateTime.now();
        LocalDateTime current = start;
        Duration durationTimeout = Duration.ofSeconds(fileToAppearTimeout);

        while ( durationTimeout.compareTo( Duration.between(start,current) ) > 0 ) {
            fc.WaitUntilObjectClickable(fc.refreshButtonBy);
            fc.WaitUntilObjectDisappear(fc.spinnerBy);
            fc.WaitUntilObjectClickable(fc.refreshButtonBy);

            fc.refreshButton_element.click();

            Thread.sleep(checkInterval);
            current = LocalDateTime.now();




            if ( fc.IsElementExist(FileCabinet.endPointExeBy)) {
                found = true;
                break;
            }


        }

        if(!found)
            org.testng.Assert.fail("Download failed. Installation file did not appeared at File Cabinet after timeout: " + fileToAppearTimeoutString+ " seconds. See screenshot or video links below" );



        boolean errorMessageAppear = true;


        start = LocalDateTime.now();
        current = start;
        durationTimeout = Duration.ofSeconds(fileStoredAndVirusScanTimeout);

        while ( durationTimeout.compareTo( Duration.between(start,current) ) > 0 ) {

            fc.WaitUntilObjectClickable(fc.endPointExeBy);
            fc.TrustwaveEndpointExe_element.click();
            if (! fc.IsElementExist(fc.fileUnableToBeDownloadedBy) ) {
                errorMessageAppear=false;
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

        if(errorMessageAppear)
            org.testng.Assert.fail("Message appears: File is still being processed (virus scanned and stored). after timeout: " + fileStoredAndVirusScanTimeoutString+ " seconds. See video link below" );



    }


    public void CreateAndCleanDownloadFolder() throws IOException {
        File nepFolder = new File(PropertiesFile.readProperty("DownloadFolder"));
        //Creating the directory
        if (! nepFolder.exists() || ! nepFolder.isDirectory() ) {
            boolean bool = nepFolder.mkdir();
            if ( ! bool)
                org.testng.Assert.fail("Could not create download directory: " + nepFolder );

        }

        try {
            FileUtils.cleanDirectory(nepFolder);
        } catch (Exception e) {
            org.testng.Assert.fail("Could delete all old files from the following directory: " + nepFolder + "\n" + e.toString() );
        }

    }

    public void VerifyFilesExist (int timeoutSeconds) throws IOException, InterruptedException {
        File nepFolder = new File(PropertiesFile.readProperty("DownloadFolder"));
        //String [] expected = {"client.pem" , "client_key.pem" , "TrustwaveEndpoint.exe"};
        String [] expected = {windowsInstallationFile};

        boolean foundFiles =false;
        String [] filesArr = nepFolder.list();

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime current = start;
        Duration durationTimeout = Duration.ofSeconds(timeoutSeconds);
        while ( durationTimeout.compareTo( Duration.between(start,current) ) > 0 ) {
            Thread.sleep(checkInterval);
            current = LocalDateTime.now();
            filesArr = nepFolder.list();
            if (Arrays.equals(nepFolder.list(), expected)) {
                foundFiles = true;
                break;
            }
        }
        if (foundFiles == false)
                org.testng.Assert.fail("Could not find expected installation files at folder: " + nepFolder + "\n"+ "Found files:   " + Arrays.toString(filesArr)
                    + "\n" + "Expected files:" + Arrays.toString(expected));

    }

    public void VerifyInstallerSignature() throws IOException {
        final String expectedVerified1 = "Verified:\tA certificate chain processed, but terminated in a root certificate which is not trusted by the trust provider";
        final String expectedVerified2 = "Verified:\tSigned ";

        String sigcheckPath = "c:\\Selenium\\Utils\\sigcheck.exe";

        File file = new File(sigcheckPath);
        if( ! file.exists())
            org.testng.Assert.fail("Signature check failed. Could not find signature check utility at: " + sigcheckPath);

        String command = sigcheckPath + " -nobanner -a ";
        String installerLocation = PropertiesFile.readProperty("DownloadFolder");
        installerLocation += "\\" + windowsInstallationFile;
        command += installerLocation;
        String result = execCmd(command , false);
        if (  ! (result.contains(expectedVerified1)  || result.contains(expectedVerified2) ) )
            org.testng.Assert.fail("Failed to verify siganture of file: " + installerLocation + "\nCheck Signature output:\n" + result
                    + "\nExpected check signature result could be one of the following:\n" + expectedVerified1 + "\nOr:\n" + expectedVerified2);

        int startLine = result.indexOf("Binary Version");
        int endLine = result.indexOf("\n", startLine);
        String version = result.substring(startLine,endLine);
        JLog.logger.info("End Point Agent " + version);

    }

    public void InstallEndPoint(int timeout) throws IOException, InterruptedException {

        String installerLocation = PropertiesFile.readProperty("DownloadFolder");
        installerLocation += "\\" + windowsInstallationFile;
        //String host = "DS_HOST_NAME=" + PropertiesFile.getCurrentClusterNepHost() ;
        String command =  installerLocation + " /q " ;//+ host;
        String result = execCmd(command, false);
        boolean found = false;

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime current = start;
        Duration durationTimeout = Duration.ofSeconds(timeout);

        while ( durationTimeout.compareTo( Duration.between(start,current) ) > 0 ) {
            Thread.sleep(checkInterval);
            current = LocalDateTime.now();
            if (EndPointServiceExist()) {
                found=true;
                break;
            }
        }

        if(!found)
            org.testng.Assert.fail("Trustwave Endpoint installation failed. Trustwave Endpoint Agent Service was not found on services list");

    }

    public void UnInstallEndPoint(int timeout) throws IOException, InterruptedException {


        String installerLocation = PropertiesFile.readProperty("DownloadFolder");
        installerLocation += "\\" + windowsInstallationFile;
        String command =  installerLocation + " /q /uninstall" ;

        //wmic is not working because EP bootstrap has missing data
        //execCmd("wmic product where \"description='Trustwave Endpoint Agent (64bit)' \" uninstall",true);
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime current = start;
        Duration durationTimeout = Duration.ofSeconds(timeout);

        execCmd(command, false);
        boolean found = true;
        while ( durationTimeout.compareTo( Duration.between(start,current) ) > 0 ) {
            Thread.sleep(checkInterval);
            current = LocalDateTime.now();
            if (!EndPointServiceExist()) {
                found=false;
                break;
            }
        }

        if(found)
            org.testng.Assert.fail("Uninstall failed. Trustwave Endpoint Agent Service still found after timeout(sec): " + Integer.toString(timeout));


        final String installationFolder = "C:\\Program Files\\Trustwave\\NEPAgent";
        File file = new File(installationFolder);
        found = true;

        while ( durationTimeout.compareTo( Duration.between(start,current) ) > 0 ) {
            if (!file.exists()) {
                found=false;
                break;
            }
            Thread.sleep(checkInterval);
            current = LocalDateTime.now();
            //JLog.logger.debug("Found Folder!!!");

        }

        if(found)
            org.testng.Assert.fail("Uninstall failed. Trustwave Endpoint installation folder not deleted  after timeout(sec): " + Integer.toString(timeout) + "   Installation folder: " + installationFolder);


        found = true;
        while ( durationTimeout.compareTo( Duration.between(start,current) ) > 0 ) {

            String result = execCmd("tasklist", false);
            if (! result.contains(windowsInstallationFile)) {
                found= false;
                break;
            }
            //JLog.logger.debug("Found Process!!!");
            Thread.sleep(checkInterval);
            current = LocalDateTime.now();

        }

        if(found)
            org.testng.Assert.fail("Uninstall failed. Trustwave installation process is still active after timeout (sec): " +  Integer.toString(timeout) + "   Installation process: " + windowsInstallationFile);

    }


    public void StopEPService (int timeout) throws IOException, InterruptedException {
        execCmd("Net stop NepaService", false);

        boolean active = true;
        String result ="";

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime current = start;
        Duration durationTimeout = Duration.ofSeconds(timeout);

        while ( durationTimeout.compareTo( Duration.between(start,current) ) > 0 ) {
            Thread.sleep(checkInterval);
            current = LocalDateTime.now();
            result = execCmd("sc query \"NepaService\"", false);
            if (result.contains("STOPPED")) {
                active = false;
                break;
            }
        }

        if(active)
            org.testng.Assert.fail("Failed to stop End Point service");

    }

    public void StartEPService(int timeout) throws IOException, InterruptedException {
        execCmd("Net start NepaService", false);

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime current = start;
        Duration durationTimeout = Duration.ofSeconds(timeout);

        boolean active = false;

        while ( durationTimeout.compareTo( Duration.between(start,current) ) > 0 ) {
            Thread.sleep(checkInterval);
            current = LocalDateTime.now();

            String result = execCmd("sc query \"NepaService\"", false);
            if (result.contains("RUNNING")) {
                active = true;
                break;
            }
        }

        if(!active)
            org.testng.Assert.fail("Failed to start End Point service");

    }

    public void ChangeReportInterval(String interval) throws IOException, InterruptedException {

        StopEPService(ServiceStartStopTimeout);

        File file = new File(configJsonPath);
        if( ! file.exists())
            org.testng.Assert.fail("Could not find config.json file was not found at: " + dbJsonPath);

        FileInputStream inputStream = new FileInputStream(configJsonPath);
        String text = IOUtils.toString(inputStream, Charset.defaultCharset());
        inputStream.close();

        if( ! text.contains(configJsonReportInterval)) {
            StartEPService(ServiceStartStopTimeout);
            org.testng.Assert.fail("Could not change the logs interval as " + configJsonReportInterval + " could not be found at: " + configJsonReportInterval);
        }

        int start= text.indexOf(configJsonReportInterval)+ configJsonReportInterval.length();
        int end = text.indexOf(",",start);
        StringBuilder builder = new StringBuilder(text);
        builder.replace(start,end,interval);

        PrintWriter out = new PrintWriter(configJsonPath);
        out.print(builder.toString());
        out.close();


        inputStream = new FileInputStream(configJsonPath);
        text = IOUtils.toString(inputStream, Charset.defaultCharset());
        inputStream.close();
        end = text.indexOf(",",start);
        String foundInFile = text.substring(start,end);
        if(foundInFile.compareTo(interval) != 0 )
            org.testng.Assert.fail("Could not change value of report interval at at the file: " + configJsonPath);

        StartEPService(ServiceStartStopTimeout);

    }


    public void WriteEvent (LogEntry entry) throws IOException {
        //Example "EventCreate /t INFORMATION /id 1234 /l APPLICATION /so AutomationTest /d \"Hello!! this is the test info\""
        if (entry.addedTimeToDescription)
            entry.AddTimeToDescription(java.time.LocalDateTime.now().toString());
        entry.eventDescription = "\"" + entry.eventDescription + "\"";
        String eventCommand ="EventCreate /t " + entry.eventType + " /id " + entry.eventID + " /l " + entry.eventLog + " /so " + entry.eventSource + " /d " + entry.eventDescription;
        String result = execCmd(eventCommand,false);
        if ( ! result.contains("SUCCESS: An event of type"))
            org.testng.Assert.fail("Could no add log event.\nAdd event result: " + result + "\nCommand sent: " +eventCommand);

    }




    public void CheckEndPointActiveByDbJson(int timeout) throws IOException, InterruptedException {
        String text = "";
        boolean active = false;
        File file = new File(dbJsonPath);

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime current = start;
        Duration durationTimeout = Duration.ofSeconds(timeout);

        while ( durationTimeout.compareTo( Duration.between(start,current) ) > 0 ) {
            if( file.exists()) {
                FileInputStream inputStream = new FileInputStream(dbJsonPath);
                text = IOUtils.toString(inputStream, Charset.defaultCharset());
                inputStream.close();
                if (text.contains("\"EndpointId\": \"")  && text.contains("\"DsInitialHost\": ")) {
                    active = true;
                    break;
                }
            }

            Thread.sleep(checkInterval);
            current = LocalDateTime.now();

        }

        if(! file.exists())
            org.testng.Assert.fail("Service is not connected - db.json file was not found at: " + dbJsonPath + " after timeout(sec): " + timeout );

        if(! active)
            org.testng.Assert.fail("Service is not connected according to db.json file after timeout(sec): " + timeout + ". Failed to find in db.json: End Point ID  Or Host.\n"+ "db.json file content:\n"+text);

    }


    public boolean EndPointServiceExist() throws IOException {
        //String result = execCmd("net start | find \"Trustwave Endpoint Agent Service\"");
        String result = execCmd("net start", false);
        if (result.contains("Trustwave Endpoint Agent Service"))
            return true;
        else
            return false;

    }

}

/*
    //legacy flow - Setting customer MTD would generate files creation
    public void SetCustomerMTD(String customerName){
        PortalManagementPage mngPage = new PortalManagementPage();
        mngPage.searchTextBox_element.sendKeys(customerName + "\n");
        mngPage.searchTextBox_element.click(); //help to avoid issue
        //mngPage.searchButton_element.click(); //search button click is not necessary
        mngPage.WaitUntilObjectClickable(mngPage.customerRowBy);
        mngPage.customerRow_element.click();
        mngPage.customerMagnifyingGlass_element.click();

        CustomerDetailPage detailPage = new CustomerDetailPage();
        detailPage.WaitUntilObjectClickable(detailPage.editServicesLinkBy);
        detailPage.WaitUntilObjectDisappear(detailPage.spinnerBy);
        detailPage.WaitUntilObjectClickable(detailPage.editServicesLinkBy);

        detailPage.editServicesLink_element.click();
        ServicesPage srv = new ServicesPage();
        srv.MTD_element.click();
        srv.OK_element.click();
        detailPage.WaitUntilObjectClickable(detailPage.editServicesLinkBy);
        detailPage.editServicesLink_element.click();
        srv.MTD_element.click();
        srv.OK_element.click();

    }


        //legacy flow = replace certificate files
        public void ReplaceEndPointFilesAndRestartService(int timeout) throws IOException, InterruptedException {

        execCmd("Net stop NepaService", true);

        boolean active = true;
        String result ="";
        for(int count = 0; count <= timeout ; count+=5) {
            Thread.sleep(5000);
            result = execCmd("sc query \"NepaService\"", false);
            if (result.contains("STOPPED")) {
                active = false;
                break;
            }
        }

        if(active)
            org.testng.Assert.fail("Failed to stop End Point service");

        String downloadLocation = PropertiesFile.readProperty("DownloadFolder");

        //Shortcut to program files is used to avoid space failure
        String clientKeyPemPath = downloadLocation + clientKeyPem;
        String command = "xcopy /Y " + clientKeyPemPath + " " + destinationFolder;
        execCmd(command, true);

        String clientPemPath = downloadLocation + clientPem;
        command = "xcopy /Y " + clientPemPath + " " + destinationFolder;
        execCmd(command, true);

        File file1 = new File(clientKeyPemPath);
        File file1Copied = new File(destinationFolder + clientKeyPem );
        File file2 = new File(clientPemPath);
        File file2Copied = new File(destinationFolder + clientPem );

        boolean fileCopied = false;
        for(int count = 0; count <= 30 ; count+=2) {
            Thread.sleep(2000);
            if (FileUtils.contentEquals(file1, file1Copied)  && FileUtils.contentEquals(file2, file2Copied)) {
                fileCopied = true;
                break;
            }
        }

        if (! fileCopied)
            org.testng.Assert.fail("Failed copy downloaded client_key.pem and client.pem to folder: " + destinationFolder );

        execCmd("Net start NepaService", true);

        active = false;
        for(int count = 0; count <= timeout ; count+=5) {
            Thread.sleep(5000);
            result = execCmd("sc query \"NepaService\"", false);
            if (result.contains("RUNNING")) {
                active = true;
                break;
            }
        }

        if(!active)
            org.testng.Assert.fail("Failed to start End Point service");

    }

*/