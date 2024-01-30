package org.example;

import java.io.Serializable;

public class UserInfo implements Serializable {
    private String id = "";
    private String name = "";
    private String recipeRole = "";

    public UserInfo() {
    }

    public UserInfo(String id, String name) {
        this.id = id;
        this.name = name;
        this.recipeRole = "";
    }

    public UserInfo(String id, String name, String recipeRole) {
        this.id = id;
        this.name = name;
        this.recipeRole = recipeRole;
    }

    public String getPingText(){
        return "<@" + id + ">";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRecipeRole() {
        return recipeRole;
    }

    public void setRecipeRole(String recipeRole) {
        this.recipeRole = recipeRole;
    }
}
