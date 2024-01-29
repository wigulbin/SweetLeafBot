package org.example;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.*;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionPresentModalSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.interaction.GuildCommandRegistrar;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class Main {
    private static final String token = System.getenv("token");
    //    private static final long guildId = Long.parseLong("937741716042174486");
    private static final long guildId = Long.parseLong("1151985071272763452");

    static final String CHAT_INPUT_COMMAND_NAME = "party";
    static final String MODAL_CUSTOM_ID = "my-modal";
    static final String REMOVE_MODAL_ID = "removeModal";
    static final String SELECT_CUSTOM_ID = "my-select";
    static final String INPUT_CUSTOM_ID = "my-input";

    //  "discordAPIKey": "NDM4MTI2OTU4NjgyMDQ2NDY1.GfErSC.vHSjHQG0Uiuchv5WzbRCxAtc9WJadDc3SZL6aA"
    public static void main( String[] args )
    {
        Recipes.loadRecipesFromFile();
        FileTask.loadObjectsFromFile();

        DiscordClient.create(JSONFile.getJSONValueFromFile("discordAPIKey", "keys.json"))
                .withGateway(client -> {
                    if(guildId == 0){
                        Mono<Void> handlePingCommand = createPingCommand(client);
                        return handlePingCommand;
                    } else {
                        System.out.println(client);
                        List<ApplicationCommandOptionData> options = getApplicationCommandOptionData();

                        ApplicationCommandRequest example = ApplicationCommandRequest.builder()
                                .name(CHAT_INPUT_COMMAND_NAME)
                                .addAllOptions(options)
                                .description("Create a party for Palia")
                                .build();

                        List<ApplicationCommandRequest> commands = Collections.singletonList(example);

                        Publisher<?> onChatInput = client.on(ChatInputInteractionEvent.class, event -> {
                            if (CHAT_INPUT_COMMAND_NAME.equals(event.getCommandName())) {
                                String commandGuid = Common.createGUID();
                                PartyInfo partyInfo = PartyInfo.createFromEvent(event, commandGuid);

                                ButtonInfo buttonInfo = createButtons(client, partyInfo, commandGuid);

                                return event.reply()
                                        .withEmbeds(partyInfo.createEmbed())
//                                    .withComponents(ActionRow.of(buttonInfo.buttons))
                                        .withComponents(buttonInfo.getActionRows())
                                        .then(buttonInfo.listener);
                            }
                            return Mono.empty();
                        });

                        Publisher<?> onModal = client.on(ModalSubmitInteractionEvent.class, event -> {
                            if (REMOVE_MODAL_ID.equals(event.getCustomId())) {
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
//                                                    .withComponents(ActionRow.of(button, deleteButton));
                                            }
                                        }
                                    }

                                }
                            }
                            return Mono.empty();
                        });

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
                            return null;
                        });

                        return GuildCommandRegistrar.create(client.getRestClient(), commands)
                                .registerCommands(Snowflake.of(guildId))
//                            .thenMany(Mono.when(onChatInput, onModal));
                                .thenMany(Mono.when(onChatInput, onModal, onChat));
                    }
                })
                .block();
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


    private static ButtonInfo createButtons(GatewayDiscordClient client, PartyInfo partyInfo, String commandGuid) {
        List<Button> buttons = new ArrayList<>();

        String signUpButtonGUID     = "signup:" + commandGuid;
        String signUpRoleButtonGUIDBase = "signupRole:" + commandGuid + ":";
        String deleteButtonGUID     = "delete:" + commandGuid;

        List<Button> roleButtons = new ArrayList<>();
        if(partyInfo.getRecipe() != null) {
            Recipe recipe = partyInfo.getRecipe();
            System.out.println(recipe.getRecipeName());
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

        // Issues with adding self to multiple roles (got stuck with 4 and could not add more)
        Mono<Void> buttonListener = client.on(ButtonInteractionEvent.class, buttonEvent -> {
                    if (buttonEvent.getCustomId().equals(signUpButtonGUID)) {
                        String buttonUserName = buttonEvent.getInteraction().getUser().getGlobalName().get();
                        String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();

                        partyInfo.addUser(new PartyInfo.UserInfo(buttonUserid, buttonUserName, ""));
                        return buttonEvent.edit("")
                                .withEmbeds(partyInfo.createEmbed());
//                                .withComponents(ActionRow.of(buttons));
                    }

                    if (buttonEvent.getCustomId().startsWith(signUpRoleButtonGUIDBase)) {
                        String buttonUserName = buttonEvent.getInteraction().getUser().getGlobalName().get();
                        String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();
                        String buttonValue = "";
                        if(buttonEvent.getCustomId().split(":").length > 2)
                            buttonValue = buttonEvent.getCustomId().split(":")[2];

                        partyInfo.addUser(new PartyInfo.UserInfo(buttonUserid, buttonUserName, buttonValue));

                        return buttonEvent.edit("")
                                .withEmbeds(partyInfo.createEmbed());
//                                .withComponents(ActionRow.of(buttons));
                    }

                    if (buttonEvent.getCustomId().equals(deleteButtonGUID)) {
                        String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();

                        if (buttonUserid.equals(partyInfo.getHostInfo().getId())) {

                            InteractionPresentModalSpec.Builder spec = InteractionPresentModalSpec.builder()
                                    .title("Remove User from event: ")
                                    .customId(REMOVE_MODAL_ID);

                            for (PartyInfo.UserInfo userInfo : partyInfo.getUserList())
                                spec.addComponent(ActionRow.of(TextInput.small(REMOVE_MODAL_ID + "_" + signUpButtonGUID + "_" + userInfo.getId(), userInfo.getName(), "Removed").required(false).prefilled(userInfo.getName())));

                            return buttonEvent.presentModal(spec.build());
                        } else {
                            partyInfo.removeUser(buttonUserid);
                        }

                        return buttonEvent.edit("")
                                .withEmbeds(partyInfo.createEmbed());
//                                .withComponents(ActionRow.of(buttons));

                    }

                    // Ignore it
                    return Mono.empty();

                })
                .timeout(Duration.ofDays(3))      //Change to be date set for - date submitted
                .onErrorResume(TimeoutException.class, ignore -> Mono.empty())
                .then();

        return new ButtonInfo(buttonListener, buttons);
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
                System.out.println(e);
            }

            return actionRows;
        }
    }

    private static List<ApplicationCommandOptionData> getApplicationCommandOptionData() {
        List<ApplicationCommandOptionData> options = new ArrayList<>();

        options.add(ApplicationCommandOptionData.builder().name("type").description("type").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());
        options.add(ApplicationCommandOptionData.builder().name("server").description("server").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());
        options.add(ApplicationCommandOptionData.builder().name("people").description("# of People").type(ApplicationCommandOption.Type.INTEGER.getValue()).autocomplete(false).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("timestamp").description("Timestamp").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(false).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("recipe").description("Recipe").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("quantity").description("quantity").type(ApplicationCommandOption.Type.INTEGER.getValue()).autocomplete(false).required(false).build());

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