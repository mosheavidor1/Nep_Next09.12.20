package WebElements;

import org.openqa.selenium.WebElement;

public class Button extends GenericWebElement {
	
	public Button() {
		super();
	}
	
	public Button(WebElement elementToSet) {
		super( elementToSet);
	}

	public void click() {
		element.click();
	}

}
