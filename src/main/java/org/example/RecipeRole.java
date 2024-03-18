package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecipeRole implements Serializable {
    private String roleName = "";
    private String station = "";
    private String emoji = "";
    private List<Ingredient> brings;

    public RecipeRole() {
    }



    public String getBringDisplayString(long quantity){
        return brings.stream()
                .map(ingredient -> ingredient.getDisplayString(quantity))
                .collect(Collectors.joining("\r\n"));
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<Ingredient> getBrings() {
        return brings;
    }

    public void setBrings(List<Ingredient> brings) {
        this.brings = brings;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}
