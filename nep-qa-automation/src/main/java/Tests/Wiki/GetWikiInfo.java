package Tests.Wiki;

import Actions.WikiActions;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.Test;


public class GetWikiInfo {


    @Test()
    public void GetAllLBAddressess() throws Exception {
        WikiActions action = new WikiActions();
        action.LaunchApplication("Chrome");
        action.SetApplicationUrl(WikiActions.SWGCloudAWSProductionStatus);

        action.Login(PropertiesFile.readProperty("Wiki.UserName"), PropertiesFile.readProperty("Wiki.Password"));

        action.GetTableData();
        action.SetApplicationUrl(WikiActions.SWGCloudAWSTestStatus);

        action.GetTableData();

        action.CloseApplication();

        action.CheckAllClustersWhereRead();
        String[] arr = {"us-east-1", "eu-central-1", "eu-west-2", "ap-southeast-1", "ap-northeast-1", "ap-southeast-2" ,"qa", "inc", "stg"};

        for (int i = 0; i < WikiActions.productionClustersNames.length; i++) {
            System.out.println(WikiActions.productionClustersNames[i] + ":\n" + PropertiesFile.readProperty(WikiActions.productionClustersNames[i]));
        }
        for (int i = 0; i < WikiActions.testClustersNames.length; i++) {
            System.out.println(WikiActions.testClustersNames[i] + ":\n" + PropertiesFile.readProperty(WikiActions.testClustersNames[i]));
        }

    }
}
