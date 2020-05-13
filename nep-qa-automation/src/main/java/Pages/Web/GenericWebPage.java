package Pages.Web;

import Pages.GenericPage;
import org.openqa.selenium.WebElement;

public class GenericWebPage extends GenericPage {
    WebElement webPageIdentifier;

    public boolean isPageAppearAsExpected() throws Exception {
        if (webPageIdentifier.isDisplayed())
            return true;
        else return false;
    }


}
