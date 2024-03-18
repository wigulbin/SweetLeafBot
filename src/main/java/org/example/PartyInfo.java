package org.example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.stream.Collectors;

@JsonIgnoreProperties
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

    private Recipe recipe;


    private List<UserInfo> userList;

    private static final Logger log = LoggerFactory.getLogger(PartyInfo.class);
    public PartyInfo(){}
    public PartyInfo(TypeInfo type, UserInfo hostInfo, long people, String server, boolean status, String commandGuid, String timestamp, Recipe recipe, long quantity, boolean voice, long channelid)
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
        this.channelid = channelid;

        this.userList = Collections.synchronizedList(new ArrayList<>());
    }

    private static List<PartyInfo> infoList = new ArrayList<>();
    private static AtomicBoolean changed = new AtomicBoolean(true);

    public static void writeInfoList(){
        if(changed.get()){
            new Thread(() -> Fileable.write(PartyInfo.class, PartyInfo.getInfoList())).start();
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

    public  static List<PartyInfo> getInfoList(){
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

    /*
    - Overprep
        1. If overprep flag is set on party, look through recipe items to see if any of them have the overprep flag
        2. If that exists, create new "role" for overprep
            - There will be a button/section for overpreppers
        3. They will need to write what they will overprep
        4. They can only overprep one thing
        5. They will click Overprep button
            - Response will be ephemeral with action row of available items that can be overprepped
            - After selecting on, buttons will vanish. PartyInfo will display "Username - item" under the overprep section

     - Starter role can only be one person

     - Remove button display row of buttons with each role that user is currently in
     */
    public static PartyInfo createFromEvent(ChatInputInteractionEvent event, String modalGuid) {
        String userName = event.getInteraction().getUser().getGlobalName().get();
        String userid = event.getInteraction().getUser().getId().asString();

        if(event.getInteraction().getMember().isPresent())
            userName = event.getInteraction().getMember().get().getDisplayName();


        TypeInfo type = TypeInfo.getType(event.getOption("type").get().getValue().get().asString());
        long people = getPeople(event);
        long quantity = getQuantity(event);

        String timestamp = getTimestamp(event);

        String server = "";
        if(event.getOption("server").isPresent()) server = event.getOption("server").get().getValue().get().asString();

        long finalPeople = people;

        UserInfo userInfo = new UserInfo(userid, userName, "");
        Recipe recipe = getRecipe(event);
        boolean voice = event.getOption("voice").isPresent() && event.getOption("voice").get().getValue().get().asBoolean();
        long channelid = event.getInteraction().getChannelId().asLong();

        PartyInfo info = new PartyInfo(type, userInfo, finalPeople, server, true, modalGuid, timestamp, recipe, quantity, voice, channelid);
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
        boolean isCooking = this.getType().getName().equals("Cooking");
        if(!userInfo.getId().equals(hostInfo.getId()) || isCooking) { // Hosts cannot signup for own party, unless it's a cooking party
            if(isCooking || !hasRole(userInfo.getRecipeRole())) {     // Cooking can have the same person sign up multiple times, others cannot
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

        String partyName = getPartyNameForEmbed();

        String description = partyInfo.getHostInfo().getPingText() + " is hosting a " + partyName + " party " + partyInfo.getTimestamp();
        if(partyInfo.isVoice())
            description += "\r\n:microphone2: Join VC/Muted";

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

    @JsonIgnore
    public String getPartyNameForEmbed() {
        String partyName = this.getType().toString();
        if(this.getRecipe() != null) partyName = this.quantity + "x " + this.getRecipe().getRecipeName();
        return partyName;
    }

    private static EmbedCreateSpec.Builder addUsersToEmbed(PartyInfo partyInfo, EmbedCreateSpec.Builder embed) {
        if(partyInfo.recipe == null) {
            int personCount = 0;

            List<String> fieldUsers = new ArrayList<>();
            for (UserInfo userInfo : partyInfo.getUserList())
            {
                fieldUsers.add(userInfo.getPingText());
                personCount++;
            }

            for(int i = personCount; i < partyInfo.getPeople(); i++)
                fieldUsers.add("Open");

            embed = embed.addField("", fieldUsers.stream().map(u -> "- " + u).collect(Collectors.joining("\r\n")), false);
        }
//<:AdorableFrog:937754121795174420>
        if(partyInfo.recipe != null) {
            Recipe recipe = partyInfo.recipe;
            int roleCounter = 0;
            for (RecipeRole role : recipe.getRoles()) {
                embed = embed.addField(role.getRoleName() + " - " + role.getStation(), role.getBringDisplayString(partyInfo.quantity), false);

                int personCount = 0;
                List<String> fieldUsers = new ArrayList<>();
                for (UserInfo userInfo : partyInfo.getUserList())
                {
                    if(userInfo.getRecipeRole().equals(roleCounter + "")){
                        personCount++;
                        fieldUsers.add(personCount + ". " + userInfo.getPingText());
                    }
                }

                if(personCount == 0)
                    embed = embed.addField("", "1. \r\n", false);

                if(personCount > 0){
                    embed = embed.addField("", String.join("\r\n", fieldUsers) + "\r\n", false);
                }

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
