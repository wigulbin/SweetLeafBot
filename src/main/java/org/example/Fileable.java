package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Fileable implements Serializable {
    public static <T> void write(Class<T> className, List<T> infoList){
        try(FileOutputStream f = new FileOutputStream(className.getSimpleName() + ".txt"); ObjectOutputStream o = new ObjectOutputStream(f);){
            for (T info : infoList)
                o.writeObject(info);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public static <T> List<T> readFromFile(Class<T> className){
        List<T> infoList = new ArrayList<>();
        try(FileInputStream fi = new FileInputStream(className.getSimpleName() + ".txt"); ObjectInputStream oi = new ObjectInputStream(fi);){
            T info;
            do{
                info = (T) oi.readObject();
                if(info != null) infoList.add(info);
            } while (info != null);
        } catch (Exception e){
            System.out.println(e);
        }

        return infoList;
    }

    public static String getFileName() {
        return "dumpFile.txt";
    }
}
