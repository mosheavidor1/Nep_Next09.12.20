package Pages.Portal;

import Applications.SeleniumBrowser;
import Pages.GenericPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class CentComSearchDetailsPage extends GenericPage {

    @FindBy(how= How.XPATH,using="//span[contains(text(),'Reset Installer')]")
    public WebElement resetInstaller_element;

    @FindBy(how= How.XPATH,using="//div[@class='pane modal visible']//twa-button[@aria-label='Continue']")
    public WebElement continueButton_element;


    @FindBy(how= How.XPATH,using="//div[normalize-space(text())='Endpoints']")
    public WebElement endPointTab_element;

    @FindBy(how= How.XPATH,using="//span[contains(text(),'Open Configuration')]")
    public WebElement openConfiguration_element;

    //This item removed from Fusion July 2020 version. Still exist in production
    @FindBy(how= How.XPATH,using="//twa-table-search-input//twa-search-input//input[@placeholder='All Columns']")
    public WebElement endPointSearchBox_element;

    @FindBy(how= How.XPATH,using="//twa-button[@icon='tw-binoculars']/material-button")
    public WebElement binocularsButton_element;

    private static final String valueToSearchXpath ="//twa-form-item[@label='Value']//input";
    public static final By valueToSearchBy = By.xpath(valueToSearchXpath);
    @FindBy(how= How.XPATH,using=valueToSearchXpath)
    public WebElement valueToSearch_element;


    //no need for find as text is searched with \n does the same operation
    @FindBy(how= How.XPATH,using="//twa-button[@aria-label='Find']/material-button")
    public WebElement findButton_element;

    @FindBy(how= How.XPATH,using="//twa-button[@icon='tw-refresh']//material-button")
    public WebElement refreshButton_element;

    //spinner
    public static final By spinnerBy =By.xpath("//span/twa-spinner");

    private static final String OkXpath = "//twa-table-row[not(contains(@class,'hidden'))]//div[contains(text(),'Okay')]";
    public static final By OkBy = By.xpath(OkXpath);

    private static final String rowXpath ="//twa-table-row[not(contains(@class,'hidden'))]";
    public static final By rowBy = By.xpath(rowXpath);
    private static final String hostNameRowXpath = rowXpath + "//div[normalize-space(text())='XXX']";

    public static By GetHostNameRowBy(String hostName){
        String xpath = hostNameRowXpath.replace("XXX",hostName);
        By host = By.xpath(xpath);
        return host;
    }

    public static WebElement GetHostNameRowWebElement(String hostName){
        WebElement epRow = SeleniumBrowser.GetDriver().findElement(GetHostNameRowBy(hostName));
        return epRow;
    }

    private static final String actionsButtonXpath ="//twa-menu[@twatooltip='Actions']";
    @FindBy(how= How.XPATH,using=actionsButtonXpath)
    public WebElement actionButton_element;

    private static final String deleteEndpointsXpath ="//span[contains(text(),'Delete Endpoint')]";
    public static final By deleteEndpointsBy = By.xpath(deleteEndpointsXpath);
    @FindBy(how= How.XPATH,using=deleteEndpointsXpath)
    public WebElement deleteEndpointsMenu_element;

    private static final String continueButtonConfirmDeleteXpath ="//div[@class='pane modal visible']//twa-button[@aria-label='Continue']";
    public static final By continueButtonConfirmDeleteBy = By.xpath(continueButtonConfirmDeleteXpath);
    @FindBy(how= How.XPATH,using=continueButtonConfirmDeleteXpath)
    public WebElement continueButtonConfirmDelete_element;

    private static final String totalItemsXpath ="//twa-portal-stage[@id='stg-operations']//span[contains(text(),'Total Items')]";
    public static final By TotalItemsBy = By.xpath(totalItemsXpath);
    @FindBy(how= How.XPATH,using=totalItemsXpath)
    public WebElement totalItems_element;







    /*for future implantation
    private By visibleRowBy = By.xpath( "//twa-table-row[not(contains(@class,'hidden'))]");
    private By nameColumn = By.xpath( "/div[@column='Name']");
    private By stateColumn = By.xpath( "/div[@column='State']");
    private By nameColumn2 = By.xpath( "//twa-table-row[not(contains(@class,'hidden'))]/div[@column='Name']/twa-table-cell-ellipsis/div");

    public String GetHostState(String hostName){
        List<WebElement> rowList = driver.findElements(visibleRowBy);
        for(int index=0;index<rowList.size();index++){
            WebElement currentHostElement = rowList.get(index).findElement(nameColumn);
            String ggg = currentHostElement.getText();

            WebElement currentHostElement2 = rowList.get(index).findElement(nameColumn2);
            String ggg2 = currentHostElement2.getText();

            if (hostName.compareTo(currentHostElement.getText().trim()) == 0) {
                WebElement currentStateElement = rowList.get(index).findElement(stateColumn);
                return currentStateElement.getText().trim();

            }
        }
        return null;

    }*/


}
