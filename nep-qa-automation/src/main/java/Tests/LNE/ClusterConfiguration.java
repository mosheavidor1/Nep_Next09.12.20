package Tests.LNE;

import Actions.LNEActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.JsonUtil;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ClusterConfiguration extends GenericTest {

    private LNEActions lneActions;

    @Factory(dataProvider = "getData")
    public ClusterConfiguration(Object dataToSet) {
        super(dataToSet);
    }

    @Test
    public void setClusterConfiguration() {
        lneActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"),general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));
        String confJson =data.get("Settings Json");
        long customerId = JsonUtil.GetCustomerIDFromSentConfiguration(confJson);
        String clusterName = "CraftyCluster";

        SimulatedAgentActions simulatedAgentInCluster = new SimulatedAgentActions(customerId, "1.2.3.4", "CraftyEp", "84-7B-EB-21-99-99","Windows 10");
        SimulatedAgentActions simulatedAgentNotInCluster = new SimulatedAgentActions(customerId, "1.2.3.5", "KoteEp", "85-7B-EB-21-99-99","Windows 10");


        lneActions.setClusterConfig(customerId,clusterName, confJson);

        Map<String,List<String>> assignments = new HashMap<>();
        List<String> epsNames = new LinkedList<>();
        epsNames.add(simulatedAgentInCluster.getName());
        assignments.put(clusterName,epsNames);

        lneActions.updateClusterMap( customerId,assignments);

        Map<String,Object> tagsToChange = new HashMap<>();
        tagsToChange.put("check_update_period",600);
        tagsToChange.put("report_period",600);
        String updatedClusterConfig = JsonUtil.ChangeTagsConfiguration(confJson, tagsToChange);

        lneActions.setClusterConfig(customerId,clusterName,updatedClusterConfig);


        String actionAgentInCluster = simulatedAgentInCluster.checkUpdates(simulatedAgentInCluster.getName(), "1.2.0.100", "0", 0, "1.1.1");
        String actionAgentNotInCluster = simulatedAgentNotInCluster.checkUpdates(simulatedAgentInCluster.getName(), "1.2.0.100", "0", 0, "1.1.1");

        org.testng.Assert.assertTrue(actionAgentInCluster.contains("switch"),"setClusterConfiguration test failed, ep added to cluster should have received configuration switch when checking update");
        org.testng.Assert.assertTrue(!actionAgentNotInCluster.contains("switch"),"setClusterConfiguration test failed, ep not in cluster should not have received configuration switch when checking update");


        String simulatedAgentInClusterConf = simulatedAgentInCluster.getConf(simulatedAgentInCluster.getAgentUuid());

        if(!JsonUtil.CompareKeyValue(simulatedAgentInClusterConf, "check_update_period", 600) ||
                !JsonUtil.CompareKeyValue(simulatedAgentInClusterConf, "report_period", 600)
        ){
            org.testng.Assert.fail("setClusterConfiguration test failed, ep added to cluster should have received configuration switch when checking update");
        }

        simulatedAgentNotInCluster.getConf(simulatedAgentNotInCluster.getAgentUuid());
        String simulatedAgentNotInClusterConf = simulatedAgentNotInCluster.getConf();
        if(JsonUtil.CompareKeyValue(simulatedAgentNotInClusterConf, "check_update_period", 600) ||
                JsonUtil.CompareKeyValue(simulatedAgentNotInClusterConf, "report_period", 600)
        ){
            org.testng.Assert.fail("setClusterConfiguration test failed, ep not added to cluster should have not contain this configuration");
        }

        assignments.put(clusterName,new LinkedList<>());
        lneActions.updateClusterMap(customerId,assignments);

        actionAgentInCluster = simulatedAgentInCluster.checkUpdates(simulatedAgentInCluster.getName(), "1.2.0.100", "0", 0, "1.1.1");
        org.testng.Assert.assertTrue(actionAgentInCluster.contains("switch"),"setClusterConfiguration test failed, ep added to cluster should have received configuration switch when checking update");


        simulatedAgentInClusterConf =simulatedAgentInCluster.getConf(simulatedAgentInCluster.getAgentUuid());

        if(JsonUtil.CompareKeyValue(simulatedAgentInClusterConf, "check_update_period", 600) ||
                JsonUtil.CompareKeyValue(simulatedAgentInClusterConf, "report_period", 600)
        ){
            org.testng.Assert.fail("setClusterConfiguration test failed, ep removed from cluster should have general configuration");
        }
    }
}
