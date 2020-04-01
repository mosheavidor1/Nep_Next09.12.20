package WebElements;

import org.openqa.selenium.WebElement;

public class TextBox extends GenericWebElement {

	public TextBox () {
		super ();
		
	}
	
	public TextBox (WebElement elementToSet) {
		super (elementToSet);
	}
	
	public void SetText ( String text) {
		element.click();
		element.sendKeys(text);
	}
	
}
