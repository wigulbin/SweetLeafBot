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
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionPresentModalSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.interaction.GuildCommandRegistrar;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static java.util.Collections.emptyList;

public class Main {
    private static final String token = System.getenv("token");
    private static final long guildId = Long.parseLong("937741716042174486");

    static final String CHAT_INPUT_COMMAND_NAME = "party";
    static final String MODAL_CUSTOM_ID = "my-modal";
    static final String REMOVE_MODAL_ID = "removeModal";
    static final String SELECT_CUSTOM_ID = "my-select";
    static final String INPUT_CUSTOM_ID = "my-input";

    public static void main( String[] args )
    {
        new Timer().schedule(new FileTask(), 0, 300000);

        DiscordClient.create(JSONFile.getJSONValueFromFile("discordAPIKey", "keys.json"))
                .withGateway(client -> {
//                Mono<Void> handlePingCommand = createPingCommand(client);
//                return handlePingCommand;

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

                            String signUpButtonGUID = "signup:" + commandGuid;
                            String deleteButtonGUID = "delete:" + commandGuid;
                            Button button = Button.primary(signUpButtonGUID, "Sign Up!");
                            Button deleteButton = Button.danger(deleteButtonGUID, "Remove Name");

                            Mono<Void> buttonListener = createButtonListener(client, signUpButtonGUID, partyInfo, button, deleteButton, deleteButtonGUID);

                            return event.reply()
                                    .withEmbeds(partyInfo.createEmbed())
                                    .withComponents(ActionRow.of(button, deleteButton))
                                    .then(buttonListener);
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


                            // Finally, return the list of choices to the user
                            return event.respondWithSuggestions(suggestions);
                        }
                        return null;
                    });

                    return GuildCommandRegistrar.create(client.getRestClient(), commands)
                            .registerCommands(Snowflake.of(guildId))
//                            .thenMany(Mono.when(onChatInput, onModal));
                            .thenMany(Mono.when(onChatInput, onModal, onChat));
                })
                .block();
    }

    private static Mono<Void> createButtonListener(GatewayDiscordClient client, String signUpButtonGUID, PartyInfo partyInfo, Button button, Button deleteButton, String deleteButtonGUID) {
        return client.on(ButtonInteractionEvent.class, buttonEvent -> {
                    if (buttonEvent.getCustomId().equals(signUpButtonGUID)) {
                        String buttonUserName = buttonEvent.getInteraction().getUser().getGlobalName().get();
                        String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();

                        partyInfo.addUser(new PartyInfo.UserInfo(buttonUserid, buttonUserName));

                        return buttonEvent.edit("")
                                .withEmbeds(partyInfo.createEmbed())
                                .withComponents(ActionRow.of(button, deleteButton));
                    }

                    if (buttonEvent.getCustomId().equals(deleteButtonGUID)) {
                        String buttonUserName = buttonEvent.getInteraction().getUser().getGlobalName().get();
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
                                .withEmbeds(partyInfo.createEmbed())
                                .withComponents(ActionRow.of(button, deleteButton));

                    }
                    // Ignore it
                    return Mono.empty();

                }).timeout(Duration.ofDays(3))      //Change to be date set for - date submitted
                .onErrorResume(TimeoutException.class, ignore -> Mono.empty())
                .then();
    }

    private static List<ApplicationCommandOptionData> getApplicationCommandOptionData() {
        List<ApplicationCommandOptionData> options = new ArrayList<>();

        options.add(ApplicationCommandOptionData.builder().name("type").description("type").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());
        options.add(ApplicationCommandOptionData.builder().name("server").description("server").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());
        options.add(ApplicationCommandOptionData.builder().name("people").description("# of People").type(ApplicationCommandOption.Type.INTEGER.getValue()).autocomplete(false).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("quantity").description("quantity").type(ApplicationCommandOption.Type.INTEGER.getValue()).autocomplete(false).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("timestamp").description("Timestamp").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(false).required(false).build());

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

            if (message.getContent().equals("!ping"))
                return message.getChannel().flatMap(channel -> channel.createMessage("pong!"));

            return Mono.empty();
        }).then();
    }
}