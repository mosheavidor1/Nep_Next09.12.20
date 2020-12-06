package Pages.Portal;

import Applications.SeleniumBrowser;
import Pages.GenericPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

import java.util.List;

public class FileCabinet extends GenericPage {
    public static final By spinnerBy =By.xpath("//span/twa-spinner");

    private static final String trustwaveEndpointFolderID = "endpointprotection";
    public static final By trustwaveEndpointFolderBy = By.id(trustwaveEndpointFolderID);
    @FindBy(how= How.ID,using="endpointprotection")
    public WebElement trustwaveEndpointFolder_element;

    private static final String endPointExeXpath = "//span[contains(text(),'TrustwaveEndpoint.exe')]";
    public static final By endPointExeBy = By.xpath(endPointExeXpath);
    @FindBy(how= How.XPATH,using=endPointExeXpath)
    public WebElement TrustwaveEndpointExe_element;

    private static final String endPointLnxXpath = "//span[contains(text(),'TrustwaveEndpoint.centos') and not(contains(text(),'sha256'))]";
    public static final By endPointLnxBy = By.xpath(endPointLnxXpath);
    @FindBy(how= How.XPATH,using=endPointLnxXpath)
    public WebElement TrustwaveEndpointLnx_element;

    private static final String endPointMsiXpath = "//span[contains(text(),'TrustwaveEndpoint.msi')]";
    public static final By endPointMsiBy = By.xpath(endPointMsiXpath);
    @FindBy(how= How.XPATH,using=endPointMsiXpath)
    public WebElement TrustwaveEndpointMsi_element;

    private static final String endPointUbuntuXpath = "//span[contains(text(),'TrustwaveEndpoint.ubuntu') and not(contains(text(),'sha256'))]";
    public static final By endPointUbuntuBy = By.xpath(endPointUbuntuXpath);
    @FindBy(how= How.XPATH,using=endPointUbuntuXpath)
    public WebElement TrustwaveEndpointUbuntu_element;


    private static final String clientKeyXpath = "//span[contains(text(),'client_key.pem')]";
    public static final By clientKeyBy = By.xpath(clientKeyXpath);
    @FindBy(how= How.XPATH,using=clientKeyXpath)
    public WebElement client_key_element;

    private static final String clientPemXpath = "//span[contains(text(),'client.pem')]";
    public static final By clientPemBy = By.xpath(clientPemXpath);
    @FindBy(how= How.XPATH,using=clientPemXpath)
    public WebElement clientPem_element;

    private static final String threeDotsIconXpath = "//twa-menu";
    public static final By threeDotsIconBy = By.xpath(threeDotsIconXpath);
    @FindBy(how= How.XPATH,using=threeDotsIconXpath)
    public WebElement threeDotsIcon_element;

    //The error message contains the following:
    //div[contains(text(),'File is still being processed (virus scanned and stored')]
    private static final String fileUnableToBeDownloadedXpath = "//div[@class='pane modal visible']//div[contains(text(),'File is still being processed (virus scanned and stored')]";
    public static final By fileUnableToBeDownloadedBy = By.xpath(fileUnableToBeDownloadedXpath);
    @FindBy(how= How.XPATH,using=fileUnableToBeDownloadedXpath)
    public WebElement fileUnableToBeDownloaded_element;

    @FindBy(how= How.XPATH,using="//div[@class='pane modal visible']//twa-button[@aria-label='OK']")
    public WebElement errorMessageOKButton_element;



    public List<WebElement> GetFirstThreeDotsIcon() {
        SeleniumBrowser.ChangeImplicitWait(10);
        List<WebElement>  list = this.driver.findElements(threeDotsIconBy);
        SeleniumBrowser.ChangeImplicitWait(0);

        return list;
    }

    private static final String removeMenuItemXpath = "//span[contains(text(),'Remove')]";
    public static final By removeMenuItemBy = By.xpath(removeMenuItemXpath);
    @FindBy(how= How.XPATH,using=removeMenuItemXpath)
    public WebElement removeMenuItem_element;

    private static final String removeButtonConfirmXpath = "//div[@class='pane modal visible']//twa-button[1]";
    public static final By removeButtonConfirmBy = By.xpath(removeButtonConfirmXpath);
    @FindBy(how= How.XPATH,using=removeButtonConfirmXpath)
    public WebElement removeButtonConfirm_element;

    private static final String refreshButtonXpath = "//twa-file-cabinet-view//twa-datalist//twa-button[@icon='tw-refresh']//material-button";
    public static final By refreshButtonBy = By.xpath(refreshButtonXpath);
    @FindBy(how= How.XPATH,using=refreshButtonXpath)
    public WebElement refreshButton_element;


}
