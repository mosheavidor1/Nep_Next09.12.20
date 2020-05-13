package Pages.Portal;

import Pages.GenericPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

//BLOCK MESSAGE policy test

    public class PolicyTestBlockMessage extends GenericPage {

      //public static final String  BlockMessage = "//label[contains(text(),'Blocked')]";

        public static final By BlockMessage = By.xpath("//label[contains(text(),'Blocked')]");

       // public static final By MessageB = By.xpath(BlockMessage);



        //Trustwave image
       // @FindBy(how= How.XPATH,using=BlockMessage)
      @FindBy(how= How.XPATH,using="//label[contains(text(),'Blocked')]")
        public WebElement BlockMessage_element;







    }


