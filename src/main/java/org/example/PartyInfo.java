package org.example;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

import java.io.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PartyInfo implements Serializable
{
    private TypeInfo type;
    private UserInfo hostInfo;
    private long people;
    private String server;
    private boolean status;
    private String commandGuid;
    private String timestamp = "";

    private Recipe recipe;


    private List<UserInfo> userList;

    public PartyInfo(TypeInfo type, UserInfo hostInfo, long people, String server, boolean status, String commandGuid, String timestamp, Recipe recipe)
    {
        this.type = type;
        this.hostInfo = hostInfo;
        this.people = people;
        this.server = server;
        this.status = status;
        this.commandGuid = commandGuid;
        this.timestamp = timestamp;
        this.recipe = recipe;

        this.userList = new ArrayList<>();
    }

    private static List<PartyInfo> infoList = new ArrayList<>();
    public static List<PartyInfo> getInfoList(){
        if(infoList.isEmpty()) infoList = Fileable.readFromFile(PartyInfo.class);

        return new ArrayList<>(infoList);
    }

    public static PartyInfo getPartyInfoByGuid(String guid){
        return getInfoList().stream().filter(info -> info.getCommandGuid().equals(guid)).findFirst().orElse(null);
    }

    public static PartyInfo createFromEvent(ChatInputInteractionEvent event, String modalGuid) {
        String userName = event.getInteraction().getUser().getGlobalName().get();
        String userid = event.getInteraction().getUser().getId().asString();

        TypeInfo type = TypeInfo.getType(event.getOption("type").get().getValue().get().asString());
        long people = getPeople(event);

        String timestamp = getTimestamp(event);

        String server = event.getOption("server").get().getValue().get().asString();

        long finalPeople = people;

        UserInfo userInfo = new PartyInfo.UserInfo(userid, userName, "");
        Recipe recipe = getRecipe(event);

        PartyInfo info = new PartyInfo(type, userInfo, finalPeople, server, true, modalGuid, timestamp, recipe);
        infoList.add(info);
        return info;
    }
    private static Recipe getRecipe(ChatInputInteractionEvent event) {
        Recipe recipe = null;

        String recipeName = "";
        if(event.getOption("recipe").isPresent()) recipeName = event.getOption("recipe").get().getValue().get().asString();

        if(!recipeName.isEmpty()){
            String recipeCode = Common.removeSpaces(recipeName).toLowerCase();
            recipe = Recipes.getRecipe(recipeCode);
        }

        return recipe;
    }

    private static String getTimestamp(ChatInputInteractionEvent event) {
        String timestamp = "";
        if(event.getOption("timestamp").isPresent()) timestamp = event.getOption("timestamp").get().getValue().get().asString();
        return timestamp;
    }

    private static long getPeople(ChatInputInteractionEvent event) {
        long people = 10;
        if(event.getOption("people").isPresent()) people = event.getOption("people").get().getValue().get().asLong();
        return people;
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

    public void removeUser(String userid){
        userList.removeIf(user -> user.getId().equals(userid));
    }


    public EmbedCreateSpec createEmbed()
    {
        PartyInfo partyInfo = this;
        String status = getStatus(partyInfo);

        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                .color(partyInfo.getColor())
                .title("Hosted by " + partyInfo.getHostInfo().name + "")
                .author( "(" + status + ") " + partyInfo.getServer() + " " + partyInfo.getType() + " Party", "", partyInfo.getImageURL())
                .description(partyInfo.getHostInfo().getPingText() + " is hosting a " + partyInfo.getType() + " party " + partyInfo.getTimestamp())
                .thumbnail(partyInfo.getImageURL());


        embed = embed.addField("Participants:", "", false);

        embed = addUsersToEmbed(partyInfo, embed);

        embed = embed.timestamp(Instant.now());

        return embed.build();
    }

    private static EmbedCreateSpec.Builder addUsersToEmbed(PartyInfo partyInfo, EmbedCreateSpec.Builder embed) {
        if(partyInfo.recipe == null) {
            int personCount = 0;
            for (UserInfo userInfo : partyInfo.getUserList())
            {
                embed = embed.addField("**Test**- Test", "- " + userInfo.getPingText(), false);
                personCount++;
            }

            for(int i = personCount; i < partyInfo.getPeople(); i++)
                embed = embed.addField("", "- Open", false);
        }

        return embed;
    }

    private static String getStatus(PartyInfo partyInfo) {
        String status = "Open";
        if(!partyInfo.isStatus())
            status = "Closed";
        return status;
    }


    public record UserInfo(String id, String name, String recipeRole) implements Serializable {
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

    public String getCommandGuid() {
        return commandGuid;
    }

    public void setCommandGuid(String commandGuid) {
        this.commandGuid = commandGuid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
}
