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
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.*;
import discord4j.discordjson.json.*;
import discord4j.rest.interaction.GuildCommandRegistrar;
import discord4j.rest.util.Color;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import javax.swing.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static java.util.Collections.emptyList;

public class Main {
    private static final String token = System.getenv("token");
    private static final long guildId = Long.parseLong("937741716042174486");

    static final String CHAT_INPUT_COMMAND_NAME = "party";
    static final String MODAL_CUSTOM_ID = "my-modal";
    static final String SELECT_CUSTOM_ID = "my-select";
    static final String INPUT_CUSTOM_ID = "my-input";

    public static void main( String[] args )
    {
        DiscordClient.create(JSONFile.getJSONValueFromFile("discordAPIKey", "keys.json"))
                .withGateway(client -> {
//                Mono<Void> handlePingCommand = createPingCommand(client);
//                return handlePingCommand;

//                    List<String> types = List.of("fishing", "hunting", "applepie", "cake");
                    List<ApplicationCommandOptionData> options = getApplicationCommandOptionData();

                    ApplicationCommandRequest example = ApplicationCommandRequest.builder()
                            .name(CHAT_INPUT_COMMAND_NAME)
                            .addAllOptions(options)
                            .description("Create a party for Palia")
                            .build();

                    List<ApplicationCommandRequest> commands = Collections.singletonList(example);

                    Publisher<?> onChatInput = client.on(ChatInputInteractionEvent.class, event -> {
                        if (CHAT_INPUT_COMMAND_NAME.equals(event.getCommandName())) {
                            String userName = event.getInteraction().getUser().getGlobalName().get();
                            String userid = event.getInteraction().getUser().getId().asString();
                            String type = event.getOption("type").get().getValue().get().asString();
                            long people;
                            if(event.getOption("people").isPresent())
                                people = event.getOption("people").get().getValue().get().asLong();
                            else
                                people = 10;

                            String server = event.getOption("server").get().getValue().get().asString();

                            long finalPeople = people;
//                            return client.getChannelById(Snowflake.of(event.getInteraction().getChannelId().asLong()))
//                                    .ofType(GuildMessageChannel.class)
//                                    .flatMap(channel -> channel.createMessage(createPartyEmbed(new PartyInfo(type, userid, finalPeople, server, userName, true))));


                            Button button = Button.primary("custom-id", "Sign Up!");
                            Button deleteButton = Button.danger("delete-id", "Remove");
                            return client.getChannelById(Snowflake.of(event.getInteraction().getChannelId().asLong()))
                                    .ofType(GuildMessageChannel.class)
                                    .flatMap(channel -> {
                                        PartyInfo partyInfo = new PartyInfo(type, new UserInfo(userid, userName), finalPeople, server, true);

                                        MessageCreateSpec messageSpec = MessageCreateSpec.builder()
                                                // Buttons must be in action rows
                                                .addEmbed(createPartyEmbed(partyInfo))
                                                .addComponent(ActionRow.of(button, deleteButton)).build();

                                        Mono<Message> messageMono = channel.createMessage(messageSpec);

                                        Mono<Void> buttonListener = client.on(ButtonInteractionEvent.class, buttonEvent -> {
                                                    if (buttonEvent.getCustomId().equals("custom-id"))
                                                    {
                                                        String buttonUserName = buttonEvent.getInteraction().getUser().getGlobalName().get();
                                                        String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();

                                                        partyInfo.addUser(new UserInfo(buttonUserid, buttonUserName));

                                                        return buttonEvent.edit("")
                                                                .withEmbeds(createPartyEmbed(partyInfo))
                                                                .withComponents(ActionRow.of(button, deleteButton));
                                                    }

                                                    if (buttonEvent.getCustomId().equals("delete-id")) {
                                                        String buttonUserName = buttonEvent.getInteraction().getUser().getGlobalName().get();
                                                        String buttonUserid = buttonEvent.getInteraction().getUser().getId().asString();

                                                        if(buttonUserid.equals(partyInfo.getHostInfo().id)){

                                                            InteractionPresentModalSpec.Builder spec = InteractionPresentModalSpec.builder()
                                                                    .title("Remove User from event: ")
                                                                    .customId(MODAL_CUSTOM_ID);

                                                            for (UserInfo userInfo : partyInfo.getUserList())
                                                                spec.addComponent(ActionRow.of(TextInput.small("text-" + userInfo.id, userInfo.name, userInfo.name).prefilled(userInfo.name)));

                                                            return buttonEvent.presentModal(spec.build());
                                                        } else {
                                                            partyInfo.removeUser(new UserInfo(buttonUserid, buttonUserName));
                                                        }

                                                        return buttonEvent.edit("")
                                                                .withEmbeds(createPartyEmbed(partyInfo))
                                                                .withComponents(ActionRow.of(button, deleteButton));

                                                    }
                                                    // Ignore it
                                                    return Mono.empty();

                                                }).timeout(Duration.ofMinutes(30))
                                                .onErrorResume(TimeoutException.class, ignore -> Mono.empty())
                                                .then();


                                        return messageMono.then(buttonListener);
                                    });


//                            return event.presentModal(InteractionPresentModalSpec.builder()
//                                    .title(type + " event for " + quantity)
//                                    .customId(MODAL_CUSTOM_ID)
//                                    .addComponent(ActionRow.of(TextInput.small(INPUT_CUSTOM_ID, "When is this event taking place?").required(false)))
//                                    .build());
                        }
                        return Mono.empty();
                    });

                    Publisher<?> onModal = client.on(ModalSubmitInteractionEvent.class, event -> {
                        if (MODAL_CUSTOM_ID.equals(event.getCustomId())) {
                            String comments = "";
                            for (TextInput component : event.getComponents(TextInput.class)) {
                                if (INPUT_CUSTOM_ID.equals(component.getCustomId())) {
                                    comments = component.getValue().orElse("");
                                }
                            }
                            for (SelectMenu component : event.getComponents(SelectMenu.class)) {
                                if (SELECT_CUSTOM_ID.equals(component.getCustomId())) {
                                    return event.reply("You selected: " +
                                                    component.getValues().orElse(emptyList()) +
                                                    (comments.isEmpty() ? "" : "\nwith a comment: " + comments))
                                            .withEphemeral(true);
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
                                    .orElse(""); // In case the user has not started typing, we return an empty string

                            /*
                            Build a list of choices to present to the user as suggested input

                            For the sake of simplicity in this demo, we are returning a static list here.
                            Ideally you would use fuzzy matching or other techniques to suggest up to 25 choices for the user.
                            */
                            List<ApplicationCommandOptionChoiceData> suggestions = new ArrayList<>();

                            if(event.getFocusedOption().getName().equals("type"))
                            {
                                suggestions.add(ApplicationCommandOptionChoiceData.builder().name("Fishing").value("Fishing").build());
                                suggestions.add(ApplicationCommandOptionChoiceData.builder().name("Hunting").value("Hunting").build());
                                suggestions.add(ApplicationCommandOptionChoiceData.builder().name("Foraging").value("Foraging").build());
                                suggestions.add(ApplicationCommandOptionChoiceData.builder().name("Bug Catching").value("BugCatching").build());
                                suggestions.add(ApplicationCommandOptionChoiceData.builder().name("Apple Pie").value("ApplePie").build());
                                suggestions.add(ApplicationCommandOptionChoiceData.builder().name("Cake").value("Cake").build());
                            }

                            if(event.getFocusedOption().getName().equals("server"))
                            {
                                suggestions.add(ApplicationCommandOptionChoiceData.builder().name("NA").value("NA").build());
                                suggestions.add(ApplicationCommandOptionChoiceData.builder().name("EU").value("EU").build());
                                suggestions.add(ApplicationCommandOptionChoiceData.builder().name("PA").value("PA").build());
                            }

                            if(event.getFocusedOption().getName().equals("host"))
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

    private static List<ApplicationCommandOptionData> getApplicationCommandOptionData() {
        List<ApplicationCommandOptionData> options = new ArrayList<>();

        options.add(ApplicationCommandOptionData.builder().name("type").description("type").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());
        options.add(ApplicationCommandOptionData.builder().name("server").description("server").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());
        options.add(ApplicationCommandOptionData.builder().name("people").description("# of People").type(ApplicationCommandOption.Type.INTEGER.getValue()).autocomplete(false).required(false).build());
        options.add(ApplicationCommandOptionData.builder().name("quantity").description("quantity").type(ApplicationCommandOption.Type.INTEGER.getValue()).autocomplete(false).required(false).build());
//        options.add(ApplicationCommandOptionData.builder().name("host").description("host").type(ApplicationCommandOption.Type.STRING.getValue()).autocomplete(true).required(true).build());


        return options;
    }

    public static EmbedCreateSpec createPartyEmbed(PartyInfo partyInfo)
    {
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

    record UserInfo(String id, String name) {
        String getPingText(){
            return "<@" + id + ">";
        }
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