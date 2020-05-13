package Pages.NEP;

import Pages.GenericPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class CentComConfigurationPage extends GenericPage {

    private static final String publishXpath="//twa-button[@aria-label='Publish']";
    public static final By publishBy = By.xpath(publishXpath);
    @FindBy(how= How.XPATH,using=publishXpath)
    public WebElement publishButton_element;

    private static final String refreshXpath="//twa-button[@label='Refresh']";
    public static final By refreshBy = By.xpath(refreshXpath);
    @FindBy(how= How.XPATH,using=refreshXpath)
    public WebElement refreshButton_element;


    @FindBy(how= How.XPATH,using="//div[@class='pane modal visible']//twa-button[@aria-label='Continue']")
    public WebElement continueButton_element;

    public static final By spinnerBy =By.xpath("//span/twa-spinner");


    private static final String percent100Xpath="//label[contains(text(),'100%')]";
    public static final By percent100By = By.xpath(percent100Xpath);
    @FindBy(how= How.XPATH,using=percent100Xpath)
    public WebElement percent_100_element;




}
