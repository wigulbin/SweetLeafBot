package org.example;

import java.util.TimerTask;

public class FileTask extends TimerTask {

    @Override
    public void run() {
        System.out.println("Saving...");
        Fileable.write(PartyInfo.class, PartyInfo.getInfoList());
//        Fileable.write(User.class, User.getUserList());
    }
}
