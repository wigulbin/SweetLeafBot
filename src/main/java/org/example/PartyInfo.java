package org.example;

import discord4j.rest.util.Color;

import java.util.ArrayList;
import java.util.List;

public class PartyInfo
{
    private String type;
    private Main.UserInfo hostInfo;
    private long people;
    private String server;
    private boolean status;

    private List<Main.UserInfo> userList;

    public PartyInfo(String type, Main.UserInfo hostInfo, long people, String server, boolean status)
    {
        this.type = type;
        this.hostInfo = hostInfo;
        this.people = people;
        this.server = server;
        this.status = status;

        this.userList = new ArrayList<>();
    }

    String getImageURL() {
        if(type.equals("Fishing"))
            return "https://palia.wiki.gg/images/thumb/9/97/Currency_Fishing.png/75px-Currency_Fishing.png";
        if(type.equals("Hunting"))
            return "https://palia.wiki.gg/images/thumb/8/8b/Currency_Hunting.png/75px-Currency_Hunting.png";
        if(type.equals("Foraging"))
            return "https://palia.wiki.gg/images/thumb/b/b0/Currency_Foraging.png/75px-Currency_Foraging.png";
        if(type.equals("BugCatching"))
            return "https://palia.wiki.gg/images/thumb/4/47/Currency_Bug.png/75px-Currency_Bug.png";

        return "https://palia.wiki.gg/images/thumb/c/cb/Currency_Cooking.png/75px-Currency_Cooking.png";
    }

    Color getColor() {
        if(type.equals("Fishing"))
            return Color.CYAN;
        if(type.equals("Hunting"))
            return Color.DARK_GOLDENROD;
        if(type.equals("Foraging"))
            return Color.LIGHT_SEA_GREEN;
        if(type.equals("BugCatching"))
            return Color.MOON_YELLOW;

        return Color.PINK;
    }

    public void addUser(Main.UserInfo userInfo){
        if(!userList.contains(userInfo))
            userList.add(userInfo);
    }

    public void removeUser(Main.UserInfo userInfo){
        userList.remove(userInfo);
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public Main.UserInfo getHostInfo()
    {
        return hostInfo;
    }

    public void setHostInfo(Main.UserInfo hostInfo)
    {
        this.hostInfo = hostInfo;
    }

    public long getPeople()
    {
        return people;
    }

    public void setPeople(long people)
    {
        this.people = people;
    }

    public String getServer()
    {
        return server;
    }

    public void setServer(String server)
    {
        this.server = server;
    }

    public boolean isStatus()
    {
        return status;
    }

    public void setStatus(boolean status)
    {
        this.status = status;
    }

    public List<Main.UserInfo> getUserList()
    {
        return userList;
    }

    public void setUserList(List<Main.UserInfo> userList)
    {
        this.userList = userList;
    }
}
