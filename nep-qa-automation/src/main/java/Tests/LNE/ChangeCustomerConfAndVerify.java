package Tests.LNE;

import Actions.AgentActionsFactory;
import Actions.BaseAgentActions;
import Actions.LNEActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


public class ChangeCustomerConfAndVerify extends GenericTest {

    private LNEActions lennyActions;
    private String customerId;
    
    private BaseAgentActions agent;
    private String confJson;
    
    private boolean confWasSet = false;

    @Factory(dataProvider = "getData")
    public ChangeCustomerConfAndVerify(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
    }

    @Test(groups = { "ChangeCustomerConfAndVerify" } )
    public void ChangeCustomerConfAndVerify()  {

        JLog.logger.info("Starting ChangeCustomerConfAndVerify...");
        
        if (!confWasSet) { //Configuration will be set only once, and not for every EP

	        lennyActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
	        confJson = data.get("Settings Json");
	        lennyActions.SetCustomerConfiguration(customerId, confJson);
	        
	        confWasSet = true;
        }
        
        agent = AgentActionsFactory.getAgentActions(data.get("EP_Type_1"), data.get("EP_HostName_1"), data.get("EP_UserName_1"), data.get("EP_Password_1"));
        //TODO Change it!!!
        agent.stopEPService(Integer.parseInt(general.get("EP Service Timeout")));
        agent.startEPService(Integer.parseInt(general.get("EP Service Timeout")));
        agent.compareConfigurationToEPConfiguration(true, confJson);

    }

    @AfterMethod
    public void Close(){
        if(lennyActions!=null){
            lennyActions.Close();
        }
        if (agent != null) {
        	agent.close();
        }
    }


}
