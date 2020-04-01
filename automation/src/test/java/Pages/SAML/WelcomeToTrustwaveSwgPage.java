package Pages.SAML;

import Pages.GenericPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class WelcomeToTrustwaveSwgPage extends GenericPage {

    @FindBy(how= How.ID,using="btnSubmit")
    public WebElement connectButton_element;

}