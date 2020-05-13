package Actions;

import Applications.SeleniumBrowser;
import Pages.Wiki.LoginPage;
import Tests.Wiki.Cluster;
import Utils.PropertiesFile.PropertiesFile;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class WikiActions extends TestActions {

    public static final String SWGCloudAWSTestStatus =  "https://wiki.trustwave.com/display/GAS/SWG+Cloud+AWS+TW-Test+Status";
    public static final String SWGCloudAWSProductionStatus =  "https://wiki.trustwave.com/display/GAS/SWG+Cloud+AWS+Production+Status";
    public static final String[] productionClustersNames = {"us-east-1", "eu-central-1", "eu-west-2", "ap-southeast-1", "ap-northeast-1", "ap-southeast-2"};
    public static final String[] testClustersNames = {"qa", "inc", "stg"};


    private static final String Environment="Environment";
    private static final String Region="Region";
    private static final String ClusterID ="Cluster ID (10.127.###)";
    private static final String AMIBuildNumber="AMI Build #";
    private static final String PoweredUp="Powered Up";
    private static final String PolicyTest="PolicyTest";
    private static final String DNS="DNS";
    private static final String LBPublicIP="Cluster Manager (CM) Public IPs";
    private static final String VPNPublicIP="VPN Public IPs";
    private static final String Notes="Notes";


    //private String[] columnsArr= {Region ,ClusterID, AMIBuildNumber,PoweredUp ,PolicyTest,DNS,LBPublicIP,VPNPublicIP,Notes};
    private List<Cluster> clusters = new ArrayList<Cluster>();
    private List<Cluster> DNSclusters = new ArrayList<Cluster>();



    public WikiActions() {
        super();
    }


    public void Login(String userName, String password) throws Exception {
        LoginPage login = new LoginPage();

        login.WaitUntilTitleAppearAndPageLoad("Log In - Trustwave Confluence");
        login.UserName.SetText(userName);
        login.Password.SetText(password);
        login.LoginButton.click();


    }

    public void GetTableData() throws IOException {
        WebElement table= SeleniumBrowser.GetDriver().findElement(By.id("main-content"));
        List<WebElement> allRows = table.findElements(By.tagName("tr"));
        List<WebElement> header = allRows.get(0).findElements(By.tagName("div"));

        Hashtable<String, Integer> columnNumber = new Hashtable<String,Integer> ();

        for(int i=0;i<header.size();i++) {
            //System.out.println(header.get(i).getText());
            columnNumber.put(header.get(i).getText().trim(), i );
        }

        for (int i=1;i<allRows.size();i++) {
            //*****new Row*****
            List<WebElement> cells = allRows.get(i).findElements(By.tagName("td"));
            Cluster currentCluster = new Cluster();
            currentCluster.Region = cells.get(columnNumber.get(Region)).getText().trim();
            //if empty row skip it
            if(currentCluster.Region.isEmpty())
                continue;
            if(columnNumber.containsKey(Environment))
                currentCluster.Environment = cells.get(columnNumber.get(Environment)).getText().trim();
            else
                currentCluster.Environment="";
            currentCluster.ClusterID = cells.get(columnNumber.get(ClusterID)).getText().trim();
            currentCluster.AMIBuildNumber = cells.get(columnNumber.get(AMIBuildNumber)).getText().trim();
            currentCluster.PoweredUp = IsGreenImage(cells.get(columnNumber.get(PoweredUp)));
            currentCluster.PolicyTest = IsGreenImage(cells.get(columnNumber.get(PolicyTest)));
            currentCluster.DNS = IsGreenImage(cells.get(columnNumber.get(DNS)));
            currentCluster.LBPublicIPs = cells.get(columnNumber.get(LBPublicIP)).getText().trim().split("\n");
            currentCluster.VPNPublicIPs = cells.get(columnNumber.get(VPNPublicIP)).getText().trim().split("\n");
            currentCluster.Notes = cells.get(columnNumber.get(Notes)).getText().trim();
            clusters.add(currentCluster);
            if(currentCluster.DNS && currentCluster.PoweredUp ) {
                DNSclusters.add(currentCluster);
                if(currentCluster.Environment.isEmpty())
                    PropertiesFile.writeProperty(currentCluster.Region,currentCluster.LBPublicIPs[0]);
                else
                    PropertiesFile.writeProperty(currentCluster.Environment,currentCluster.LBPublicIPs[0]);

            }
        }

        String comment;
        if(clusters.get(clusters.size()-1).Environment.isEmpty())
            comment = "Read production clusters data successfully";
        else
            comment = "Read test clusters data successfully";
        PropertiesFile.saveFile(comment);

    }

    public void CheckAllClustersWhereRead(){
        String errorMessage ="";
        boolean found;
        for(int i=0; i<productionClustersNames.length;i++) {
            found = false;
            for (int j = 0; j < DNSclusters.size(); j++)
                if (DNSclusters.get(j).Region.compareToIgnoreCase(productionClustersNames[i]) == 0) {
                    found = true;
                    break;
                }
            if (!found)
                errorMessage = errorMessage + "Could not find powered up LB DNS for cluster: " + productionClustersNames[i] + "\n";
        }

        for(int i=0; i<testClustersNames.length;i++) {
            found = false;
            for (int j = 0; j < DNSclusters.size(); j++)
                if (DNSclusters.get(j).Environment.compareToIgnoreCase(testClustersNames[i]) == 0) {
                    found = true;
                    break;
                }
            if (!found)
                errorMessage = errorMessage + "Could not find powered up LB DNS for cluster: " + testClustersNames[i] + "\n";
        }


        if( ! errorMessage.isEmpty())
            org.testng.Assert.fail("\n" + errorMessage);

    }




    private boolean IsGreenImage(WebElement cell){

        WebElement image = cell.findElement(By.tagName("img"));
        String imageName = image.getAttribute("data-emoticon-name").toString().trim();
        if(imageName.compareToIgnoreCase("tick") == 0)
            return true;
        return false;

    }

}
