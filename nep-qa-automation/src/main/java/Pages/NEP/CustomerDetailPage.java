package Pages.NEP;

import Pages.GenericPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class CustomerDetailPage extends GenericPage {

    public static final By spinnerBy =By.xpath("//span/twa-spinner");

    private static final String editServicesXpath = "//span[contains(text(),'edit services')]";
    public static final By editServicesLinkBy = By.xpath(editServicesXpath);
    @FindBy(how= How.XPATH,using=editServicesXpath)
    public WebElement editServicesLink_element;




}
