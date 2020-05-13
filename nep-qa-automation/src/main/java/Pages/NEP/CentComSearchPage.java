package Pages.NEP;

import Pages.GenericPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class CentComSearchPage extends GenericPage {

    @FindBy(how= How.XPATH,using="//twa-form-item[@label='Customers']//input[@type=\"text\"]")
    public WebElement customersText_element;

    @FindBy(how= How.XPATH,using="//twa-button[@aria-label='Search']")
    public WebElement searchButton_element;

    //@FindBy(how= How.XPATH,using="//twa-datalist-column//div[normalize-space(text())='TrustwaveEP Automation #2']")
    //public WebElement row_element;

    @FindBy(how= How.XPATH,using="//twa-button[@icon='tw-search-plus']")
    public WebElement detailsButton_element;

    @FindBy(how= How.XPATH,using="//twa-button[@tooltip=\"Open Configuration\"]")
    public WebElement openConfigurationButton_element;

    private final static String customerRowXpath = "//twa-datalist-column//div[normalize-space(text())='XXX']";

    public WebElement GetCustomerRow(String customerName){

        String xpath = customerRowXpath.replace("XXX", customerName);
        return driver.findElement(By.xpath(xpath));

    }


}
