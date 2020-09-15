package Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

public class JsonUtil {


    public static String ChangeTagsConfiguration(String configurationJson, Map<String,Object> tagsNewValues) {
        try {
            JSONObject json = new JSONObject(configurationJson);
            for(String tag: tagsNewValues.keySet()) {
                ChangeTagConfiguration(json, tag,tagsNewValues.get(tag));
            }
            return json.toString();
        } catch (Exception e) {
            org.testng.Assert.fail("Could not .... Json sent: " + configurationJson + "\n" + e.toString());
            return null;
        }
    }

    public static String ChangeTagConfiguration(String configurationJson,String tag,Object val) {
        try {
            JSONObject json = new JSONObject(configurationJson);
            ChangeTagConfiguration(json, tag,val);

            return json.toString();
        } catch (Exception e) {
            org.testng.Assert.fail("Could not .... Json sent: " + configurationJson + "\n" + e.toString());
            return null;
        }
    }
    
    public static boolean ChangeTagConfiguration(JSONObject json, String tag, Object value) {
        if(json.has(tag)){
            json.put(tag, value);
            return true;
        }else {
            Iterator<String> jsonKeys = json.keys();
            while (jsonKeys.hasNext()) {
                String jsonKey = jsonKeys.next();
                if(json.get(jsonKey) instanceof JSONArray){
                    JSONArray jsonArray = (JSONArray) json.get(jsonKey);
                    Iterator<Object> jsonArrayIterator = jsonArray.iterator();
                    while (jsonArrayIterator.hasNext()){
                        Object jsonArrayNext = jsonArrayIterator.next();
                        if(jsonArrayNext instanceof JSONObject){
                            if(ChangeTagConfiguration((JSONObject) jsonArrayNext, tag,value)){
                                return true;
                            }
                        }
                    }
                }
                else if (json.get(jsonKey) instanceof JSONObject) {
                    if(ChangeTagConfiguration((JSONObject) json.get(jsonKey), tag,value)){
                        return true;
                    }
                }

            }
        }
        return false;
    }

    public static boolean CompareKeyValue(String conf, String tag, Object val) {
        JSONObject json = new JSONObject(conf);
        return CompareKeyValue(json,tag,val);
    }
    public static boolean CompareKeyValue(JSONObject json, String tag, Object val) {
        if(json.has(tag)){
            return json.get(tag).equals(val);
        }else {
            Iterator<String> jsonKeys = json.keys();
            while (jsonKeys.hasNext()) {
                String jsonKey = jsonKeys.next();
                if(json.get(jsonKey) instanceof JSONArray){
                    JSONArray jsonArray = (JSONArray) json.get(jsonKey);
                    Iterator<Object> jsonArrayIterator = jsonArray.iterator();
                    while (jsonArrayIterator.hasNext()){
                        Object jsonArrayNext = jsonArrayIterator.next();
                        if(jsonArrayNext instanceof JSONObject){
                            if(CompareKeyValue((JSONObject) jsonArrayNext, tag,val)){
                                return true;
                            }
                        }
                    }
                }
                else if (json.get(jsonKey) instanceof JSONObject) {
                    if(CompareKeyValue((JSONObject) json.get(jsonKey), tag,val)){
                        return true;
                    }
                }

            }
        }
        return false;
    }
}

