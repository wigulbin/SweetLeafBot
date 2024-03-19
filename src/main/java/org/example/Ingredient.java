package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Ingredient implements Serializable {
    private String itemName = "";
    private int quantity;
    private String emoji = "";
    private boolean mustBeStar;
    private String lock;
    private boolean overprep;

    public Ingredient() {
    }

    public String getDisplayString(long multiplier){
        String value = "";
        if(mustBeStar) value += "⭐";

        String emojiText = emoji;
        if(!emoji.endsWith(":"))
            emojiText = "<" + emoji + "> ";

        String itemText = itemName;
        if(!itemText.equalsIgnoreCase("Recipe"))
            itemText = (multiplier * quantity) + " " + itemName;

        value += emojiText + " " + itemText;

        return value;
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

    public boolean isOverprep() {
        return overprep;
    }

    public void setOverprep(boolean overprep) {
        this.overprep = overprep;
    }
}
