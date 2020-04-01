package Pages.Web;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class ExampleDotComPage extends GenericWebPage {

    @FindBy(how= How.XPATH,using="//h1[contains(text(),'Example Domain')]")
    private WebElement Submit_element;

    public ExampleDotComPage(){
        webPageIdentifier=Submit_element;
    }
}
