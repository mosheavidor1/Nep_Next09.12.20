package Pages.Portal;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

import Pages.GenericPage;
import WebElements.Button;
import WebElements.TextBox;

public class LoginPage extends GenericPage{
	
	 @FindBy(how=How.ID,using="login-username--material-input")
	 	private WebElement UserName_element;
	 	public TextBox UserName;

	 @FindBy(how=How.ID,using="login-password--material-input")
	 	private WebElement Password_element;
	 	public TextBox Password;
	 	
	 @FindBy(how=How.ID,using="login-submit-button--material-button")
	 	private WebElement LoginButton_element;
		 public Button LoginButton;


		 public LoginPage () {
			 UserName = new TextBox(UserName_element);
			 Password = new TextBox (Password_element);
			 LoginButton = new Button(LoginButton_element);
		 }

}
