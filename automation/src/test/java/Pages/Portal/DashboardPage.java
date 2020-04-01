package Pages.Portal;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

import Pages.GenericPage;
import WebElements.Button;


public class DashboardPage extends GenericPage {	
	public static final By dashboradByID = By.id("dashboard");
	
	 @FindBy(how=How.ID,using="dashboard")
	 	private WebElement dashboard_element;
	 	public Button dashboard;
	 	
		 public DashboardPage () {
			dashboard = new Button(dashboard_element);
		 }

}
