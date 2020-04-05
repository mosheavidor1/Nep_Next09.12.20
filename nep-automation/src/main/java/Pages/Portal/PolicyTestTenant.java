package Pages.Portal;

import Pages.GenericPage;
import WebElements.Button;
import WebElements.TextBox;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;



//Set the tenant ID on Policy Test

public class PolicyTestTenant extends GenericPage {


    public static final By PolicyTestUrlType = By.xpath("/html/body/div[1]/div[4]/div/div/div/main/div/div/div[1]/div/twa-input/div/material-input/div/div[1]/label/input");


    @FindBy(how = How.XPATH, using = "/html/body/div[1]/div[4]/div/div/div/main/div/div/div[1]/div/twa-input/div/material-input/div/div[1]/label/input")

    private WebElement tenant_id;

    public TextBox tenant;



    public PolicyTestTenant() {
        tenant = new TextBox(tenant_id);



    }



//clicks on the selected tenant

    public static final By ChooseTenant = By.cssSelector(".customer-name");


    @FindBy(how = How.CSS, using = ".customer-name")

    private WebElement Tenant_Id;



    public void ChooseTenantId() {


        Button TenantId = new Button(Tenant_Id);


        TenantId.element.click();


    }




}





