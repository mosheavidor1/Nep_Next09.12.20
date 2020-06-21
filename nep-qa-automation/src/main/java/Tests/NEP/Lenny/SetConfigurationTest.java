package Tests.NEP.Lenny;

import Actions.NepActions;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class SetConfigurationTest extends GenericTest {
    private NepActions action;

    @Factory(dataProvider = "getData")
    public SetConfigurationTest(Object dataToSet) {
        super(dataToSet);
        action = new NepActions();

    }

    @Test()
    public void SetConfiguration () {

        //Short POC test

        //String myJson = "{ \"customerId\": 1007, \"configuration\": { \"global_conf\": { \"log_level\": \"info\", \"conf_version\": 3 }, \"agent\": { \"ds_host\": \"endpoint-protection-services.local.tw-test.net\", \"ds_port\": 443, \"ds_protocol\": \"https\", \"check_update_period\": 61, \"report_period\": 60, \"ds_max_off_period\": 48, \"modules\": [{ \"name\": \"Windows Log Monitor\", \"binary_name\": \"WLM.dll\", \"enabled\": true }, { \"name\": \"Log File Monitor\", \"binary_name\": \"LFM.dll\", \"enabled\": true } ], \"transport\": { \"transport_type\": 2, \"syslog\": { \"port\": 0 }, \"scp\": { \"host\": \"siem-ingress.trustwave.com\", \"dest_folder\": \"/var/siem/data/nep\", \"port\": 9022, \"user\": \"twsiem\", \"ack\": false, \"max_send_folder_size\": 100 } } }, \"wlm\": { \"max_monitor_queue_size\": 10000, \"queues_collector_idle_time\": 5, \"monitor_items\": [{ \"log_name\": \"Security\", \"enabled\": true, \"advanced_filter\": false, \"filters\": [] }, { \"log_name\": \"System\", \"enabled\": true, \"advanced_filter\": false, \"filters\": [] } ] }, \"lfm\": { \"max_monitor_queue_size\": 10000, \"queues_collector_idle_time\": 5, \"monitor_items\": [] } } }";
        String myJson = data.get("Settings Json");
        String lennyIP = PropertiesFile.readProperty("ClusterToTest");

        RestAssured.baseURI  = "http://" + lennyIP + ":9091/nep-centcom-client/";

        Response r = given()
                .contentType("application/json").
                        body(myJson).
                        when().
                        post("initCustomerSettings");

        int response = r.getStatusCode();
        JLog.logger.info("Set configuration response: " + response);

        //Second method of posting and verifying response
        given()
                .contentType("application/json").
                body(myJson).
                when().
                post("initCustomerSettings").
                then().statusCode(200);


    }

    @AfterMethod
    public void Close() throws Exception {
        afterMethod();
        action.CloseApplication();

    }



}
