package Pages.Portal;



import Pages.GenericPage;
import WebElements.Button;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class PolicyTestPage extends GenericPage {

    WebDriver driver;


    //Clicks on the trustwave123 Button.

   //public static final By Trustwave123 = By.cssSelector("div.autocomplete-selector:nth-child(5) > div:nth-child(1) > span:nth-child(1)");
 //public static final By Trustwave123 = By.xpath("//div[contains(text(),'Trustwave 123')]");
 public static final By Trustwave123 = By.xpath("//*[@id=\"stg-operations\"]/div/div[1]/twa-org-selector/div/div[3]/div[1]/span");


 //@FindBy(how = How.CSS, using = "div.autocomplete-selector:nth-child(5) > div:nth-child(1) > span:nth-child(1)")
// @FindBy(how = How.XPATH, using = "//div[contains(text(),'Trustwave 123')]")
 @FindBy(how = How.XPATH, using = "//*[@id=\"stg-operations\"]/div/div[1]/twa-org-selector/div/div[3]/div[1]/span")


    private WebElement PolicyTest_elements;
    public Button TrustWaveButton;

    public PolicyTestPage(){
        TrustWaveButton=new Button(PolicyTest_elements);
    }

    public void TrustWaveButton() {

        Button buttonTrust123 = new Button(PolicyTest_elements);

        buttonTrust123.click();


    }


    //Set the tenant ID on Policy Test

//    public static final By Trustwave123TextBox = By.xpath("div.autocomplete-selector:nth-child(5) > div:nth-child(1) > span:nth-child(1)");
//
//
//    @FindBy(how = How.XPATH, using = "/html/body/div[1]/div[4]/div/div/div/main/div/div/div[1]/div/twa-input/div/material-input/div/div[1]/label/input")
//
//
//    private WebElement client_id;
//
//    public void Trustwave123ChangeClient() {
//
//        TextBox text123Field = new TextBox(client_id);
//        Button text123B = new Button(client_id);
//
//
//        text123B.element.click();
//
//        text123B.element.sendKeys("twtest_swgcloud13");



//
//
////Choose the tenant ID on Policy Test
//
//    public static final By ChooseTenant = By.cssSelector(".customer-name");
//
//
//    @FindBy(how = How.CSS, using = ".customer-name")
//
//    private WebElement Tenant_Id;
//
//    public void ChooseTenantId() {
//
//
//        Button TenantId = new Button(Tenant_Id);
//
//
//        TenantId.element.click();
//
//
//    }

//type the website url in the text box

//    public static final By PolicyTestUrlType = By.xpath("//material-input[@id='url--material-input']");
//
//
//    @FindBy(how = How.XPATH, using = "//material-input[@id='url--material-input']")
//
//    private WebElement Type_Url;
//
//    public void PolicyTestUrlwrite() {
//
//
//        Button typeUrl = new Button(Type_Url);
//
//
//        typeUrl.element.click();

    // typeUrl.element.sendKeys("https://www.apple.com");







    //type the website IP in the text box

//    public static final By PolicyTestIPType = By.cssSelector("#ipAddress--material-input");
//
//
//    @FindBy(how = How.CSS, using = "#ipAddress--material-input")
//
//    private WebElement Type_Ip;
//
//    public void PolicyTestIPWrite() {
//
//
//        Button typeIp = new Button(Type_Ip);
//
//
//        typeIp.element.click();
//
//        typeIp.element.sendKeys("100.20.3.71");





    //Clicks on GO button

    public static final By PolicyTestClickOnGo = By.cssSelector("#policyTestForm--submit-button--div-label");


    @FindBy(how = How.CSS, using = "#policyTestForm--submit-button--div-label")

    private WebElement Go_Click;

    public void ClickOnGoButton() {


        Button click_Go = new Button(Go_Click);


        click_Go.element.click();


    }


    //Checks if website is block.


    //public static final By PolicyCheckIfBlocked = By.xpath("//label[contains(text(),'Blocked')]");

    public static final By PolicyCheckIfBlocked = By.xpath("//twa-form[@id='policyTestResultForm']//twa-form-item[3]");



    // @FindBy(how = How.XPATH, using = "//label[contains(text(),'Blocked')]")
    @FindBy(how = How.XPATH, using = "//twa-form[@id='policyTestResultForm']//twa-form-item[3]")

    private WebElement Verified_Blocked;




}







