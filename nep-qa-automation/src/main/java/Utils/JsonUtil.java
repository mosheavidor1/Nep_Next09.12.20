package Utils;

import org.json.JSONObject;

public class JsonUtil {
    public static Long GetCustomerIDFromSentConfiguration (String configurationJson) {
        try {
            JSONObject json = new JSONObject(configurationJson);
            long customerId = json.getLong("customerId");
            return customerId;
        }
        catch (Exception e){
            org.testng.Assert.fail("Could not find customerId at configuration json sent to LNE. It is expected to be at the json top level. Json sent: " + configurationJson + "\n" + e.toString());
            return (long)-1;
        }
    }

    public static String ChangeTagConfiguration(String configurationJson, String tag, String nameToSet) {
        try {
            JSONObject json = new JSONObject(configurationJson);
            json = json.put(tag, nameToSet);
            return json.toString();
        } catch (Exception e) {
            org.testng.Assert.fail("Could not .... Json sent: " + configurationJson + "\n" + e.toString());
            return null;
        }
    }
}

