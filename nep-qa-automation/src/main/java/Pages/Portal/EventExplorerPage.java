package Pages.Portal;

import Pages.GenericPage;
import org.openqa.selenium.By;
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

    @FindBy(how= How.XPATH,using="//twa-datetime-input/div")
    public WebElement timeRangeBox;

    private static final String last24HoursXpath = "//div[contains(text(),'Last 24 Hours')]";
    public static final By last24HoursBy = By.xpath(last24HoursXpath);
    @FindBy(how= How.XPATH,using=last24HoursXpath)
    public WebElement last24Hours;

    @FindBy(how= How.XPATH,using="//div[@id='default-acx-overlay-container']//twa-button[@aria-label='Apply']/material-button")
    public WebElement applyTimeButton;

    @FindBy(how= How.XPATH,using="//twa-search-builder//twa-select-icon-dropdown//material-button/div")
    public WebElement addQuery;

    private static final String detectorHostQueryXpath = "//twa-select-dropdown-item//span[contains(text(),'Detector Host')]";
    public final By detectorHostQueryBy = By.xpath(detectorHostQueryXpath);
    @FindBy(how= How.XPATH,using=detectorHostQueryXpath)
    public WebElement detectorHostQuery_element;

    @FindBy(how= How.XPATH,using="//div[contains(text(),'Detector Host: All')]")
    public WebElement detectorHostAll;

    @FindBy(how= How.XPATH,using="//input[@aria-label='Specify an option...']")
    public WebElement specifyAnOption;

    @FindBy(how= How.XPATH,using="//twa-select//twa-checkbox")
    public WebElement selectQueryOption;

    @FindBy(how= How.XPATH,using="//twa-button[@aria-label='SEARCH']/material-button")
    public WebElement searchButton;

    public final By spinnerBy =By.xpath("//span/twa-spinner");



}
