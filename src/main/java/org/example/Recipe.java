package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Recipe implements Serializable {
    private String recipeName = "";
    private List<RecipeRole> roles;

    private int recipeMultiplier;

    public Recipe() {
    }



    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public List<RecipeRole> getRoles() {
        return roles;
    }

    public void setRoles(List<RecipeRole> roles) {
        this.roles = roles;
    }

    public int getRecipeMultiplier() {
        return recipeMultiplier;
    }

    public void setRecipeMultiplier(int recipeMultiplier) {
        this.recipeMultiplier = recipeMultiplier;
    }
}
