package org.example;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RoleBrings implements Serializable {
    private List<Ingredient> ingredientList = new ArrayList<>();

    public RoleBrings() {
    }

    public List<Ingredient> getIngredientList() {
        return ingredientList;
    }

    public void setIngredientList(List<Ingredient> ingredientList) {
        this.ingredientList = ingredientList;
    }
}
