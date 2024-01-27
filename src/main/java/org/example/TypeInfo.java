package org.example;

import discord4j.rest.util.Color;
import me.xdrop.fuzzywuzzy.FuzzySearch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TypeInfo implements Serializable {
    private String code = "";
    private String name = "";

    private static List<TypeInfo> codesList = new ArrayList<>();
    public static void generateCodes(){
        List<String> types = List.of("Fishing", "Hunting", "Bug Catching", "Cooking", "Custom", "Mining");
        codesList = types.stream().map(name -> new TypeInfo(Common.removeSpaces(name).toLowerCase(), name)).toList();
    }

    public static List<TypeInfo> getOrCreateTypeCodes(){
        if(codesList.isEmpty())
            generateCodes();

        return new ArrayList<>(codesList);
    }

    public static TypeInfo getType(String code) {
        return codesList.stream().filter(info -> info.code.equals(code)).findFirst().orElse(null);
    }

    private TypeInfo(){};
    private TypeInfo(String code, String name)
    {
        this.code = code;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeInfo typeInfo = (TypeInfo) o;
        return Objects.equals(code, typeInfo.code) && Objects.equals(name, typeInfo.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name);
    }

    public static int compareTypes(TypeInfo info1, TypeInfo info2, String comparison){
        String name1 = info1.getName().toLowerCase();
        String name2 = info2.getName().toLowerCase();

        if(name1.startsWith(comparison)) return -1;
        if(name2.startsWith(comparison)) return 1;

        return -1 * Integer.compare(FuzzySearch.ratio(name1, comparison), FuzzySearch.ratio(name2, comparison));
    }

    @Override
    public String toString() {
        return name;
    }

    String getImageURL() {
        if(code.equals("fishing"))
            return "https://palia.wiki.gg/images/thumb/9/97/Currency_Fishing.png/75px-Currency_Fishing.png";
        if(code.equals("hunting"))
            return "https://palia.wiki.gg/images/thumb/8/8b/Currency_Hunting.png/75px-Currency_Hunting.png";
        if(code.equals("foraging"))
            return "https://palia.wiki.gg/images/thumb/b/b0/Currency_Foraging.png/75px-Currency_Foraging.png";
        if(code.equals("bugcatching"))
            return "https://palia.wiki.gg/images/thumb/4/47/Currency_Bug.png/75px-Currency_Bug.png";
        if(code.equals("cooking"))
            return "https://palia.wiki.gg/images/thumb/c/cb/Currency_Cooking.png/75px-Currency_Cooking.png";
        if(code.equals("mining"))
            return "https://palia.wiki.gg/images/thumb/b/b0/Currency_Mining.png/75px-Currency_Mining.png";

        return "https://palia.com/assets/img/squirrels.png";
    }


    Color getColor() {
        if(code.equals("fishing"))
            return Color.CYAN;
        if(code.equals("hunting"))
            return Color.DARK_GOLDENROD;
        if(code.equals("foraging"))
            return Color.LIGHT_SEA_GREEN;
        if(code.equals("bugcatching"))
            return Color.MOON_YELLOW;
        if(code.equals("cooking"))
            return Color.PINK;
        if(code.equals("mining"))
            return Color.DEEP_LILAC;

        return Color.BROWN;
    }


    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
