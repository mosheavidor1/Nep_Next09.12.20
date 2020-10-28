package Tests.LNE;

import Actions.LNEActions;
import Tests.GenericTest;
import Utils.ConfigHandling;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class InitCustomer extends GenericTest {

	private static final LNEActions lennyActions = GlobalTools.getLneActions();
    private String customerId;

    @Factory(dataProvider = "getData")
    public InitCustomer(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }

    @Test(groups = { "InitCustomer" } )
    public void initCustomerAndDownloadInstaller () {
    	JLog.logger.info("Starting InitCustomerAndDownloadInstaller test ...");

        String confJson = ConfigHandling.getDefaultConfiguration();
        lennyActions.DeleteCurrentInstallerFromLNE(Long.valueOf(customerId));
        lennyActions.InitCustomerSettingsWithDuration(customerId, confJson, Integer.parseInt(data.get("From LNE up until response OK timeout")));
        lennyActions.DownloadInstallerIncludingRequisites(Long.valueOf(customerId) , Integer.parseInt(data.get("Download timeout")));

    }


}
