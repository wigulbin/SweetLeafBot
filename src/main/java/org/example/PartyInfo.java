package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PartyInfo implements Serializable
{
    private TypeInfo type;
    private UserInfo hostInfo;
    private long people;
    private String server;
    private boolean status;
    private String commandGuid;
    private String timestamp = "";
    private long quantity;
    private LocalDateTime created;
    private long messageid;
    private long channelid;
    private boolean voice;
    private boolean multiRoles;

    private Recipe recipe;


    private List<UserInfo> userList;

    private static final Logger log = LoggerFactory.getLogger(PartyInfo.class);
    public PartyInfo(){}
    public PartyInfo(TypeInfo type, UserInfo hostInfo, long people, String server, boolean status, String commandGuid, String timestamp, Recipe recipe, long quantity, boolean voice, boolean multiRoles)
    {
        this.type = type;
        this.hostInfo = hostInfo;
        this.people = people;
        this.server = server;
        this.status = status;
        this.commandGuid = commandGuid;
        this.timestamp = timestamp;
        this.recipe = recipe;
        this.quantity = quantity;
        this.created = LocalDateTime.now();
        this.voice = voice;
        this.multiRoles = multiRoles;

        this.userList = Collections.synchronizedList(new ArrayList<>());
    }

    private static List<PartyInfo> infoList = new ArrayList<>();
    private static AtomicBoolean changed = new AtomicBoolean(true);

    public synchronized static void writeInfoList(){
        if(changed.get()){
            Fileable.write(PartyInfo.class, PartyInfo.getInfoList());
            changed.set(false);
        }
    }

    public static void updateInfoList(){
        changed.set(true);
    }

    public static void addToInfoList(PartyInfo info){
        infoList.add(info);
        changed.set(true);
    }

    public synchronized static List<PartyInfo> getInfoList(){
        if(infoList.isEmpty()) {
            List<PartyInfo> result = Fileable.readFromFile(PartyInfo.class.getSimpleName(), new TypeReference<List<PartyInfo>>() {});
            if(result != null) infoList.addAll(result);
        }


        return new ArrayList<>(infoList);
    }

    public static PartyInfo getPartyInfoByGuid(String guid){
        return getInfoList().stream().filter(info -> info.getCommandGuid().equals(guid)).findFirst().orElse(null);
    }

    public static void removeClosedParties(){
        infoList.removeIf(info -> !info.isStatus());
        changed.set(true);
        writeInfoList();
    }

    public boolean isHost(Member member){
        if(member == null) return false;

        return member.getId().asString().equals(getHostInfo().getId());
    }

    @Override
    public String toString() {
        return type + " created on " + created.format(DateTimeFormatter.ofPattern("dd/MM/yy hh:mm a"));
    }

    public String fullNameString() {
        return type + " created on " + created.format(DateTimeFormatter.ofPattern("dd/MM/yy hh:mm a")) + " by " + hostInfo.getName();
    }

    public static PartyInfo createFromEvent(ChatInputInteractionEvent event, String modalGuid) {
        String userName = event.getInteraction().getUser().getGlobalName().get();
        String userid = event.getInteraction().getUser().getId().asString();

        if(event.getInteraction().getMember().isPresent())
            userName = event.getInteraction().getMember().get().getDisplayName();


        TypeInfo type = TypeInfo.getType(event.getOption("type").get().getValue().get().asString());
        long people = getPeople(event);
        long quantity = getQuantity(event);

        String timestamp = getTimestamp(event);

        String server = event.getOption("server").get().getValue().get().asString();

        long finalPeople = people;

        UserInfo userInfo = new UserInfo(userid, userName, "");
        Recipe recipe = getRecipe(event);
        boolean voice = event.getOption("voice").isPresent() && event.getOption("voice").get().getValue().get().asBoolean();
        boolean multiRole = event.getOption("multiroles").isPresent() && event.getOption("multiroles").get().getValue().get().asBoolean();

        PartyInfo info = new PartyInfo(type, userInfo, finalPeople, server, true, modalGuid, timestamp, recipe, quantity, voice, multiRole);
        addToInfoList(info);
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

    private static long getQuantity(ChatInputInteractionEvent event) {
        long quantity = 1;
        if(event.getOption("quantity").isPresent()) quantity = event.getOption("quantity").get().getValue().get().asLong();
        return quantity;
    }


    String getImageURL() {
        return type.getImageURL();
    }

    Color getColor() {
        return type.getColor();
    }

    public synchronized void addUser(UserInfo userInfo){
        if(!userInfo.getId().equals(hostInfo.getId())) { //Hosts cannot signup for own party
            if(multiRoles || !hasRole(userInfo.getRecipeRole())) { //If party is set to someone can only signup once
                userList.add(userInfo);
                updateInfoList();
            }
        }
    }

    public boolean hasRole(String role) {
        return userList.stream().anyMatch(user -> user.getRecipeRole().equals(role));
    }

    public void removeUser(String userid){
        userList.removeIf(user -> user.getId().equals(userid));
        updateInfoList();
    }




    public Main.ButtonInfo createButtons() {
        PartyInfo partyInfo = this;
        List<Button> buttons = new ArrayList<>();

        String signUpButtonGUID         = "signup:" + commandGuid;
        String signUpRoleButtonGUIDBase = "signupRole:" + commandGuid + ":";
        String deleteButtonGUID         = "delete:" + commandGuid;

        List<Button> roleButtons = new ArrayList<>();
        if(partyInfo.getRecipe() != null) {
            Recipe recipe = partyInfo.getRecipe();
            int id = 0;
            for (RecipeRole role : recipe.getRoles()) {
                if(multiRoles || !this.hasRole(id + ""))
                    roleButtons.add(Button.primary(signUpRoleButtonGUIDBase + (id), role.getRoleName()));

                id++;
            }
        } else {
            Button button = Button.primary(signUpButtonGUID, "Sign Up!");
            buttons.add(button);
        }

        buttons.addAll(roleButtons);

        Button deleteButton = Button.danger(deleteButtonGUID, "Remove Name");
        buttons.add(deleteButton);

        return new Main.ButtonInfo(null, buttons);
    }

    public EmbedCreateSpec createEmbed()
    {
        PartyInfo partyInfo = this;
        String status = getStatus(partyInfo);

        String partyName = partyInfo.getType().toString();
        if(partyInfo.getRecipe() != null) partyName = partyInfo.quantity + "x " + partyInfo.getRecipe().getRecipeName();

        String description = partyInfo.getHostInfo().getPingText() + " is hosting a " + partyName + " party " + partyInfo.getTimestamp();
        if(partyInfo.isVoice())
                description += "\r\n:microphone2: Microphone required";

        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                .color(partyInfo.getColor())
                .title("Hosted by " + partyInfo.getHostInfo().getName() + "")
                .author( "(" + status + ") " + partyInfo.getServer() + " " + partyInfo.getType() + " Party", "", partyInfo.getImageURL())
                .description(description);

        if(partyInfo.getRecipe() == null)
            embed = embed.addField("Participants:", "", false);

        embed = addUsersToEmbed(partyInfo, embed);
        embed = embed.thumbnail(partyInfo.getImageURL());

        embed = embed.timestamp(Instant.now());
        embed = embed.footer(getCommandGuid(), "");

        return embed.build();
    }

    private static EmbedCreateSpec.Builder addUsersToEmbed(PartyInfo partyInfo, EmbedCreateSpec.Builder embed) {
        if(partyInfo.recipe == null) {
            int personCount = 0;
            for (UserInfo userInfo : partyInfo.getUserList())
            {
                embed = embed.addField("", "- " + userInfo.getPingText(), false);
                personCount++;
            }

            for(int i = personCount; i < partyInfo.getPeople(); i++)
                embed = embed.addField("", "- Open", false);
        }
//<:AdorableFrog:937754121795174420>
        if(partyInfo.recipe != null) {
            Recipe recipe = partyInfo.recipe;
            int roleCounter = 0;
            for (RecipeRole role : recipe.getRoles()) {
                embed = embed.addField(role.getRoleName() + " - " + role.getStation(), role.getBringDisplayString(partyInfo.quantity), false);

                int personCount = 0;
                for (UserInfo userInfo : partyInfo.getUserList())
                {
                    if(userInfo.getRecipeRole().equals(roleCounter + "")){
                        personCount++;
                        embed.addField("", personCount + ": " + userInfo.getPingText(), false);
                    }
                }

                if(personCount == 0)
                    embed = embed.addField("", "1: ", false);

                embed = embed.addField("", " ", false);
                roleCounter++;
            }
        }

        return embed;
    }

    private static String getStatus(PartyInfo partyInfo) {
        String status = "Open";
        if(!partyInfo.isStatus())
            status = "Closed";
        return status;
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

    public long getMessageid() {
        return messageid;
    }

    public void setMessageid(long messageid) {
        this.messageid = messageid;
    }

    public long getChannelid() {
        return channelid;
    }

    public void setChannelid(long channelid) {
        this.channelid = channelid;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public boolean isVoice()
    {
        return voice;
    }

    public void setVoice(boolean voice)
    {
        this.voice = voice;
    }
}
