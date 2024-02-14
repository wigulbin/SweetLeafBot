package org.example;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return Objects.equals(id, userInfo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @JsonIgnore
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
