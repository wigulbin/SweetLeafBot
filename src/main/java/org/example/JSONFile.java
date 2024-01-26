package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JSONFile {
    private static final String keys = "";

    public static String getJSONValueFromFile(String key, String resourceName) {
        try
        {
            Map<String, String> keyMap = parseJsonStringForMap(Files.readString(Path.of(getResourcePath(resourceName))));
            if(keyMap != null)
                return keyMap.getOrDefault(key, "");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return "";
    }
    public static Map<String, String> parseJsonStringForMap(String json) {
        try
        {
            return new ObjectMapper().readValue(json, Map.class);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return new HashMap<>();
    }

    public static String getResourcePath(String resourceName) {
        URL resource = JSONFile.class.getClassLoader().getResource(resourceName);
        if(resource != null) {
            String path =resource.getPath();
            if(path.startsWith("/"))
                path = path.substring(1);
            return path;
        }

        return "";
    }
}
