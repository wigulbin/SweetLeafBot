package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public class FileTask extends TimerTask {

    private static String GUILD_ID = "";
    public static void loadObjectsFromFile() {
//        new Timer().schedule(new FileTask(), 0, 300000);
        new Timer().schedule(new FileTask(), 0, 1000);
    }

    @Override
    public void run() {
        PartyInfo.writeInfoList();
//        Fileable.write(User.class, User.getUserList());
    }
}
