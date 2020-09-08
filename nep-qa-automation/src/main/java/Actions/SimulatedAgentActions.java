package Actions;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.exception.JsonPathException;

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
	private String conf;
	private String name;
	
	@JsonInclude(JsonInclude.Include.NON_NULL)
	class RegisterBody{

		@JsonProperty("Name")
		private String name;
		@JsonProperty("OsTypeAndVersion")
		private String os;
		@JsonProperty("CustomerId")
		private long customerId;
		@JsonProperty("EndpointIp")
		private String ip;
		@JsonProperty("MacAddress")
		private String macAddress;

		public RegisterBody(long customerId, String ip, String name, String macAddress, String os) {
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

		public long getCustomerId() {
			return customerId;
		}

		public void setCustomerId(long customerId) {
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

	public SimulatedAgentActions(long customerId, String ip, String name, String macAddress, String osType) {
		try {
			requestSpecification = new RequestSpecBuilder().setBaseUri(String.format(DS_URL, PropertiesFile.readProperty("ClusterToTest"))).build();
//			RestAssured.baseURI = String.format(DS_URL, PropertiesFile.readProperty("ClusterToTest"));
			register(customerId, ip, name, osType, macAddress);
		}
		catch (Exception e) {
			org.testng.Assert.fail("Could not set RestAssured.baseURI", e);
		}
	}

	public String checkUpdates(String epName, String binVersion, String confVersion, int reportingStatus, String schemaVersion) {

		JLog.logger.info("Starting checkUpdates. Params: uuid {} epName {} binVersion {} confVersion {} reporting status {} schema version "
				, getAgentUuid(), epName, binVersion, confVersion, reportingStatus, schemaVersion);

		JsonPath jsonPathEvaluator = null;
		try {
			String action=
					given().spec(requestSpecification)
							.contentType("application/json")
							.when()
							.get(String.format(CHECK_UPDATES, getAgentUuid()/*, epName*/, binVersion, confVersion, reportingStatus,epName ,schemaVersion)).then()
							.assertThat()
							.statusCode(HttpStatus.SC_OK)
							.extract().response().body().asString();
			JLog.logger.info("Check updates succeeded, got action: '{}'", action);

			return action;

		} catch (JsonPathException e) {
			JLog.logger.error("Failed to parse the check updates response {}", (jsonPathEvaluator != null ? jsonPathEvaluator.prettify() : ""), e);
			org.testng.Assert.fail("Failed to parse the check updates response", e);
			return null;
		} catch (Exception e) {
			JLog.logger.error("Failed to process the check updates request", e);
			org.testng.Assert.fail("Failed to process the check updates request", e);
			return null;
		}
	}

	public void register(long customerId, String ip, String hostname, String osType, String macAddress) {

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

	public void getConf(String epId){

		JLog.logger.info("Starting SimulatedAgentActions:getConf. Params: epId {}",epId);


		JsonPath jsonPathEvaluator = null;
		try {
			String conf=
					given().spec(requestSpecification)
							.contentType("application/json")
							.when()
							.get(String.format(GET_CONF, epId)).then()
							.assertThat()
							.statusCode(HttpStatus.SC_OK)
							.extract().response().body().asString();
			JLog.logger.info("get conf succeeded, got conf: '{}'", conf);
			setConf(conf);

		} catch (JsonPathException e) {
			JLog.logger.error("Failed to parse the get conf response {}", (jsonPathEvaluator != null ? jsonPathEvaluator.prettify() : ""), e);
			org.testng.Assert.fail("Failed to parse the get conf response", e);
		} catch (Exception e) {
			JLog.logger.error("Failed to process the get conf request", e);
			org.testng.Assert.fail("Failed to process the get conf request", e);
		}
	}
	public String getAgentUuid() {
		return agentUuid;
	}

	public void setAgentUuid(String agentUuid) {
		this.agentUuid = agentUuid;
	}

	public String getConf() {
		return conf;
	}

	public void setConf(String conf) {
		this.conf = conf;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
