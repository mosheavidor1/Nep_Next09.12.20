package Tests.LNE;

import Actions.DsMgmtActions;
import Actions.SimulatedAgentActions;
import Tests.GenericTest;
import Utils.ConfigHandling;
import Utils.JsonUtil;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ClusterConfiguration extends GenericTest {

    private String customerId;
    String clusterName = "CraftyCluster";
    String ep1Name = "CraftyEp";
    String ep2Name = "KoteEp";
    SimulatedAgentActions simulatedAgentInCluster;
    SimulatedAgentActions simulatedAgentNotInCluster;


    @Factory(dataProvider = "getData")
    public ClusterConfiguration(Object dataToSet) {
        super(dataToSet);
        customerId = getGeneralData().get("Customer Id");
    }

    @Test(groups = { "ClusterConfiguration" }, priority = 20 )//so that it will run after the send logs test, im portal env
    public void setClusterConfiguration() {
        JLog.logger.info("Starting setClusterConfiguration test ...");

        String confJson = ConfigHandling.getDefaultConfiguration();       
        Map<String, List<String>> assignments = new HashMap<>();

        simulatedAgentInCluster = new SimulatedAgentActions(getGeneralData().get("DS Name"), customerId);
        simulatedAgentInCluster.register(customerId, "1.2.3.4", ep1Name, "84-7B-EB-21-99-99", "Windows 10");

        simulatedAgentNotInCluster = new SimulatedAgentActions(getGeneralData().get("DS Name"), customerId);
        simulatedAgentNotInCluster.register(customerId, "1.2.3.5", ep2Name, "85-7B-EB-21-99-99", "Windows 10");

        DsMgmtActions.setClusterConfig(customerId, clusterName, confJson);


        List<String> epsNames = new LinkedList<>();
        epsNames.add(simulatedAgentInCluster.getName());
        assignments.put(clusterName, epsNames);

        DsMgmtActions.updateClusterMap(Long.valueOf(customerId), assignments);


        Map<String, Object> tagsToChange = new HashMap<>();
        tagsToChange.put("check_update_period", 666);
        tagsToChange.put("report_period", 666);
        String updatedClusterConfig = JsonUtil.ChangeTagsConfiguration(confJson, tagsToChange);

        DsMgmtActions.setClusterConfig(customerId, clusterName, updatedClusterConfig);


        String actionAgentInCluster = simulatedAgentInCluster.sendCheckUpdatesAndGetAction(simulatedAgentInCluster.getName(), GlobalTools.currentBinaryBuild, 1, 0, GlobalTools.currentSchemaVersion, customerId);
        String actionAgentNotInCluster = simulatedAgentNotInCluster.sendCheckUpdatesAndGetAction(simulatedAgentNotInCluster.getName(), GlobalTools.currentBinaryBuild, 1, 0, GlobalTools.currentSchemaVersion, customerId);


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
        DsMgmtActions.updateClusterMap(Long.valueOf(customerId), assignments);

        actionAgentInCluster = simulatedAgentInCluster.sendCheckUpdatesAndGetAction(simulatedAgentInCluster.getName(), GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
        org.testng.Assert.assertTrue(actionAgentInCluster.contains("switch"), "setClusterConfiguration test failed, ep added to cluster should have received configuration switch when checking update");


        simulatedAgentInClusterConf = simulatedAgentInCluster.getConf(customerId);

        if (JsonUtil.CompareKeyValue(simulatedAgentInClusterConf, "check_update_period", 666) ||
                JsonUtil.CompareKeyValue(simulatedAgentInClusterConf, "report_period", 666)) {
            org.testng.Assert.fail("setClusterConfiguration test failed, ep removed from cluster should have general configuration");
            return;
        }
        JLog.logger.info("Finished setClusterConfiguration successfully.");


}


    @AfterMethod
    public void close(){

        //delete if exist and clean cluster
    	DsMgmtActions.updateClusterMap(Long.valueOf(customerId), new HashMap<>());
    	DsMgmtActions.deleteWithoutVerify(customerId, ep1Name);
        simulatedAgentInCluster.sendCheckUpdatesAndGetResponse(simulatedAgentInCluster.getName(), GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
        DsMgmtActions.deleteWithoutVerify(customerId, ep2Name);
        simulatedAgentNotInCluster.sendCheckUpdatesAndGetResponse(simulatedAgentNotInCluster.getName(), GlobalTools.currentBinaryBuild, 0, 0, GlobalTools.currentSchemaVersion, customerId);
    }
}
