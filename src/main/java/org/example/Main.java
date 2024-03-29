package org.example;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.*;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.*;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionPresentModalSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.*;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.entity.RestMessage;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.interaction.GuildCommandRegistrar;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    public static final Snowflake SPROUT_ROLE_ID = Snowflake.of("1205616550132981771");
    public static final Snowflake WILD_GARLIC_ROLE_ID = Snowflake.of("1152033973762011166");
    private static final Logger log = LoggerFactory.getLogger(Main.class);


    private static final String token = System.getenv("token");
    public static final long guildId = Long.parseLong(System.getenv("guild_id"));

    static final String CHAT_INPUT_MENTION_PARTY_COMMAND_NAME         = "mentionparty";
    static final String CHAT_INPUT_COMMAND_NAME         = "party";
    static final String CHAT_INPUT_REMOVE_COMMAND_NAME  = "removeuser";
    static final String CHAT_INPUT_CLOSE_COMMAND_NAME   = "closeparty";
    static final String REMOVE_MODAL_ID                 = "removeModal";
    static final String CLOSE_PARTY_ID                  = "closeParty:";

    static final String SIGN_UP_BUTTON_BASEID           = "signup:";
    static final String SIGN_UP_ROLE_BUTTON_BASEID      = "signupRole:";
    static final String DELETE_BUTTON_BASEID            = "delete:";

    static final String REMOVE_USER_SELECT              = "removeUserSelect:";
    static final String REMOVE_USER_BUTTON              = "removeUserButton:";

    static final Set<Snowflake> MOD_ROLES = Set.of(Snowflake.of("1156959674655047783"), Snowflake.of("1152059333916512297"));
    public static final String INTRO_CHANNEL_ID = "1152046915731603487";

    //TODO
    // New command to mention @everyone in a party /mention
    // When closing party, make an @everyone "The party is starting @user1 @user2 @etc...


    public static void main( String[] args )
    {
        Recipes.loadRecipesFromFile();
        FileTask.loadObjectsFromFile();

        log.info("Starting up...");
        DiscordClient.create(token)
                .withGateway(client -> {
                    if(guildId == 0){
                        Mono<Void> handlePingCommand = createPingCommand(client);
                        return handlePingCommand;
                    } else {
                        List<ApplicationCommandRequest> commands = getApplicationCommandRequests();

                        Publisher<?> onChatInput    = getPublisherOnChatInput(client);
                        Publisher<?> onModal        = getPublisherOnModal(client);
                        Publisher<?> onChat         = getPublisherOnChatInputAutoComplete(client);
                        Publisher<?> onButtonPress  = getPublisherOnButtonInteraction(client);
                        Publisher<?> onSelectMenu   = getPublisherOnSelectInteraction(client);
                        Publisher<?> onMessage      = getOnMessage(client);

                        return GuildCommandRegistrar.create(client.getRestClient(), commands)
                                .registerCommands(Snowflake.of(guildId))
                                .thenMany(Mono.when(onChatInput, onModal, onChat, onButtonPress, onMessage, onSelectMenu).doOnError(e -> log.error("Error", e)))
                                .doOnError(e -> log.error("Error", e));
                    }
                })
                .doOnError(e -> log.error("Failed to authenticate with Discord", e))
                .doOnSuccess(result -> log.info("Connected to Discord"))
                .block();
    }

    private static Publisher<?> getPublisherOnSelectInteraction(GatewayDiscordClient client) {
        Publisher<?> onButtonPress = client.on(SelectMenuInteractionEvent.class, selectEvent -> {
            try{
                String selectId = selectEvent.getCustomId();

                if (selectEvent.getCustomId().startsWith(REMOVE_USER_SELECT)) {
                    PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(selectId.split(":")[1]);
                    String userid = selectEvent.getValues().stream().findFirst().orElse("");

                    return selectEvent.edit("Are you sure?")
                            .withEphemeral(true)
                            .withComponents(ActionRow.of(Button.danger(REMOVE_USER_BUTTON + partyInfo.getCommandGuid() + ":" + userid, "Remove")));
                }
            } catch (Exception e){
                log.error("Button Error", e);
            }

            // Ignore it
            return Mono.empty();
        }).doOnError(e -> log.error("Button Error", e));
        return onButtonPress;
    }


    private static Publisher<?> getPublisherOnButtonInteraction(GatewayDiscordClient client) {
        Publisher<?> onButtonPress = client.on(ButtonInteractionEvent.class, buttonEvent -> {
            try{
                String buttonId = buttonEvent.getCustomId();

                if (buttonEvent.getCustomId().startsWith(SIGN_UP_BUTTON_BASEID)) {
                    PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(buttonId.split(":")[1]);
                    String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();
                    String buttonUserName = buttonEvent.getInteraction().getMember().get().getDisplayName();

                    partyInfo.addUser(new UserInfo(buttonUserid, buttonUserName));
                    partyInfo.setMessageid(buttonEvent.getInteraction().getMessageId().get().asLong());
                    return buttonEvent.edit("")
                            .withEmbeds(partyInfo.createEmbed())
                            .withComponents(partyInfo.createButtons().getActionRows())
                            .doOnError(e -> log.error("Button Error", e));
                }

                if (buttonEvent.getCustomId().startsWith(SIGN_UP_ROLE_BUTTON_BASEID)) {
                    PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(buttonId.split(":")[1]);
                    String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();
                    String buttonUserName = buttonEvent.getInteraction().getMember().get().getDisplayName();

                    String buttonValue = "";
                    if(buttonEvent.getCustomId().split(":").length > 2)
                        buttonValue = buttonEvent.getCustomId().split(":")[2];

                    partyInfo.addUser(new UserInfo(buttonUserid, buttonUserName, buttonValue));
                    partyInfo.setMessageid(buttonEvent.getInteraction().getMessageId().get().asLong());

                    return buttonEvent.edit("")
                            .withEmbeds(partyInfo.createEmbed())
                            .withComponents(partyInfo.createButtons().getActionRows())
                            .doOnError(e -> log.error("Button Error", e));
                }

                if (buttonEvent.getCustomId().startsWith(DELETE_BUTTON_BASEID)) {
                    String guid = buttonId.split(":")[1];

                    PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(guid);
                    String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();
                    partyInfo.removeUser(buttonUserid);
                    partyInfo.setMessageid(buttonEvent.getInteraction().getMessageId().get().asLong());

                    return buttonEvent.edit("")
                            .withEmbeds(partyInfo.createEmbed())
                            .withComponents(partyInfo.createButtons().getActionRows())
                            .doOnError(e -> log.error("Button Error", e));
                }

                if (buttonEvent.getCustomId().startsWith(CLOSE_PARTY_ID)) {
                    String guid = buttonId.split(":")[1];
                    String modalGuid = Common.createGUID();
                    PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(guid);
                    partyInfo.setStatus(false);
                    String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();

                    if (buttonUserid.equals(partyInfo.getHostInfo().getId())) {

                        InteractionPresentModalSpec.Builder spec = InteractionPresentModalSpec.builder()
                                .title("Remove User from event: ")
                                .customId(REMOVE_MODAL_ID + "_" + SIGN_UP_BUTTON_BASEID + guid + "_" + modalGuid);

                        int counter = 1;
                        for (UserInfo userInfo : partyInfo.getUserList()) {
                            spec.addComponent(ActionRow.of(TextInput.small(REMOVE_MODAL_ID + "_" + SIGN_UP_BUTTON_BASEID + guid + "_" + userInfo.getId() + "_" + counter, userInfo.getName(), "Removed").required(false).prefilled(userInfo.getName())));
                        }

                    } else {
                        partyInfo.removeUser(buttonUserid);
                    }

                    partyInfo.setMessageid(buttonEvent.getInteraction().getMessageId().get().asLong());
                    return buttonEvent.edit("")
                            .withEmbeds(partyInfo.createEmbed())
                            .withComponents(partyInfo.createButtons().getActionRows())
                            .doOnError(e -> log.error("Button Error", e));

                }

                if (buttonEvent.getCustomId().startsWith(REMOVE_USER_BUTTON)) {
                    String guid = buttonId.split(":")[1];
                    String userid = buttonId.split(":")[2];

                    PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(guid);
                    partyInfo.removeUser(userid);

                    partyInfo.setMessageid(buttonEvent.getInteraction().getMessageId().get().asLong());
                    return buttonEvent.edit("Success").withComponents(new ArrayList<>())
                            .doOnError(e -> log.error("Button Error", e))
                            .doFinally(s -> updateMessage(buttonEvent, partyInfo));

                }
            } catch (Exception e){
                log.error("Button Error", e);
            }

            // Ignore it
            return Mono.empty();
        }).doOnError(e -> log.error("Button Error", e));
        return onButtonPress;
    }

    private static Publisher<?> getPublisherOnChatInputAutoComplete(GatewayDiscordClient client) {
        Publisher<?> onChat = client.on(ChatInputAutoCompleteEvent.class, event -> {
            if (event.getCommandName().equals(CHAT_INPUT_COMMAND_NAME)) {
                // Get the string value of what the user is currently typing
                String typing = event.getFocusedOption().getValue()
                        .map(ApplicationCommandInteractionOptionValue::asString)
                        .orElse("").toLowerCase(); // In case the user has not started typing, we return an empty string

                List<ApplicationCommandOptionChoiceData> suggestions = new ArrayList<>();

                if(event.getFocusedOption().getName().equals("type"))
                {
                    List<TypeInfo> types = TypeInfo.getOrCreateTypeCodes();
                    types.stream()
                            .sorted((s1, s2) -> TypeInfo.compareTypes(s1, s2, typing))
                            .map(s -> ApplicationCommandOptionChoiceData.builder().name(s.getName()).value(s.getCode()).build())
                            .forEach(suggestions::add);
                }

                if(event.getFocusedOption().getName().equals("server"))
                {
                    suggestions.add(ApplicationCommandOptionChoiceData.builder().name("NA").value("NA").build());
                    suggestions.add(ApplicationCommandOptionChoiceData.builder().name("EU").value("EU").build());
                    suggestions.add(ApplicationCommandOptionChoiceData.builder().name("PA").value("PA").build());
                }

                if(event.getFocusedOption().getName().equals("recipe"))
                {
                    Recipes.getRecipeList().stream()
                            .map(Recipe::getRecipeName)
                            .sorted((s1, s2) -> compareTypes(s1, s2, typing))
                            .map(s -> ApplicationCommandOptionChoiceData.builder().name(s).value(normalize(s)).build())
                            .forEach(suggestions::add);
                }

                // Finally, return the list of choices to the user
                return event.respondWithSuggestions(suggestions);
            }



            if(CHAT_INPUT_REMOVE_COMMAND_NAME.equalsIgnoreCase(event.getCommandName()) ||
                    CHAT_INPUT_CLOSE_COMMAND_NAME.equalsIgnoreCase(event.getCommandName()) ||
                    CHAT_INPUT_MENTION_PARTY_COMMAND_NAME.equalsIgnoreCase(event.getCommandName())) {
                List<PartyInfo> partyInfos = PartyInfo.getInfoList();
                Member member = event.getInteraction().getMember().get();

                List<ApplicationCommandOptionChoiceData> suggestions = new ArrayList<>();
                //Show all party infos
                if(isMod(member)){
                    suggestions.addAll(partyInfos.stream()
                            .map(info -> ApplicationCommandOptionChoiceData.builder().name(info.fullNameString()).value(info.getCommandGuid()).build()).toList());
                }

                if(!isMod(member)){
                    suggestions.addAll(partyInfos.stream()
                            .filter(info -> info.getHostInfo().getId().equals(member.getId().asString()))
                            .map(info -> ApplicationCommandOptionChoiceData.builder().name(info.toString()).value(info.getCommandGuid()).build())
                            .toList());
                }

                return event.respondWithSuggestions(suggestions);
            }

            return null;
        });
        return onChat;
    }

    private static Publisher<?> getPublisherOnModal(GatewayDiscordClient client) {
        Publisher<?> onModal = client.on(ModalSubmitInteractionEvent.class, event -> {
            if (event.getCustomId().startsWith(REMOVE_MODAL_ID)) {
                for (TextInput component : event.getComponents(TextInput.class)) {
                    String[] fields = component.getCustomId().split("_");
                    if(fields.length == 3) {
                        String commandGuid = fields[1].split(":")[1];
                        String userId = fields[2];
                        if(!userId.isEmpty() && component.getValue().orElse("").isEmpty()){
                            PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(commandGuid);
                            if(partyInfo != null){
                                partyInfo.removeUser(userId);

                                return event.edit("")
                                        .withEmbeds(partyInfo.createEmbed())
                                        .withComponents(partyInfo.createButtons().getActionRows());
                            }
                        }
                    }

                }
            }
            return Mono.empty();
        });

        return onModal;
    }

    private static Publisher<?> getOnMessage(GatewayDiscordClient client) {
        Publisher<?> onModal = client.on(MessageCreateEvent.class, event -> {
            Message message = event.getMessage();
            if(message.getAuthor().get().isBot()){
                Embed embed = message.getEmbeds().stream().findFirst().orElse(null);
                if(embed != null && embed.getFooter().isPresent()){
                    String id = embed.getFooter().get().getText();
                    PartyInfo info = PartyInfo.getPartyInfoByGuid(id);
                    if(info != null){
                        info.setMessageid(message.getId().asLong());
                        info.setChannelid(message.getChannelId().asLong());

                        PartyInfo.writeInfoList();
                    }
                }
            }

            if(message.getChannelId().asString().equals(INTRO_CHANNEL_ID)) {
                return message.getAuthorAsMember()
                        .filter(m -> m.getRoleIds().contains(SPROUT_ROLE_ID))
                        .flatMap(m -> m.removeRole(SPROUT_ROLE_ID)
                                .then(Mono.defer(() -> m.addRole(WILD_GARLIC_ROLE_ID))));
            }


            return Mono.empty();
        });

        return onModal;
    }

    private static Publisher<?> getPublisherOnChatInput(GatewayDiscordClient client) {
        Publisher<?> onChatInput = client.on(ChatInputInteractionEvent.class, event -> {
            if (CHAT_INPUT_COMMAND_NAME.equals(event.getCommandName())) {
                String commandGuid = Common.createGUID();
                PartyInfo partyInfo = PartyInfo.createFromEvent(event, commandGuid);

                ButtonInfo buttonInfo = partyInfo.createButtons();

                return event.reply()
                        .withEmbeds(partyInfo.createEmbed())
                        .withComponents(buttonInfo.getActionRows());
            }

            //TODO ADD RIGHTS TO THESE COMMANDS ONLY PARTY OWNERS/ADMINS SHOULD BE ABLE TO REMOVE/CLOSE PARTIES

            if(CHAT_INPUT_REMOVE_COMMAND_NAME.equalsIgnoreCase(event.getCommandName())) {
                String guid = event.getOption("partyid").get().getValue().get().asString();
                PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(guid);
                if(partyInfo == null)
                    return event.reply("Invalid party selected").withEphemeral(true);

                Member member = event.getInteraction().getMember().get();

                //Is user the host
                if(partyInfo.isHost(member) || isMod(member)){
                    if(partyInfo.getUserList().isEmpty())
                        return event.reply("There are no users to remove").withEphemeral(true);

                    SelectMenu selectMenu = SelectMenu.of(REMOVE_USER_SELECT + guid, partyInfo.getUserList().stream().distinct().map(user -> SelectMenu.Option.of(user.getName(), user.getId())).toList());

                    return event.reply("Select a user to remove:")
                            .withComponents(ActionRow.of(selectMenu))
                            .withEphemeral(true);
                }

                return event.reply("You do not have access to modify this party").withEphemeral(true);
            }

            if(CHAT_INPUT_CLOSE_COMMAND_NAME.equalsIgnoreCase(event.getCommandName())) {
                String guid = event.getOption("partyid").get().getValue().get().asString();

                PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(guid);
                Member member = event.getInteraction().getMember().get();
                if(partyInfo == null)
                    return event.reply("Invalid party selected").withEphemeral(true);

                //Is user the host
                if(partyInfo.isHost(member) || isMod(member)){
                    partyInfo.setStatus(false);

                    PartyInfo.removeClosedParties();

                    String hostName = partyInfo.getHostInfo().getName() + "'s " + partyInfo.getPartyNameForEmbed() + " party is starting! <:OrangeCatHype:1162198496372334653> \r\n";
                    String userPings = partyInfo.getUserList().stream().distinct().map(UserInfo::getPingText).collect(Collectors.joining(" "));
                    return event.reply().withContent(hostName + userPings)
                            .doFinally(s -> updateMessage(event, partyInfo));
                }
                return event.reply("You do not have access to modify this party").withEphemeral(true);
            }

            if(CHAT_INPUT_MENTION_PARTY_COMMAND_NAME.equalsIgnoreCase(event.getCommandName())) {
                String guid = event.getOption("partyid").get().getValue().get().asString();

                PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(guid);
                Member member = event.getInteraction().getMember().get();
                if(partyInfo == null)
                    return event.reply("Invalid party selected").withEphemeral(true);

                //Is user the host
                if(partyInfo.isHost(member) || isMod(member)){
                    List<UserInfo> userInfo = partyInfo.getUserList();
                    userInfo.add(partyInfo.getHostInfo());
                    String userPings = partyInfo.getUserList().stream().distinct().map(UserInfo::getPingText).collect(Collectors.joining(" "));


                    if(userPings.isEmpty())
                        return event.reply("No party members found").withEphemeral(true);

                    return event.reply().withContent(userPings)
                            .doFinally(s -> updateMessage(event, partyInfo));
                }
                return event.reply("You do not have access to modify this party").withEphemeral(true);
            }
            return Mono.empty();
        });
        return onChatInput;
    }

    private static void updateMessage(DeferrableInteractionEvent event, PartyInfo partyInfo){
        {
            // Retrieve the message using the message ID

            try{

                RestChannel restChannel = event.getClient()
                        .getChannelById(Snowflake.of(partyInfo.getChannelid())).block()
                        .getRestChannel();


                Id messageId = restChannel.getData().block().lastMessageId().get().orElse(null);
                if(messageId != null){
                    RestMessage restMessage = null;

                    //Button interactions set the message id
                    if(partyInfo.getMessageid() > 0)
                        restMessage = restChannel.getRestMessage(Snowflake.of(partyInfo.getMessageid()));

                    //If no buttons were pressed, this will trigger. Message might be too far back to find, but pressing a button and closing again would fix.
                    if(partyInfo.getMessageid() == 0 || restMessage == null){

                        for (MessageData message : restChannel.getMessagesBefore(Snowflake.of(messageId)).collectList().block()) {
                            if(message.author().bot().get()) {
                                boolean correctMessage = false;
                                EmbedData existingEmbed = message.embeds().stream().findFirst().orElse(null);
                                if(existingEmbed != null && existingEmbed.footer().get() != null){
                                    String id = existingEmbed.footer().get().text();
                                    correctMessage = id.equals(partyInfo.getCommandGuid());
                                }

                                if(correctMessage) {
                                    restMessage = restChannel.getRestMessage(Snowflake.of(message.id()));
                                }
                            }
                        }
                    }

                    if(restMessage != null) {
                        // Create a new Embed with updated information
                        EmbedCreateSpec embed = partyInfo.createEmbed();

                        List<ComponentData> buttons = new ArrayList<>();
                        if(partyInfo.isStatus()){
                            ButtonInfo buttonInfo = partyInfo.createButtons();
                            buttons.addAll(buttonInfo.getActionRows().stream().map(MessageComponent::getData).toList());
                        }

                        MessageData updatedMessage = restMessage.edit(MessageEditRequest.builder().embeds(List.of(embed.asRequest())).components(buttons).build()).block();
                    }

                    if(restMessage == null) {
                        if(restChannel.getData().block() == null){
                            partyInfo.setStatus(false);
                            PartyInfo.removeClosedParties();
                        }
                    }
                }
            } catch (ClientException e) {
                partyInfo.setStatus(false);
                PartyInfo.removeClosedParties();
            }
        }
    }

    private static List<ApplicationCommandRequest> getApplicationCommandRequests() {
        List<ApplicationCommandRequest> commands = new ArrayList<>();

        ApplicationCommandRequest partyCommand = ApplicationCommandRequest.builder()
                .name(CHAT_INPUT_COMMAND_NAME)
                .addAllOptions(getPartyCommandOptionData())
                .description("Create a party for Palia")
                .build();

        ApplicationCommandRequest removeCommand = ApplicationCommandRequest.builder()
                .name(CHAT_INPUT_REMOVE_COMMAND_NAME)
                .addAllOptions(getRemoveUserCommandOptionData())
                .description("Remove user from a party")
                .build();

        //Removes button, (Open) becomes closed
        ApplicationCommandRequest closeCommand = ApplicationCommandRequest.builder()
                .name(CHAT_INPUT_CLOSE_COMMAND_NAME)
                .addAllOptions(getClosePartyCommandOptionData())
                .description("Closes party")
                .build();

        //Mentions everyone in a specified party
        ApplicationCommandRequest mentionCommand = ApplicationCommandRequest.builder()
                .name(CHAT_INPUT_MENTION_PARTY_COMMAND_NAME)
                .addAllOptions(getClosePartyCommandOptionData())
                .description("Mentions users in party")
                .build();

        commands.add(partyCommand);
        commands.add(removeCommand);
        commands.add(closeCommand);
        commands.add(mentionCommand);
        return commands;
    }

    public static String normalize(String string){
        return Common.removeSpaces(string).toLowerCase();
    }

    public static int compareTypes(String s1, String s2, String comparison){
        String name1 = s1.toLowerCase();
        String name2 = s2.toLowerCase();

        if(name1.startsWith(comparison)) return -1;
        if(name2.startsWith(comparison)) return 1;

        return -1 * Integer.compare(FuzzySearch.ratio(name1, comparison), FuzzySearch.ratio(name2, comparison));
    }

    record ButtonInfo(Mono<Void> listener, List<Button> buttons){
        public List<LayoutComponent> getActionRows(){
            if(buttons.size() <= 5){
                return List.of(ActionRow.of(buttons));
            }

            List<LayoutComponent> actionRows = new ArrayList<>();
            try{
                for(int i = 0; i < buttons.size(); i+= 5){
                    actionRows.add(ActionRow.of(buttons.subList(i, i + Math.min(5, buttons.size()-i))));
                }
            } catch (Exception e){
                log.error("Button Action Row Error", e);
            }

            return actionRows;
        }
    }

    private static List<ApplicationCommandOptionData> getPartyCommandOptionData() {
        List<ApplicationCommandOptionData> options = new ArrayList<>();

        List<ApplicationCommandOptionChoiceData> voiceOptions = List.of(ApplicationCommandOptionChoiceData.builder().name("Voice Chat/Muted Required").value(true).build()
                , ApplicationCommandOptionChoiceData.builder().name("No Voice Chat Required").value(false).build());

        options.add(ApplicationCommandOptionData.builder().name("type").description("type").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());
        options.add(ApplicationCommandOptionData.builder().name("server").description("server").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("people").description("Max # of Participants for a Non Cooking Party").type(ApplicationCommandOption.Type.INTEGER.getValue()).autocomplete(false).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("timestamp").description("Timestamp").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(false).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("recipe").description("Recipe").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("quantity").description("Recipe Quantity").type(ApplicationCommandOption.Type.INTEGER.getValue()).autocomplete(false).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("voice").description("Voice Chat Options").type(ApplicationCommandOption.Type.BOOLEAN.getValue()).autocomplete(false).choices(voiceOptions).required(false).build());

        return options;
    }

    private static List<ApplicationCommandOptionData> getRemoveUserCommandOptionData() {
        List<ApplicationCommandOptionData> options = new ArrayList<>();

        options.add(ApplicationCommandOptionData.builder().name("partyid").description("Party").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());

        return options;
    }

    private static List<ApplicationCommandOptionData> getClosePartyCommandOptionData() {
        List<ApplicationCommandOptionData> options = new ArrayList<>();

        options.add(ApplicationCommandOptionData.builder().name("partyid").description("Party").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());

        return options;
    }


    private static boolean isMod(Member member){
        log.info(member.getRoleIds().toString());
        log.info(MOD_ROLES.toString());
        return member.getRoleIds().stream().anyMatch(MOD_ROLES::contains);
    }

    private static Mono<Void> createPingCommand(GatewayDiscordClient gateway) {
        return gateway.on(MessageCreateEvent.class, event -> {
            Message message = event.getMessage();
            Optional<User> userOptional = message.getAuthor();
            if(userOptional.isPresent()){
                User user = userOptional.get();
                System.out.println(user.getUsername() + ": " + message.getContent());
            }

            System.out.println(message.getGuildId().get().asString());
            if (message.getContent().equals("!ping"))
                return message.getChannel().flatMap(channel -> channel.createMessage("pong!"));

            return Mono.empty();
        }).then();
    }
}