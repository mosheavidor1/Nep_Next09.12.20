package Pages.Portal;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

import Pages.GenericPage;
import WebElements.Button;
import WebElements.PortalComboBox;
import WebElements.TextBox;

public class AddRulePage extends GenericPage {
	private static final String  RuleTypeButtonID = "ruleTypesDD";
	public static final By RuleTypeByID = By.id(RuleTypeButtonID);


	@FindBy(how=How.ID,using=RuleTypeButtonID)
		private WebElement ruleType_element;
		public PortalComboBox ruleType;
		

	@FindBy(how=How.ID,using="actionsDD")
		private WebElement action_element;
		public PortalComboBox Action;

		
	@FindBy(how=How.ID,using="urlInput--material-input")
		private WebElement Url_element;
		public TextBox URL;

		
		
//@FindBy(how=How.ID,using="rulesDialog--primary-button--material-button")
	@FindBy(how=How.XPATH,using="//div[@class='pane modal visible']//twa-button[@id='rulesDialog--primary-button']")
		private WebElement Submit_element;
		public Button Submit;
		
		 public AddRulePage () {
			ruleType=new PortalComboBox(ruleType_element);
			Action=new PortalComboBox(action_element);
			URL=new TextBox(Url_element);
			Submit=new Button(Submit_element);

		 }


}
