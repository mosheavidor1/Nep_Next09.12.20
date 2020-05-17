package Pages.Portal;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

import Pages.GenericPage;
import WebElements.Button;

public class UpperMenu extends GenericPage{
		
	
	 @FindBy(how=How.ID,using="dashboard")
	 private WebElement Managment_element;
	 public Button Managment;

	@FindBy(how=How.XPATH,using="//*[@id=\"stg-operations\"]/div/div[1]/div[3]/twa-swg-change-set-plugin/div/twa-swg-customer-change/div/twa-button/material-button/div")
	private WebElement pendingChanes_element;
	public Button pendingChanes;


	//private static final String customerSelectorXpath = "//*[@id=\"stg-support\"]/div/div[1]/twa-org-selector/div/div[3]/div[1]/span";
	private static final String customerSelectorXpath = "//twa-portal-stage[not(@hidden)]//twa-org-selector/div/div[3]/div[1]";
	public static final By customerSelectorBy = By.xpath(customerSelectorXpath);
	@FindBy(how= How.XPATH,using=customerSelectorXpath)
	public WebElement customerSelector_element;

	private static final String searchTextXpath = "/html/body/div/div[*]/div/div/div/main/div/div/div[1]/div/twa-input/div/material-input/div/div[1]/label/input";

	public static final By searchTextBy = By.xpath(searchTextXpath);
	@FindBy(how= How.XPATH,using=searchTextXpath)
	public WebElement searchText_element;

	private static final String customerNameCSS=".customer-name";
	public static final By customerNameBy = By.cssSelector(customerNameCSS);
	@FindBy(how = How.CSS, using = customerNameCSS)
	public WebElement customerName;

	public static final By spinnerBy =By.xpath("//span/twa-spinner");


	public UpperMenu () {
			 Managment = new Button(Managment_element);
			 pendingChanes = new Button(pendingChanes_element);
		 }

}
