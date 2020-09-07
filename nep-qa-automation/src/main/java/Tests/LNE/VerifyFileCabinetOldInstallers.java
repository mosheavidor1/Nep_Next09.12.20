package Tests.LNE;

import Actions.LNEActions;
import Tests.GenericTest;
import Utils.JsonUtil;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.Map;

public class VerifyFileCabinetOldInstallers extends GenericTest {

    private LNEActions manager;
    
    @Factory(dataProvider = "getData")
    public VerifyFileCabinetOldInstallers(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "VerifyFileCabinetOldInstallers" } )
    public void verifyFileCabinetOldInstallers () {

        manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
        String confJson =data.get("Settings Json");
        long customerId = JsonUtil.GetCustomerIDFromSentConfiguration(confJson);

        //retrieve doc ids of installers
        Map<String, String> docIds = manager.getInstallersDocIds(customerId, Integer.parseInt(data.get("Download timeout")));
        
        //init customer
        manager.InitCustomerSettings(confJson,Integer.parseInt(data.get("From LNE up until response OK timeout")));

        //verify that the doc ids retrieved previously are now backup files
        manager.VerifyInstallerBackup(customerId ,docIds, Integer.parseInt(data.get("Download timeout")));

        //make sure new installers exist
        manager.DownloadInstallerWithoutAdditions(customerId,Integer.parseInt(data.get("Download timeout")));

    }
}
