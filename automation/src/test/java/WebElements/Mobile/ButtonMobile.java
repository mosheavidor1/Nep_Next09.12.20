package WebElements.Mobile;

import java.util.List;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import Applications.AndroidApplication;
import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;

public class ButtonMobile {

	private MobileElement element;
	private String identifier;
	private String byWhat;
		
	public ButtonMobile(AndroidElement element, String identifier, String byWhat) {
		this.identifier= identifier;
		this.byWhat= byWhat;
		this.element=element;
	}
	
	public void click() {
		element.click();
	}
	
	public String getText() {
		return element.getText();
	}
    //Second install certificate(Select certificate message) does not always accept Appium click as a workaround another click is required if the first click did not work
    public void ClickIfExists() throws Exception {
    	
    	AndroidDriver<AndroidElement> driver =AndroidApplication.GetDriver();
    	
    	List<AndroidElement>okButtonMissing = null;
    	if(byWhat.compareTo("id") == 0)
    		okButtonMissing= driver.findElements(MobileBy.id(identifier));
    	else throw new Exception("By Method: " + byWhat + "  is not implemented yet please implement.");
    	
		if(okButtonMissing != null && !okButtonMissing.isEmpty() && okButtonMissing.size() == 1)
			okButtonMissing.get(0).click();
   	
    }
    
    public void WaitUntilTextBe(String text , int seconds) {
    	AndroidDriver<AndroidElement> driver =AndroidApplication.GetDriver();
 	
    	WebDriverWait wait = new WebDriverWait(driver, seconds);
    	wait.until(ExpectedConditions.textToBe(MobileBy.id(identifier), text));

    }
}
