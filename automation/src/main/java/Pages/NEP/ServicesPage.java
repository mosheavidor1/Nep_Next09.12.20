package Pages.NEP;

import Pages.GenericPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class ServicesPage extends GenericPage {

    @FindBy(how= How.XPATH,using="//span[contains(text(),'MTD')]")
    public WebElement MTD_element;

    @FindBy(how= How.ID,using="serviceProvisingDialog--primary-button--material-button")
    public WebElement OK_element;



}
