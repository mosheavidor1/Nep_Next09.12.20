package Tests.NEP.LNE;

import Actions.LNEActions;
import Tests.GenericTest;
import Utils.PropertiesFile.PropertiesFile;
import com.google.gson.JsonArray;
import org.json.JSONArray;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.io.IOException;


public class ChangeCustomerConfiguration extends GenericTest {

    private LNEActions action;

    @Factory(dataProvider = "getData")
    public ChangeCustomerConfiguration(Object dataToSet) {
        super(dataToSet);
    }

    @Test()
    public void ChangeCustomerConfigurationAndVerify()  {
        action = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));

        String confJson =data.get("Settings Json");

        action.CompareConfigurationToEPConfiguration(confJson);

        action.SetCustomerConfiguration(confJson);
        action.StopEPService(Integer.parseInt(general.get("EP Service Timeout")));
        action.StartEPService(Integer.parseInt(general.get("EP Service Timeout")));

        action.CompareConfigurationToEPConfiguration(confJson);

    }

}
