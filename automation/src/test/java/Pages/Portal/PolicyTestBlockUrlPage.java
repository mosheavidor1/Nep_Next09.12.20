package Pages.Portal;

import Pages.GenericPage;
import WebElements.TextBox;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class PolicyTestBlockUrlPage extends GenericPage {






    //Policy Test Blocked Url


    public static final By PolicyTestUrlType = By.xpath("//material-input[@id='url--material-input']");


    @FindBy(how = How.XPATH, using = "//material-input[@id='url--material-input']")

    private WebElement Block_elment;

    public TextBox Block;


    public PolicyTestBlockUrlPage() {
        Block = new TextBox(Block_elment);


    }


}



