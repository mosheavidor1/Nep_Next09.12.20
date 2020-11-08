package Tests.LNE;

import Actions.LNEActions;
import Tests.GenericTest;
import Utils.ConfigHandling;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.Map;

public class VerifyFileCabinetOldInstallers extends GenericTest {

    private static final LNEActions lennyActions = GlobalTools.getLneActions();
    private String customerId;
    
    @Factory(dataProvider = "getData")
    public VerifyFileCabinetOldInstallers(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }

    @Test(groups = { "VerifyFileCabinetOldInstallers" } )
    public void verifyFileCabinetOldInstallers () {
    	
    	JLog.logger.info("Starting verifyFileCabinetOldInstallers test ...");

        String confJson = ConfigHandling.getDefaultConfiguration();


        //retrieve doc ids of installers
        Map<String, String> docIds = lennyActions.getInstallersDocIds(Long.valueOf(customerId), Integer.parseInt(data.get("Download timeout")));
        
        //init customer
        lennyActions.InitCustomerSettingsWithDuration(customerId, confJson,Integer.parseInt(data.get("From LNE up until response OK timeout")));

        //verify that the doc ids retrieved previously are now backup files
        lennyActions.VerifyInstallerBackup(Long.valueOf(customerId) ,docIds, Integer.parseInt(data.get("Download timeout")));

        //make sure new installers exist
        lennyActions.DownloadInstallerWithoutAdditions(Long.valueOf(customerId), Integer.parseInt(data.get("Download timeout")));

    }

}
