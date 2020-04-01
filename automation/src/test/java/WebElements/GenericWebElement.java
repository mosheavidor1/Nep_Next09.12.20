package WebElements;

import org.openqa.selenium.WebElement;

public abstract class GenericWebElement {

	public WebElement element;
	
	public GenericWebElement() {
		element = null;
	}
	
	public GenericWebElement ( WebElement elementToSet) {
		element = elementToSet;
	}
}
