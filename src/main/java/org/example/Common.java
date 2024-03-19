package org.example;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
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

    public static String createRandomString(int start, int end){
        byte[] array = new byte[new Random().nextInt(start, end)];
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }

    public static <T> T getRandomItemFromList(List<T> items) {
        return items.get(new Random().nextInt(0, items.size()));
    }
}
