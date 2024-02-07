package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Fileable implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(Fileable.class);

    public static <T> void write(Class<T> className, List<T> infoList){
        String filename = className.getSimpleName() + "_" + Main.guildId + ".json";

        try(BufferedWriter writer = new BufferedWriter(new FileWriter("files" + File.separator + filename))){
            ObjectMapper mapper = createObjectMapper();
            writer.write(mapper.writeValueAsString(infoList));
            log.info("Wrote " + infoList.size() + " " + className.getSimpleName() + " to " + filename);
        } catch (Exception e){
            log.error("Error writing to file: " + filename, e);
        }
    }

    public static <T> T readFromFile(String className, TypeReference<T> type){
        String filename = className + "_" + Main.guildId + ".json";

        ObjectMapper mapper = createObjectMapper();
        try{
            String json = String.join("", Files.readAllLines(Path.of("files" + File.separator + filename)));
            return mapper.readValue(json, type);

        } catch (Exception e) {
            log.error("Error reading from file: " + filename, e);
        }

        return null;
    }

    public static ObjectMapper createObjectMapper(){
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }

    public static String getFileName() {
        return "dumpFile.txt";
    }
}
