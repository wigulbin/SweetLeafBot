package org.example;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private int id;
    private LocalDateTime lastChatted;

    private static List<User> infoList = new ArrayList<>();
    public static List<User> getUserList(){
        if(infoList.isEmpty()) infoList = Fileable.readFromFile(User.class);

        return new ArrayList<>(infoList);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getLastChatted() {
        return lastChatted;
    }

    public void setLastChatted(LocalDateTime lastChatted) {
        this.lastChatted = lastChatted;
    }
}
