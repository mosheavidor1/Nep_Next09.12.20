package Tests.LNE;

import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class InitCustomer extends GenericTest {

    private LNEActions lennyActions;
    private String customerId;

    @Factory(dataProvider = "getData")
    public InitCustomer(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
    }

    @Test(groups = { "InitCustomer" } )
    public void initCustomerAndDownloadInstaller () {
    	JLog.logger.info("Starting InitCustomerAndDownloadInstaller test ...");

        lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
        String confJson = data.get("Settings Json");
        lennyActions.DeleteCurrentInstallerFromLNE(Long.valueOf(customerId));
        lennyActions.InitCustomerSettingsWithDuration(customerId, confJson, Integer.parseInt(data.get("From LNE up until response OK timeout")));
        lennyActions.DownloadInstallerIncludingRequisites(Long.valueOf(customerId) , Integer.parseInt(data.get("Download timeout")));

    }

    @AfterMethod
    public void Close(){
        if(lennyActions!=null){
            lennyActions.Close();
        }
    }

}
