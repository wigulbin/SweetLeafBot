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
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionPresentModalSpec;
import discord4j.core.spec.InteractionReplyEditMono;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.UserData;
import discord4j.rest.interaction.GuildCommandRegistrar;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);


    private static final String token = System.getenv("token");
    public static final long guildId = Long.parseLong(System.getenv("guild_id"));
//    private static final long guildId = Long.parseLong("1151985071272763452");

    static final String CHAT_INPUT_COMMAND_NAME = "party";
    static final String CHAT_INPUT_REMOVE_COMMAND_NAME = "removeuser";
    static final String CHAT_INPUT_CLOSE_COMMAND_NAME = "closeparty";
    static final String REMOVE_MODAL_ID = "removeModal";
    static final String CLOSE_PARTY_ID = "closeParty:";

    static final String SIGN_UP_BUTTON_BASEID = "signup:";
    static final String SIGN_UP_ROLE_BUTTON_BASEID = "signupRole:";
    static final String DELETE_BUTTON_BASEID = "delete:";

    static final String REMOVE_USER_SELECT = "removeUserSelect:";
    static final String REMOVE_USER_BUTTON = "removeUserButton:";

    static final Set<Snowflake> MOD_ROLES = Set.of(Snowflake.of("1156959674655047783"), Snowflake.of("1152059333916512297"));
    public static final String BOT_NAME = "Silly Lil Bot";

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

                    String buttonUserName = buttonEvent.getInteraction().getUser().getGlobalName().get();
                    String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();

                    if(buttonEvent.getInteraction().getMember().isPresent())
                        buttonUserName = buttonEvent.getInteraction().getMember().get().getDisplayName();

                    partyInfo.addUser(new UserInfo(buttonUserid, buttonUserName));
                    return buttonEvent.edit("")
                            .withEmbeds(partyInfo.createEmbed())
                            .doOnError(e -> log.error("Button Error", e));
                }

                if (buttonEvent.getCustomId().startsWith(SIGN_UP_ROLE_BUTTON_BASEID)) {
                    PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(buttonId.split(":")[1]);
                    String buttonUserName = buttonEvent.getInteraction().getUser().getGlobalName().get();
                    String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();

                    if(buttonEvent.getInteraction().getMember().isPresent())
                        buttonUserName = buttonEvent.getInteraction().getMember().get().getDisplayName();

                    String buttonValue = "";
                    if(buttonEvent.getCustomId().split(":").length > 2)
                        buttonValue = buttonEvent.getCustomId().split(":")[2];

                    partyInfo.addUser(new UserInfo(buttonUserid, buttonUserName, buttonValue));

                    return buttonEvent.edit("")
                            .withEmbeds(partyInfo.createEmbed())
                            .doOnError(e -> log.error("Button Error", e));
                }

                if (buttonEvent.getCustomId().startsWith(DELETE_BUTTON_BASEID)) {
                    String guid = buttonId.split(":")[1];

                    PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(guid);
                    String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();
                    partyInfo.removeUser(buttonUserid);

                    return buttonEvent.edit("")
                            .withEmbeds(partyInfo.createEmbed())
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

                    return buttonEvent.edit("")
                            .withEmbeds(partyInfo.createEmbed())
                            .doOnError(e -> log.error("Button Error", e));

                }

                if (buttonEvent.getCustomId().startsWith(REMOVE_USER_BUTTON)) {
                    String guid = buttonId.split(":")[1];
                    String userid = buttonId.split(":")[2];

                    PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(guid);
                    partyInfo.removeUser(userid);

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



            if(CHAT_INPUT_REMOVE_COMMAND_NAME.equalsIgnoreCase(event.getCommandName()) || CHAT_INPUT_CLOSE_COMMAND_NAME.equalsIgnoreCase(event.getCommandName())) {
                List<PartyInfo> partyInfos = PartyInfo.getInfoList();
                Member member = event.getInteraction().getMember().get();

                List<ApplicationCommandOptionChoiceData> suggestions = new ArrayList<>();
                //Show all party infos
                if(member.getRoleIds().stream().anyMatch(MOD_ROLES::contains)){
                    suggestions.addAll(partyInfos.stream()
                            .map(info -> ApplicationCommandOptionChoiceData.builder().name(info.fullNameString()).value(info.getCommandGuid()).build()).toList());
                }

                if(member.getRoleIds().stream().noneMatch(MOD_ROLES::contains)){
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
                                        .withEmbeds(partyInfo.createEmbed());
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
            System.out.println(message.getAuthor().get().getUsername());
            System.out.println(message.getContent());


            if(message.getAuthor().get().getUsername().equals(BOT_NAME)){
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



            return Mono.empty();
        });

        return onModal;
    }

    private static Publisher<?> getPublisherOnChatInput(GatewayDiscordClient client) {
        Publisher<?> onChatInput = client.on(ChatInputInteractionEvent.class, event -> {
            if (CHAT_INPUT_COMMAND_NAME.equals(event.getCommandName())) {
                String commandGuid = Common.createGUID();
                PartyInfo partyInfo = PartyInfo.createFromEvent(event, commandGuid);

                ButtonInfo buttonInfo = createButtons(partyInfo, commandGuid);

                return event.reply()
                        .withEmbeds(partyInfo.createEmbed())
                        .withComponents(buttonInfo.getActionRows());
            }

            if(CHAT_INPUT_REMOVE_COMMAND_NAME.equalsIgnoreCase(event.getCommandName())) {
                String guid = event.getOption("partyid").get().getValue().get().asString();
                PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(guid);

                if(partyInfo.getUserList().isEmpty())
                    return event.reply("There are no users to remove").withEphemeral(true);

                SelectMenu selectMenu = SelectMenu.of(REMOVE_USER_SELECT + guid, partyInfo.getUserList().stream().map(user -> SelectMenu.Option.of(user.getName(), user.getId())).toList());

                return event.reply("Select a user to remove:")
                        .withComponents(ActionRow.of(selectMenu))
                        .withEphemeral(true);
            }

            if(CHAT_INPUT_CLOSE_COMMAND_NAME.equalsIgnoreCase(event.getCommandName())) {
                String guid = event.getOption("partyid").get().getValue().get().asString();

                PartyInfo partyInfo = PartyInfo.getPartyInfoByGuid(guid);
                partyInfo.setStatus(false);
                return event.reply().withEphemeral(true).withContent("Closing " + partyInfo + "...")
                        .doFinally(s -> updateMessage(event, partyInfo));
            }
            return Mono.empty();
        });
        return onChatInput;
    }

    private static void updateMessage(DeferrableInteractionEvent event, PartyInfo partyInfo){
        {
            long messageId = partyInfo.getMessageid(); // Replace with the actual message ID

            // Retrieve the message using the message ID
            Channel channel = event.getClient().getChannelById(Snowflake.of(partyInfo.getChannelid())).block();
            if (channel instanceof TextChannel textChannel) {
                textChannel.getMessageById(Snowflake.of(messageId))
                        .flatMap(message -> {
                            // Create a new Embed with updated information
                            EmbedCreateSpec embed = partyInfo.createEmbed();

                            List<LayoutComponent> buttons = new ArrayList<>();
                            if(partyInfo.isStatus()){
                                ButtonInfo buttonInfo = createButtons(partyInfo, partyInfo.getCommandGuid());
                                buttons.addAll(buttonInfo.getActionRows());
                            }

                            // Update the message with the new Embed
                            return message.edit(MessageEditSpec.builder().embeds(List.of(embed)).components(buttons).build());
                        }).subscribe();
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

        commands.add(partyCommand);
        commands.add(removeCommand);
        commands.add(closeCommand);
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


    private static ButtonInfo createButtons(PartyInfo partyInfo, String commandGuid) {
        List<Button> buttons = new ArrayList<>();

        String signUpButtonGUID     = "signup:" + commandGuid;
        String signUpRoleButtonGUIDBase = "signupRole:" + commandGuid + ":";
        String deleteButtonGUID     = "delete:" + commandGuid;

        List<Button> roleButtons = new ArrayList<>();
        if(partyInfo.getRecipe() != null) {
            Recipe recipe = partyInfo.getRecipe();
            int id = 0;
            for (RecipeRole role : recipe.getRoles()) {
                roleButtons.add(Button.primary(signUpRoleButtonGUIDBase + (id++), role.getRoleName()));
            }
        } else {
            Button button = Button.primary(signUpButtonGUID, "Sign Up!");
            buttons.add(button);
        }

        buttons.addAll(roleButtons);

        Button deleteButton = Button.danger(deleteButtonGUID, "Remove Name");
        buttons.add(deleteButton);

        return new ButtonInfo(null, buttons);
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

        options.add(ApplicationCommandOptionData.builder().name("type").description("type").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());
        options.add(ApplicationCommandOptionData.builder().name("server").description("server").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());
        options.add(ApplicationCommandOptionData.builder().name("people").description("# of People").type(ApplicationCommandOption.Type.INTEGER.getValue()).autocomplete(false).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("timestamp").description("Timestamp").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(false).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("recipe").description("Recipe").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("quantity").description("Recipe Quantity").type(ApplicationCommandOption.Type.INTEGER.getValue()).autocomplete(false).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("voice").description("Voice Party").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(false).required(false).build());

        return options;
    }

    private static List<ApplicationCommandOptionData> getRemoveUserCommandOptionData() {
        List<ApplicationCommandOptionData> options = new ArrayList<>();

        options.add(ApplicationCommandOptionData.builder().name("partyid").description("Party").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());

//        options.add(ApplicationCommandOptionData.builder().name("user").description("user").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());

        return options;
    }

    private static List<ApplicationCommandOptionData> getClosePartyCommandOptionData() {
        List<ApplicationCommandOptionData> options = new ArrayList<>();

        options.add(ApplicationCommandOptionData.builder().name("partyid").description("Party").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());

        return options;
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