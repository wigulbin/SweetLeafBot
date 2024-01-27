package org.example;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PartyInfo
{
    private TypeInfo type;
    private UserInfo hostInfo;
    private long people;
    private String server;
    private boolean status;

    private List<UserInfo> userList;

    public PartyInfo(TypeInfo type, UserInfo hostInfo, long people, String server, boolean status)
    {
        this.type = type;
        this.hostInfo = hostInfo;
        this.people = people;
        this.server = server;
        this.status = status;

        this.userList = new ArrayList<>();
    }

    public static PartyInfo createFromEvent(ChatInputInteractionEvent event) {
        String userName = event.getInteraction().getUser().getGlobalName().get();
        String userid = event.getInteraction().getUser().getId().asString();
        TypeInfo type = TypeInfo.getType(event.getOption("type").get().getValue().get().asString());
        long people;
        if(event.getOption("people").isPresent())
            people = event.getOption("people").get().getValue().get().asLong();
        else
            people = 10;

        String server = event.getOption("server").get().getValue().get().asString();

        long finalPeople = people;

        return new PartyInfo(type, new PartyInfo.UserInfo(userid, userName), finalPeople, server, true);
    }


    String getImageURL() {
        return type.getImageURL();
    }

    Color getColor() {
        return type.getColor();
    }

    public void addUser(UserInfo userInfo){
        if(!userList.contains(userInfo))
            userList.add(userInfo);
    }

    public void addUser(String id, String name){
        UserInfo userInfo = new UserInfo(id, name);
        if(!userList.contains(userInfo))
            userList.add(userInfo);
    }

    public void removeUser(UserInfo userInfo){
        userList.remove(userInfo);
    }


    public EmbedCreateSpec createEmbed()
    {
        PartyInfo partyInfo = this;
        String status = "Open";
        if(!partyInfo.isStatus())
            status = "Closed";

        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                .color(partyInfo.getColor())
                .title("Hosted by " + partyInfo.getHostInfo().name + "")
                .author( "(" + status + ") " + partyInfo.getServer() + " " + partyInfo.getType() + " Party", "", partyInfo.getImageURL())
                .description(partyInfo.getHostInfo().getPingText() + " is hosting a " + partyInfo.getType() + " party at ___")
                .thumbnail(partyInfo.getImageURL());

        embed = embed.addField("Participants:", "", false);

        int personCount = 0;
        for (UserInfo userInfo : partyInfo.getUserList())
        {
            embed = embed.addField("", "- " + userInfo.getPingText(), false);
            personCount++;
        }

        for(int i = personCount; i < partyInfo.getPeople(); i++)
            embed = embed.addField("", "- Open", false);

//        embed = embed.addField((EmbedCreateFields.Field) ActionRow.of(Button.primary("test", "")));

        embed = embed.timestamp(Instant.now());

        return embed.build();
    }


    public record UserInfo(String id, String name) {
        String getId(){return id;}
        String getName(){return name;}
        String getPingText(){
            return "<@" + id + ">";
        }
    }

    public TypeInfo getType() {
        return type;
    }

    public void setType(TypeInfo type){
        this.type = type;
    }
    public UserInfo getHostInfo()
    {
        return hostInfo;
    }

    public void setHostInfo(UserInfo hostInfo)
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

    public List<UserInfo> getUserList()
    {
        return userList;
    }

    public void setUserList(List<UserInfo> userList)
    {
        this.userList = userList;
    }
}
