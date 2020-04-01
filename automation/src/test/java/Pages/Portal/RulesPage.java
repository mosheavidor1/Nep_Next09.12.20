package Pages.Portal;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

import Pages.GenericPage;
import WebElements.Button;

public class RulesPage extends GenericPage {

	 @FindBy(how=How.ID,using="rulesGrid--datalist--action-bar--delete-button--icon-button")
	 	private WebElement delete_element;
	 	public Button delete;

		 public RulesPage () {
			 delete = new Button(delete_element);
		 }
		 
		 public List<WebElement> GetAllRules(String Url) {
			    String xpath ="//span[contains(text(),'"+Url +  "')]";
			 	 return this.driver.findElements(By.xpath(xpath));

		 }

	
}
