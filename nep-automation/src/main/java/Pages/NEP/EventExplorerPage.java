package Pages.NEP;

import Pages.GenericPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class EventExplorerPage extends GenericPage {

    @FindBy(how= How.ID,using="refreshAction")
    public WebElement refreshButton_element;

    @FindBy(how= How.XPATH,using="//*[@id='eventsViewSwitch']//twa-button[2]//material-button")
    public WebElement switchToRowViewButton_element;

    @FindBy(how= How.XPATH,using="//*[@id='filterSearchInput']/input")
    public WebElement searchBox_element;

    //at the bottom of screen appears how many line filtered
    @FindBy(how= How.XPATH,using="//twa-table-paginator/span")
    public WebElement filteredItems_element;

    @FindBy(how= How.XPATH,using="//twa-table-row[not(contains(@class, 'hidden'))]//i")
    public WebElement openRowButton_element;

    //event message appear after opening the line
    @FindBy(how= How.XPATH,using="//twa-table-row[not(contains(@class, 'hidden'))]//p")
    public WebElement entryMessage_element;



}
