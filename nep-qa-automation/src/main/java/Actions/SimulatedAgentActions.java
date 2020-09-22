package Actions;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.exception.JsonPathException;
import io.restassured.response.Response;

import static io.restassured.RestAssured.*;

public class SimulatedAgentActions {

	public static final String DS_URL = "http://%s:53850/";
	public static final String REGISTER = "register";
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

	public SimulatedAgentActions() {
		try {
			JLog.logger.info("SimulatedAgentActions: {}", String.format(DS_URL, PropertiesFile.readProperty("ClusterToTest")));
			requestSpecification = new RequestSpecBuilder().setBaseUri(String.format(DS_URL, PropertiesFile.readProperty("ClusterToTest"))).build();
			
		}
		catch (Exception e) {
			org.testng.Assert.fail("Could not set RestAssured.baseURI", e);
		}
	}
	
	/**
	 *  Sends check updates post request to Lenny with the given params, without verifying response 200
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
		sendCheckUpdates(epName, binVersion, confVersion, reportingStatus, schemaVersion, customerId);
	}
	
	/**
	 * Sends check updates post request to Lenny with the given params, returns response body as is
	 * 
	 * @param epName
	 * @param binVersion
	 * @param confVersion
	 * @param reportingStatus
	 * @param schemaVersion
	 * @return
	 */
	public String sendCheckUpdatesAndGetResponse(String epName, String binVersion, int confVersion, int reportingStatus, String schemaVersion, String customerId) {
		
		Response response = sendCheckUpdates(epName, binVersion, confVersion, reportingStatus, schemaVersion, customerId);
		
		JLog.logger.info("Check updates succeeded, got response: '{}'", response);

		String responseStr = response.then()
		.assertThat()
		.statusCode(HttpStatus.SC_OK)
		.extract().response().body().asString();
		
		return responseStr;
	}
	
	/**
	 * Sends check updates post request to Lenny with the given params, returns the action from the response body 
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
	

	private Response sendCheckUpdates(String epName, String binVersion, int confVersion, int reportingStatus, String schemaVersion, String customerId) {

		JLog.logger.info("Starting checkUpdates. Params: uuid {} epName {} binVersion {} confVersion {} reporting status {} schema version {} "
				, getAgentUuid(), epName, binVersion, confVersion, reportingStatus, schemaVersion);
		
		JLog.logger.info("Will send request {}", String.format(CHECK_UPDATES, getAgentUuid(), binVersion, confVersion, reportingStatus,epName ,schemaVersion));

		try {
			return
					given().spec(requestSpecification)
							.contentType("application/json")
							.header(XSSL_Client_HEADER, String.format(XSSL_Client_HEADER_VALUE, customerId))
							.when()
							.get(String.format(CHECK_UPDATES, getAgentUuid(), binVersion, confVersion, reportingStatus,epName ,schemaVersion));
			
			

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

		JsonPath jsonPathEvaluator = null;

		try {
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
