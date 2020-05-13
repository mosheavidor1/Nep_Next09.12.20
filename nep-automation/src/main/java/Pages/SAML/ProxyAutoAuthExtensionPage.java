package Pages.SAML;

import Pages.GenericPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class ProxyAutoAuthExtensionPage extends GenericPage {

    @FindBy(how= How.ID,using="login")
    public WebElement userName_element;

    @FindBy(how= How.ID,using="password")
    public WebElement password_element;

    @FindBy(how= How.ID,using="save")
    public WebElement save_element;

}