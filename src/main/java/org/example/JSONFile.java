package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JSONFile {
    private static final String keys = "";

    private static final Logger log = LoggerFactory.getLogger(JSONFile.class);
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
            log.error("JSON Parse error", e);
        }

        return new HashMap<>();
    }

    public static String getResourcePath(String resourceName) {
        URL resource = JSONFile.class.getClassLoader().getResource(resourceName);
        if(resource != null) {
            String path =resource.getPath();
            log.info("Path: " + path);
            if(path.startsWith(File.separator))
                path = path.substring(1);
            return path;
        }

        return "";
    }

    public static String getResourcePath2(String resourceName) {
        try(InputStream inputStream = JSONFile.class.getClassLoader().getResourceAsStream(resourceName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
            return reader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        }catch (Exception e){
            log.error("File error", e);
        }

        return "";
    }

    public static String getJSONString(String fileName){
        try{
            String filePath = getResourcePath(fileName);
            log.info("File Path: " + filePath);
            Path path = Path.of(getResourcePath(fileName));

            return Files.readString(path);
        } catch (Exception e){
            log.error("JSON Parse error", e);
        }

        return "";
    }
}
