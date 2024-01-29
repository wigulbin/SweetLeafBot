package org.example;

import java.io.Serializable;
import java.util.List;

public class RecipeRole implements Serializable {
    private String roleName = "";
    private String station = "";
    private String emoji = "";
    private List<Ingredient> brings;

    public RecipeRole() {
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
