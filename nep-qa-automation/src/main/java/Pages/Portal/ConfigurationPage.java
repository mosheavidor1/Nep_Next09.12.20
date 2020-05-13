package Pages.Portal;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

import Pages.GenericPage;
import WebElements.Button;
import WebElements.TextBox;

public class ConfigurationPage extends GenericPage{
	
	private static final String  plusButtonID = "rulesGrid--datalist--action-bar--add-button--icon-button";

	private static final String rulesXpath = "//div[contains(text(),'Rules')]";
	private static final String categoriesGridID = "subCategoriesGrid";
	
	public static final By categoriesGridByID = By.id(categoriesGridID);
	public static final By costumerNameBoxByID = By.id("customerSearchInput");
	public static final By subCategorySpinnerByXpath =By.xpath("//*[@id=\"customerPolicies\"]/div/twa-cbox[2]/div[1]/span/twa-spinner");



	@FindBy(how=How.XPATH,using="//*[@id=\"customerSearchInput--material-input\"]/div/div[1]/label/input")
		private WebElement customerName_element;
	 	public TextBox customerNameBox;

	@FindBy(how=How.XPATH,using=rulesXpath)
		private WebElement rules_element;
		public Button rules;

	@FindBy(how=How.ID,using=plusButtonID)
		private WebElement plusButton_element;
		public Button plusButton;
 	
		 public ConfigurationPage () {
			 customerNameBox = new TextBox(customerName_element);
			 rules = new Button(rules_element);
			 plusButton = new Button(plusButton_element);
		 }

		 public WebElement GetFoundCustomerRowElement(String customerNameOrID) {
			 By byXpath = GetFoundCustomerRowBy(customerNameOrID);
			 //WebElement el= this.driver.findElement(byXpath);
			 //System.out.println("Number of elements!!!!: " +this.driver.findElements(byXpath).size());
			 return this.driver.findElement(byXpath);
		 }

		public By GetFoundCustomerRowBy(String customerNameOrID) {
			String xpath="//span[normalize-space(text())='"+ customerNameOrID + "']";
			return By.xpath(xpath);
	}

}
