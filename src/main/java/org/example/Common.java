package org.example;

import java.util.UUID;

public class Common {

    public static int parseInt(String num) {
        try{
            return Integer.parseInt(num);
        } catch (Exception e){}

        return 0;
    }
    public static long parseLong(String num) {
        try{
            return Long.parseLong(num);
        } catch (Exception e){}

        return 0;
    }

    public static String removeSpaces(String string){
        return string.replaceAll(" ", "");
    }

    public static String normalizeString(String string){
        return string.replaceAll(" ", "").toLowerCase();
    }

    public static String createGUID(){
        return UUID.randomUUID().toString();
    }
}
