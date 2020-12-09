package Pages.Portal;

import Pages.GenericPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;


    public class CentComNewClusterPage extends GenericPage {


        private final static String ClusterNameXpath = "  ";

        public WebElement GetClusterName(String ClusterName){
            String xpath = ClusterNameXpath.replace("XXX", ClusterName);
            return driver.findElement(By.xpath(xpath));
        }


    }








