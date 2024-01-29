package org.example;

import java.io.Serializable;

public class Ingredient implements Serializable {
    private String itemName = "";
    private int quantity;
    private String emoji = "";
    private boolean mustBeStar;
    private String lock;

    public Ingredient() {
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public boolean isMustBeStar() {
        return mustBeStar;
    }

    public void setMustBeStar(boolean mustBeStar) {
        this.mustBeStar = mustBeStar;
    }

    public String getLock() {
        return lock;
    }

    public void setLock(String lock) {
        this.lock = lock;
    }
}
