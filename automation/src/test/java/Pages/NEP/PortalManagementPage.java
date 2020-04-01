package Pages.NEP;

import Pages.GenericPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class PortalManagementPage extends GenericPage {

    @FindBy(how= How.ID,using="inputText--material-input")
    public WebElement searchTextBox_element;

    @FindBy(how= How.ID,using="searchForm--submit-button")
    public WebElement searchButton_element;

    private static final String customerRowXpath="//div[contains(text(),'MD Essential')]";
    //private static final String customerRowXpath="//div[contains(text(),'nep-automation.com')]";
    public static final By customerRowBy = By.xpath(customerRowXpath);
    @FindBy(how= How.XPATH,using=customerRowXpath)
    public WebElement customerRow_element;

    @FindBy(how= How.XPATH,using="//*[@id=\"customerTable\"]/div[1]/div[2]/twa-table-action-details")
    public WebElement customerMagnifyingGlass_element;



}
