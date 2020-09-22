package Tests.LNE;

import Actions.LNEActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.JsonUtil;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ClusterConfiguration extends GenericTest {

    private LNEActions lneActions;
    private String customerId;
    String clusterName = "CraftyCluster";
    String ep1Name = "CraftyEp";
    String ep2Name = "KoteEp";
    SimulatedAgentActions simulatedAgentInCluster;
    SimulatedAgentActions simulatedAgentNotInCluster;


    @Factory(dataProvider = "getData")
    public ClusterConfiguration(Object dataToSet) {
        super(dataToSet);
        customerId = general.get("Customer Id");
    }

    @Test(groups = { "ClusterConfiguration" } )
    public void setClusterConfiguration() {
        JLog.logger.info("Starting setClusterConfiguration test ...");
        lneActions = new LNEActions(PropertiesFile.readProperty("ClusterToTest"), general.get("LNE User Name"), general.get("LNE Password"), Integer.parseInt(general.get("LNE SSH port")));

        String confJson = data.get("Settings Json");

        //delete simulated agents
        Map<String, List<String>> assignments = new HashMap<>();

        simulatedAgentInCluster = new SimulatedAgentActions();
        simulatedAgentInCluster.register(customerId, "1.2.3.4", ep1Name, "84-7B-EB-21-99-99", "Windows 10");

        simulatedAgentNotInCluster = new SimulatedAgentActions();
        simulatedAgentNotInCluster.register(customerId, "1.2.3.5", ep2Name, "85-7B-EB-21-99-99", "Windows 10");
//        JLog.logger.info("sleeping 60 seconds until finish registering");
//        Thread.sleep(60000);

        lneActions.setClusterConfig(customerId, clusterName, confJson);


        List<String> epsNames = new LinkedList<>();
        epsNames.add(simulatedAgentInCluster.getName());
        assignments.put(clusterName, epsNames);

        lneActions.updateClusterMap(Long.valueOf(customerId), assignments);


        Map<String, Object> tagsToChange = new HashMap<>();
        tagsToChange.put("check_update_period", 666);
        tagsToChange.put("report_period", 666);
        String updatedClusterConfig = JsonUtil.ChangeTagsConfiguration(confJson, tagsToChange);

        lneActions.setClusterConfig(customerId, clusterName, updatedClusterConfig);


        String actionAgentInCluster = simulatedAgentInCluster.sendCheckUpdatesAndGetAction(simulatedAgentInCluster.getName(), "9.9.9.999", 1, 0, "1.1.1", customerId);
        String actionAgentNotInCluster = simulatedAgentNotInCluster.sendCheckUpdatesAndGetAction(simulatedAgentNotInCluster.getName(), "9.9.9.999", 1, 0, "1.1.1", customerId);


        org.testng.Assert.assertTrue(actionAgentInCluster.contains("switch"), "setClusterConfiguration test failed, ep added to cluster should have received configuration switch when checking update, action: " + actionAgentInCluster);
        org.testng.Assert.assertTrue(!actionAgentNotInCluster.contains("switch"), "setClusterConfiguration test failed, ep not in cluster should not have received configuration switch when checking update action: " + actionAgentNotInCluster);


        String simulatedAgentInClusterConf = simulatedAgentInCluster.getConf(customerId);

        if (!JsonUtil.CompareKeyValue(simulatedAgentInClusterConf, "check_update_period", 666) ||
                !JsonUtil.CompareKeyValue(simulatedAgentInClusterConf, "report_period", 666)
        ) {
            org.testng.Assert.fail("setClusterConfiguration test failed, ep added to cluster should have received configuration switch when checking update");
        }

        simulatedAgentNotInCluster.getConf(customerId);
        String simulatedAgentNotInClusterConf = simulatedAgentNotInCluster.getConf(customerId);
        if (JsonUtil.CompareKeyValue(simulatedAgentNotInClusterConf, "check_update_period", 666) ||
                JsonUtil.CompareKeyValue(simulatedAgentNotInClusterConf, "report_period", 666)
        ) {
            org.testng.Assert.fail("setClusterConfiguration test failed, ep not added to cluster should have not contain this configuration");
        }

        assignments.put(clusterName, new LinkedList<>());
        lneActions.updateClusterMap(Long.valueOf(customerId), assignments);

        actionAgentInCluster = simulatedAgentInCluster.sendCheckUpdatesAndGetAction(simulatedAgentInCluster.getName(), "9.9.9.999", 0, 0, "1.1.1", customerId);
        org.testng.Assert.assertTrue(actionAgentInCluster.contains("switch"), "setClusterConfiguration test failed, ep added to cluster should have received configuration switch when checking update");


        simulatedAgentInClusterConf = simulatedAgentInCluster.getConf(customerId);

        if (JsonUtil.CompareKeyValue(simulatedAgentInClusterConf, "check_update_period", 666) ||
                JsonUtil.CompareKeyValue(simulatedAgentInClusterConf, "report_period", 666)
        ) {
            org.testng.Assert.fail("setClusterConfiguration test failed, ep removed from cluster should have general configuration");
        }


}


    @AfterTest
    public void close(){

        //delete if exist and clean cluster
        try {
            lneActions.updateClusterMap(Long.valueOf(customerId), new HashMap<>());
            lneActions.deleteWithoutVerify(customerId, ep1Name);
            simulatedAgentInCluster.sendCheckUpdatesAndGetResponse(simulatedAgentInCluster.getName(), "1.2.0.100", 0, 0, "1.1.1", customerId);
            lneActions.deleteWithoutVerify(customerId, ep2Name);
            simulatedAgentNotInCluster.sendCheckUpdatesAndGetResponse(simulatedAgentNotInCluster.getName(), "1.2.0.100", 0, 0, "1.1.1", customerId);

        }catch (Exception e) {

        }finally {
            JLog.logger.info("Closing...");
            if(lneActions!=null){
                lneActions.Close();
            }
        }

    }
}
