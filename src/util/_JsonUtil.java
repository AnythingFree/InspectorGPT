package util;

import java.util.HashMap;
import java.util.Map;

public class _JsonUtil {

    public static Map<String, String> jsonToMap(String jsonString) {
        Map<String, String> map = new HashMap<>();

        // Remove curly braces and whitespace
        jsonString = jsonString.replaceAll("[{}]", "");

        // Split key-value pairs by commas
        String[] keyValuePairs = jsonString.split("; ");

        for (String pair : keyValuePairs) {


            //int colonIndex = pair.indexOf(":");
            String[] keyValue = pair.split(":", 2); // this will split the string into 2 parts



            if (keyValue.length == 2) {
                String key = keyValue[0].replaceAll("\"", "").trim();
                String value = keyValue[1].trim();
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    // Remove double quotes from string values
                    value = value.substring(1, value.length() - 1);
                }
                map.put(key, value);
            }
        }

        return map;
    }

    public static void main(String[] args) {
        String jsonString = "{type:request; data:some data}";

        Map<String, String> resultMap = jsonToMap(jsonString);

        for (Map.Entry<String, String> entry : resultMap.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
    }
}
