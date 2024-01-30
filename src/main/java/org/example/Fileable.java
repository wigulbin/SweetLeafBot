package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Fileable implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(Fileable.class);

    public static <T> void write(Class<T> className, List<T> infoList){
        String filename = className.getSimpleName() + "_" + Main.guildId + ".txt";

        try(FileOutputStream f = new FileOutputStream("files/" + filename); ObjectOutputStream o = new ObjectOutputStream(f);){
            for (T info : infoList)
                o.writeObject(info);

            log.info("Wrote " + infoList.size() + " " + className.getSimpleName() + " to " + filename);
        } catch (Exception e){
            log.error("Error writing to file: " + filename, e);
        }
    }

    public static <T> List<T> readFromFile(Class<T> className){
        String filename = className.getSimpleName() + "_" + Main.guildId + ".txt";

        List<T> infoList = new ArrayList<>();
        try(FileInputStream fi = new FileInputStream("files/" + filename); ObjectInputStream oi = new ObjectInputStream(fi);){
            T info;
            while(true){
                info = (T) oi.readObject();
                if(info != null) infoList.add(info);
            }

        } catch (EOFException eof){
            log.info("Read " + infoList.size() + " " + className.getSimpleName() + " from " + filename);
        }
        catch (Exception e){
            log.error("Error reading from file: " + filename, e);
        }

        return infoList;
    }

    public static String getFileName() {
        return "dumpFile.txt";
    }
}
