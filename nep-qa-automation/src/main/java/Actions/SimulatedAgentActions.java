package Actions;

import DataModel.UpdateEpDetails;
import io.restassured.authentication.CertificateAuthSettings;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.exception.JsonPathException;
import io.restassured.response.Response;


import static io.restassured.RestAssured.*;

public class SimulatedAgentActions {

	public static final String DS_URL = "https://%s:443/";
	public static final String REGISTER = "register";
	public static final String UPDATE_EP_INFO = "update-endpoint-details";
	public static final String ENDPOINT_ID = "endpoint_id";
	public static final String ENDPOINT_NAME = "endpoint_name";
	public static final String CHECK_UPDATES = "check-updates/%s/bin-ver/%s/conf-ver/%s/reporting-status/%s/local-name/%s/schema-ver/%s/";
	public static final String GET_CONF = "conf/%s/";
	public static final String XSSL_Client_HEADER = "X-SSL-Client-S-DN";
	public static final String XSSL_Client_HEADER_VALUE = "d=ggg,CN=1-1-%s,f=ddd"; //DS on Lenny ignores the header excepts the customer id 

	public static ObjectMapper objectMapper = new ObjectMapper();
	RequestSpecification requestSpecification;

	private String agentUuid;
	private String lastConf;
	private String name;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	class RegisterBody{

		@JsonProperty("Name")
		private String name;
		@JsonProperty("OsTypeAndVersion")
		private String os;
		@JsonProperty("CustomerId")
		private String customerId;
		@JsonProperty("EndpointIp")
		private String ip;
		@JsonProperty("MacAddress")
		private String macAddress;

		public RegisterBody(String customerId, String ip, String name, String macAddress, String os) {
			this.customerId = customerId;
			this.ip = ip;
			this.name = name;
			this.macAddress = macAddress;
			this.os = os;

		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getOs() {
			return os;
		}

		public void setOs(String os) {
			this.os = os;
		}

		public String getCustomerId() {
			return customerId;
		}

		public void setCustomerId(String customerId) {
			this.customerId = customerId;
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public String getMacAddress() {
			return macAddress;
		}

		public void setMacAddress(String macAddress) {
			this.macAddress = macAddress;
		}

	}

	public SimulatedAgentActions(String dsName, String customerId) {
		try {
			JLog.logger.info("SimulatedAgentActions, DS URL: {}", String.format(DS_URL, dsName));
			requestSpecification = new RequestSpecBuilder().setBaseUri(String.format(DS_URL, dsName)).build();
			String path2p12 = PropertiesFile.getManagerDownloadFolder() + "/" + GlobalTools.getClusterToTest() + "/endpoint-111-" + customerId + ".111.p12";
			String path2jks = PropertiesFile.getManagerDownloadFolder() + "/" + GlobalTools.getClusterToTest() + "/ca.jks";
			RestAssured.authentication =
					RestAssured.certificate(
							path2jks,
							"trustwave",
							path2p12,
							"trustwave",
							CertificateAuthSettings.certAuthSettings());
		}
		catch (Exception e) {
			org.testng.Assert.fail("Could not set RestAssured.baseURI", e);
		}
	}
	
	/**
	 *  Sends check updates post request to Lenny with the given params, without verifying response 200
	 *  This call must be after register, otherwise the uuid will be null
	 * 
	 * @param epName
	 * @param binVersion
	 * @param confVersion
	 * @param reportingStatus
	 * @param schemaVersion
	 * @param customerId
	 * 
	 */
	public void sendCheckUpdatesWithoutVerify(String epName, String binVersion, int confVersion, int reportingStatus, String schemaVersion, String customerId) {
		sendCheckUpdates(getAgentUuid(), epName, binVersion, confVersion, reportingStatus, schemaVersion, customerId);
	}
	
	public void sendCheckUpdatesWithoutVerify(String uuid, String epName, String binVersion, int confVersion, int reportingStatus, String schemaVersion, String customerId) {
		sendCheckUpdates(uuid, epName, binVersion, confVersion, reportingStatus, schemaVersion, customerId);
	}
	
	/**
	 * Sends check updates post request to Lenny with the given params and expects to 200 OK.
	 * Returns response body as is.	 * 
	 * 
	 * @param epName
	 * @param binVersion
	 * @param confVersion
	 * @param reportingStatus
	 * @param schemaVersion
	 * @return
	 */
	public String sendCheckUpdatesAndGetResponse(String epName, String binVersion, int confVersion, int reportingStatus, String schemaVersion, String customerId) {
		
		Response response = sendCheckUpdates(getAgentUuid(), epName, binVersion, confVersion, reportingStatus, schemaVersion, customerId);
		
		JLog.logger.info("Check updates succeeded, got response: '{}'", response);

		String responseStr = response.then()
		.assertThat()
		.statusCode(HttpStatus.SC_OK)
		.extract().response().body().asString();
		
		return responseStr;
	}
	
	/**
	 * Sends check updates post request to Lenny with the given params, and expects to 200 OK.
	 * Returns the action from the response body. 
	 * 
	 * @param epName
	 * @param binVersion
	 * @param confVersion
	 * @param reportingStatus
	 * @param schemaVersion
	 * @return
	 */
	public String sendCheckUpdatesAndGetAction(String epName, String binVersion, int confVersion, int reportingStatus, String schemaVersion, String customerId) {
		
		String response = sendCheckUpdatesAndGetResponse(epName, binVersion, confVersion, reportingStatus, schemaVersion, customerId);
		
		try {
            JSONObject json = new JSONObject(response);
            String action = json.getString("action");
            return action;
        }
        catch (Exception e){
            org.testng.Assert.fail("Could not parse action from check updates response.", e);
            return "";
        }
	}
	

	private Response sendCheckUpdates(String uuid, String epName, String binVersion, int confVersion, int reportingStatus, String schemaVersion, String customerId) {
		
		if (uuid == null || epName == null) {
			JLog.logger.info("Can't check updates with uuid/name null, uuid: {}, name: {}", uuid, epName); 
			return null;
		}

		JLog.logger.info("Starting checkUpdates. Params: uuid {} epName {} binVersion {} confVersion {} reporting status {} schema version {} "
				, uuid, epName, binVersion, confVersion, reportingStatus, schemaVersion);
		
		JLog.logger.debug("Will send request {}", String.format(CHECK_UPDATES, uuid, binVersion, confVersion, reportingStatus,epName ,schemaVersion));

		try {
				Response response = given().spec(requestSpecification)
						.contentType("application/json")
						.header(XSSL_Client_HEADER, String.format(XSSL_Client_HEADER_VALUE, customerId))
						.when()
						.get(String.format(CHECK_UPDATES, uuid, binVersion, confVersion, reportingStatus,epName ,schemaVersion));
				
				if (response == null) {
					return response;
				}
				if (response.statusCode() != HttpStatus.SC_OK) {
					JLog.logger.info("Status code: {}", response.statusCode());
					return response;
				}
				JLog.logger.info("Got 200 OK");
				try {
					JSONObject json = new JSONObject(response.body().asString());
		            String action = json.getString("action");
		            JLog.logger.info("Action: {}", action);
				}catch(Exception ex) {//Ignore exceptions
				}
				 return response;
			

		}  catch (Exception e) {
			JLog.logger.error("Failed to process the check updates request", e);
			org.testng.Assert.fail("Failed to process the check updates request", e);
			return null;
		}
	}

	public void register(String customerId, String ip, String hostname, String macAddress, String osType) {

		JLog.logger.info("Starting SimulatedAgentActions:register. Params: customer {} ip {} hostname {} os {} macAddress {}",
				customerId, ip, hostname, osType, macAddress);

		RegisterBody registerBody = new RegisterBody(customerId, ip, hostname, macAddress, osType);
		JLog.logger.debug("Register body: " + registerBody);

		JsonPath jsonPathEvaluator = null;

		try {
			JLog.logger.debug("Request Specification: " + requestSpecification);

			jsonPathEvaluator =
					given().spec(requestSpecification)
							.contentType("application/json")
							.header(XSSL_Client_HEADER, String.format(XSSL_Client_HEADER_VALUE, customerId))
							.body(objectMapper.writeValueAsString(registerBody))
							.when()
							.post(REGISTER)
							.then()
							.assertThat()
							.statusCode(HttpStatus.SC_OK)
							.extract().response().body().jsonPath();
			JLog.logger.debug("Json path evaluator: " + jsonPathEvaluator);

			String uuid = jsonPathEvaluator.get(ENDPOINT_ID);
//			String name = jsonPathEvaluator.get(ENDPOINT_NAME);
			JLog.logger.info("Register succeeded, got UUID {}", uuid);

			setAgentUuid(uuid);
			setName(registerBody.getName());

		} catch (JsonProcessingException e) {
			JLog.logger.error("Could not map RegisterBody object into a string", e);
			org.testng.Assert.fail("Could not map RegisterBody object into a string", e);
		} catch (JsonPathException e) {
			JLog.logger.error("Failed to parse the register response {}", (jsonPathEvaluator != null ? jsonPathEvaluator.prettify() : ""), e);
			org.testng.Assert.fail("Failed to parse the register response", e);
		} catch (Exception e) {
			JLog.logger.error("Failed to process the register request", e);
			org.testng.Assert.fail("Failed to process the register request", e);
		}
	}

	public void UpdateEpInfo (String epID, UpdateEpDetails json){
		JsonPath jsonPathEvaluator = null;

		try {

			JLog.logger.info("Starting SimulatedAgentActions:UpdateEpInfo. Params: customer {} name {}",
					json.getCustomerId(), json.getName());

			JLog.logger.debug("updateInfo body: " + objectMapper.writeValueAsString(json));



			JLog.logger.debug("Request Specification: " + requestSpecification);

			jsonPathEvaluator =
					given().spec(requestSpecification)
							.contentType("application/json")
							.header(XSSL_Client_HEADER, String.format(XSSL_Client_HEADER_VALUE, json.getCustomerId()))
							.body(objectMapper.writeValueAsString(json))
							.when()
							//.post(String.format(UPDATE_EP_INFO, epID))
							.post(UPDATE_EP_INFO + "/" + epID)
							.then()
							.assertThat()
							.statusCode(HttpStatus.SC_OK)
							.extract().response().body().jsonPath();
			JLog.logger.debug("Json path evaluator: " + jsonPathEvaluator);

		} catch (JsonProcessingException e) {
			JLog.logger.error("Could not map UpdateInfoBody object into a string", e);
			org.testng.Assert.fail("Could not map UpdateInfoBody object into a string", e);
		} catch (JsonPathException e) {
			JLog.logger.error("Failed to parse the UpdateInfo response {}", (jsonPathEvaluator != null ? jsonPathEvaluator.prettify() : ""), e);
			org.testng.Assert.fail("Failed to parse the UpdateInfoBody response", e);
		} catch (Exception e) {
			JLog.logger.error("Failed to process the UpdateInfo request", e);
			org.testng.Assert.fail("Failed to process the UpdateInfoBody request", e);
		}
	}


	public String getConf(String customerId){

		JLog.logger.info("Starting SimulatedAgentActions:getConf. Params: epId {}", agentUuid);


		JsonPath jsonPathEvaluator = null;
		try {
			String conf=
					given().spec(requestSpecification)
							.contentType("application/json")
							.header(XSSL_Client_HEADER, String.format(XSSL_Client_HEADER_VALUE, customerId))
							.when()
							.get(String.format(GET_CONF, agentUuid)).then()
							.assertThat()
							.statusCode(HttpStatus.SC_OK)
							.extract().response().body().asString();
			JLog.logger.info("get conf succeeded, got conf: '{}'", conf);
			setLastConf(conf);
			return conf;

		} catch (JsonPathException e) {
			JLog.logger.error("Failed to parse the get conf response {}", (jsonPathEvaluator != null ? jsonPathEvaluator.prettify() : ""), e);
			org.testng.Assert.fail("Failed to parse the get conf response", e);
		} catch (Exception e) {
			JLog.logger.error("Failed to process the get conf request", e);
			org.testng.Assert.fail("Failed to process the get conf request", e);
		}
		return null;
	}
	public String getAgentUuid() {
		return agentUuid;
	}

	public void setAgentUuid(String agentUuid) {
		this.agentUuid = agentUuid;
	}

	public String getLastConf() {
		return lastConf;
	}

	public void setLastConf(String conf) {
		this.lastConf = conf;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
