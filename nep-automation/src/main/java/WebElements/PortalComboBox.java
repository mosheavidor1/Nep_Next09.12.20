package WebElements;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import Applications.SeleniumBrowser;

public class PortalComboBox extends GenericWebElement {
	

	public PortalComboBox () {
		super ();
		
	}

	public PortalComboBox (WebElement elementToSet) {
		super (elementToSet);
	}
	
	public void SetValue ( String value) {
		
		
		element.click();
		
		String xpath =	"//span[contains(text(),'"+value + "') and not (contains(text(), '-')) ]";
		
		//This function working for now. But in the future maybe because of similar text object another function will be add to select from combo according to order
		//String xpath = "//*[@id=\"body\"]/div/div[2]/div/material-list/div/div[5]";

		By byXpathValue = By.xpath(xpath);
		
		WebDriverWait wait = new WebDriverWait(SeleniumBrowser.GetDriver(),  60);
		wait.until(ExpectedConditions.elementToBeClickable(byXpathValue));

		//WebElement valueElement =  element.findElement(byXpathValue);
		List<WebElement> valueElement =  element.findElements(byXpathValue);

	   	JavascriptExecutor executor = (JavascriptExecutor)SeleniumBrowser.GetDriver();
	   executor.executeScript("arguments[0].click();", valueElement.get(valueElement.size()-1));
	   //valueElement.get(valueElement.size()-1).click();		

	}



}
