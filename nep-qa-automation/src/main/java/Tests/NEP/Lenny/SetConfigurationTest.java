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
        String myJson = data.get("Settings Json");
        String lennyIP = PropertiesFile.readProperty("ClusterToTest");

        RestAssured.baseURI  = "http://" + lennyIP + ":9091/nep-centcom-client/";

        Response r = given()
                .contentType("application/json").
                        body(myJson).
                        when().
                        post("initCustomerSettings");

        int response = r.getStatusCode();

        if(response==200)
            JLog.logger.info("Success. Set configuration response: " + response);
        else
            org.testng.Assert.fail("Error. Response status code received is: " + response);

        //Second method of posting and verifying response
        given()
                .contentType("application/json").
                body(myJson).
                when().
                post("initCustomerSettings").
                then().statusCode(200);
    }

}
