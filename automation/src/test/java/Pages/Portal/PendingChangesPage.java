package Pages.Portal;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

import Pages.GenericPage;
import WebElements.Button;

public class PendingChangesPage extends GenericPage{
	
	private String submitXpath = "//*[@id=\"rulesDialog--primary-button--material-button\"]/div";
	public By submitBy =By.xpath(submitXpath);

	@FindBy(how=How.XPATH,using="//*[@id=\"rulesDialog--primary-button--material-button\"]/div")
 	private WebElement submit_element;
 	public Button submit;
	
 public PendingChangesPage () {
	 submit = new Button(submit_element);
 }


}
