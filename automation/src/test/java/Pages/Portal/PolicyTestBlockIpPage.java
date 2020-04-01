package Pages.Portal;

import Pages.GenericPage;
import WebElements.TextBox;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class PolicyTestBlockIpPage extends GenericPage {


    //type the website IP in the text box


    public static final By PolicyTestIpType = By.cssSelector("#ipAddress--material-input");


    @FindBy(how = How.CSS, using = "#ipAddress--material-input")

    private WebElement Block_elment;

    public TextBox Block;


    public PolicyTestBlockIpPage() {
        Block = new TextBox(Block_elment);


    }
}
