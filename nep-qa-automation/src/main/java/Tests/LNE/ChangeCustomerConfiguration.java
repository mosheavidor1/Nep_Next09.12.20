package Tests.LNE;

import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class ChangeCustomerConfiguration extends GenericTest {

    private LNEActions manager;

    @Factory(dataProvider = "getData")
    public ChangeCustomerConfiguration(Object dataToSet) {
        super(dataToSet);
    }

    @Test(groups = { "ChangeConfiguration" } )
    public void ChangeCustomerConfiguration()  {

        JLog.logger.info("Opening...");

        manager = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
        String confJson =data.get("Settings Json");
        manager.SetCustomerConfiguration(confJson);

    }

    @AfterMethod
    public void Close(){
        JLog.logger.info("Closing...");
        if(manager!=null){
            manager.Close();
        }
    }


}
