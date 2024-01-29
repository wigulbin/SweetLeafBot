package org.example;

import java.util.Timer;
import java.util.TimerTask;

public class FileTask extends TimerTask {

    public static void loadObjectsFromFile() {
        new Timer().schedule(new FileTask(), 0, 300000);
    }

    @Override
    public void run() {
        System.out.println("Saving...");
        Fileable.write(PartyInfo.class, PartyInfo.getInfoList());
//        Fileable.write(User.class, User.getUserList());
    }
}
