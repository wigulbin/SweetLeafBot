package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Recipes implements Serializable {
    List<Recipe> recipes;


    private static final List<Recipe> RECIPE_LIST = new ArrayList<>();

    public static List<Recipe> getRecipeList(){
        return new ArrayList<>(RECIPE_LIST);
    }

    public static Recipe getRecipe(String recipeCode) {
        return getRecipeList().stream().filter(recipe -> Common.normalizeString(recipe.getRecipeName()).equalsIgnoreCase(recipeCode)).findFirst().orElse(null);
    }

    public static void loadRecipesFromFile() {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = JSONFile.getJSONString("recipe.json");

        try{
            Recipes recipes = objectMapper.readValue(jsonString, Recipes.class);
            if(recipes != null) RECIPE_LIST.addAll(recipes.getRecipes());
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public Recipes() {
    }

    public List<Recipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
    }
}
