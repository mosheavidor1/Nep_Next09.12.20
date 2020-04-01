package Pages.SAML;

import Pages.GenericPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class PingIdentityPage extends GenericPage {

    @FindBy(how= How.ID,using="ping-username")
    public WebElement username_element;

    @FindBy(how= How.ID,using="ping-password")
    public WebElement password_element;

    @FindBy(how= How.ID,using="btn-sign-in")
    public WebElement SignIn_element;

}