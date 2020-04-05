package Pages.Wiki;

import Pages.GenericPage;
import WebElements.Button;
import WebElements.TextBox;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class LoginPage extends GenericPage {

    @FindBy(how= How.ID,using="os_username")
    private WebElement UserName_element;
    public TextBox UserName;

    @FindBy(how=How.ID,using="os_password")
    private WebElement Password_element;
    public TextBox Password;

    @FindBy(how=How.ID,using="loginButton")
    private WebElement LoginButton_element;
    public Button LoginButton;


    public LoginPage () {
        UserName = new TextBox(UserName_element);
        Password = new TextBox (Password_element);
        LoginButton = new Button(LoginButton_element);
    }

}
