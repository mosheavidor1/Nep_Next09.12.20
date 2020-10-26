package Actions;

import static io.restassured.RestAssured.given;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * This class is used to perform actions on DS-Mgmt by calling functions exposed in the nepa-dserver-api using the centcom client.
 * 
 * @author RSalmon
 *
 */
public class DsMgmtActions {
	
	private static ObjectMapper objectMapper = new ObjectMapper();
	private static RequestSpecification requestSpecification;
	
	public static final String centcomClientUrlFormat = "http://%s:9091/nep-centcom-client/";
	
	public DsMgmtActions(String hostIp){
        try {
            RestAssured.baseURI = String.format(centcomClientUrlFormat, hostIp);
            requestSpecification = new RequestSpecBuilder().setBaseUri(String.format(centcomClientUrlFormat, hostIp)).build();

        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not set RestAssured.baseURI for machine: " + hostIp  + "\n" + e.toString());
        }

    }
	
	public static void InitCustomerSettings(String customerId, String configJson) {

        try {
            int response = DsMgmtActions.sendInitCustomerSettings(customerId, configJson);
            if (response == 200 ) {
                JLog.logger.info("Success. LNE InitCustomerSettings response: " + response);
                return;
            }
            JLog.logger.error("LNE InitCustomerSettings response: " + response );
            org.testng.Assert.fail("Could not init customer settings. LNE response status code received is: " + response);
        }
        catch (Exception e){
            JLog.logger.error("Could not init customer setting.", e);
            org.testng.Assert.fail("Could not init customer settings. Json sent:  {}", e );
        }

    }
	
   public static int sendInitCustomerSettings (String customerId, String configJson ) {
	   
	   if (GlobalTools.isPortalEnv()) {
		   org.testng.Assert.fail("No support for provisioning customer using the centcom client not in Lenny");
	   }

        String body = buildJsonBody( Arrays.asList("customerId", "configuration"), Arrays.asList(customerId, configJson));

        Response r = given().spec(requestSpecification)
                .contentType("application/json")
                .body(body)
                .when()
                .post("initCustomerSettings");

        return r.getStatusCode();

    }

    public static void setCustomerConfig(String customerId, String configuration) {

        String body = buildJsonBody( Arrays.asList("customerId", "configuration"), Arrays.asList(customerId, configuration));

        try {
            Response r = given().spec(requestSpecification)
                    .contentType("application/json")
                    .body(body)
                    .when()
                    .post("setConfig");

            int response = r.getStatusCode();

            if (response == 200) {
                JLog.logger.info("setCustomerConfig response: " + response);
            }
            else
                org.testng.Assert.fail("Could not set customer configuration. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not set customer config.", e);
        }

    }

    public static void setClusterConfig(String customerId, String clusterName, String configJson) {

        String body = buildJsonBody( Arrays.asList("customerId", "clusterName", "config"), Arrays.asList(customerId, clusterName, configJson));

        try {
            Response r = given().spec(requestSpecification)
                    .contentType("application/json")
                    .body(body)
                    .when()
                    .post("setClusterConfig");

            int response = r.getStatusCode();

            if (response == 200) {
                JLog.logger.info("setClusterConfig response: " + response);
            }
            else
                org.testng.Assert.fail("Could not set cluster configuration. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not set cluster config.", e);
        }

    }

    public static void setEndpointConfig(String customerId, String endpointName, String configuration) {

        String body = buildJsonBody( Arrays.asList("customerId", "name", "configuration"), Arrays.asList(customerId, endpointName, configuration));

        try {
            Response r = given().spec(requestSpecification)
                    .contentType("application/json")
                    .body(body)
                    .when()
                    .post("setConfig");

            int response = r.getStatusCode();

            if (response == 200) {
                JLog.logger.info("setEndpointConfig response: " + response);
            }
            else
                org.testng.Assert.fail("Could not set endpoint configuration. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not set endpoint config.", e);
        }

    }

    public static void updateClusterMap(Long customerId, Map<String, List<String>> assignments) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("customerId",customerId);
        JSONArray assignmentsArr = new JSONArray();
        for(String clusterName : assignments.keySet()){
            List<String> eps = assignments.get(clusterName);
            JSONObject assignmentObject = new JSONObject();
            assignmentObject.put("clusterName",clusterName);
            assignmentObject.put("endpointName",eps);
            assignmentsArr.put(assignmentObject);
        }
        jsonObject.put("assignments",assignmentsArr);
        try {
            Response r = given().spec(requestSpecification)
                    .contentType("application/json").
                            body(jsonObject.toString()).
                            when().
                            post("updateClusterMap");

            int response = r.getStatusCode();

            if (response == 200) {
                JLog.logger.info("Success. LNE updateClusterMap response: " + response);
            }
            else
                org.testng.Assert.fail("Could not update Cluster Map configuration. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not update Cluster Map configuration.", e);
        }

    }
    public static void SetCustomerConfiguration (String customerId, String configuration) {

        String body = buildJsonBody( Arrays.asList("customerId", "configuration"), Arrays.asList(customerId, configuration));

        try {
            Response r = given().spec(requestSpecification)
                    .contentType("application/json")
                    .body(body)
                    .when()
                    .post("setConfig");

            int response = r.getStatusCode();

            if (response == 200) {
                JLog.logger.info("SetCustomerConfiguration response: " + response);
            }
            else
                org.testng.Assert.fail("Could not set customer configuration. LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not set customer configuration.", e);
        }

    }

    public static void revokeEpConfiguration (String customerId, String epName) {
    	JLog.logger.info("Sending revoke configuration request for customer {}, ep {}", customerId, epName);

        String body = buildJsonBody( Arrays.asList("customerId", "epName"), Arrays.asList(customerId, epName));

        try {
            Response r = given().spec(requestSpecification)
                    .contentType("application/json")
                    .body(body)
                    .when()
                    .post("revokeEpConfiguration");

            int response = r.getStatusCode();

            if (response == 200)
                JLog.logger.info("Success. LNE revokeEpConfiguration response: " + response);
            else
                org.testng.Assert.fail("Could not revokeEpConfiguration  . LNE response status code received is: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not revokeEpConfiguration.", e);
        }

    }

    public static void revoke(String customerId, String epName) {
    	JLog.logger.info("Sending revoke request for customer {}, ep {}", customerId, epName);

        String body = buildJsonBody( Arrays.asList("customerId", "epName"), Arrays.asList(customerId, epName));

        try {
            Response r = given().spec(requestSpecification)
                    .contentType("application/json")
                    .body(body)
                    .when()
                    .post("revoke");

            int response = r.getStatusCode();

            if (response == 200)
                JLog.logger.info("Success. LNE revoke response: " + response);
            else
                org.testng.Assert.fail("Failure. LNE revoke failed with response status code: " + response);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Revoke action failed.", e);
        }

    }

    public static void deleteWithoutVerify(String customerId, String epName) {

        JLog.logger.info("Going to send delete endpoint request to Centcom client, customer: {}, ep: {}", customerId, epName);

        sendDelete(customerId, epName);

    }

    public static void deleteAndVerifyResponse(String customerId, String epName) {

    	JLog.logger.info("Sending delete request for customer {}, ep {}", customerId, epName);
        int response = sendDelete(customerId, epName);

        if (response == 200)
            JLog.logger.info("Success. LNE delete response: " + response);
        else
            org.testng.Assert.fail("Failure. LNE delete failed with response status code: " + response);

    }

    private static int sendDelete(String customerId, String epName) {

        String body = buildJsonBody( Arrays.asList("customerId", "epName"), Arrays.asList(customerId, epName));

        try {
            Response r = given().spec(requestSpecification)
                    .contentType("application/json")
                    .body(body)
                    .when()
                    .post("delete");

            return r.getStatusCode();


        }
        catch (Exception e) {
            org.testng.Assert.fail("Delete action failed.", e);
            return 500;
        }
    }
    
    private static String buildJsonBody(List<String> paramNames, List<String> paramValues) {

        JsonNode jsonNode = objectMapper.createObjectNode();
        for (int i = 0; i < paramNames.size(); i++) {
            ((ObjectNode) jsonNode).put(paramNames.get(i), paramValues.get(i));
        }

        return jsonNode.toPrettyString();
    }

}
