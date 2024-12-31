package net.runelite.client.plugins.eliza.actions.chat;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.eliza.api.external.APIService;
import net.runelite.client.plugins.eliza.actions.emote.EmoteHandler;
import net.runelite.client.plugins.eliza.config.ElizaConfig;
import net.runelite.client.plugins.eliza.services.player.OtherPlayerService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.runelite.client.plugins.eliza.utils.ElizaConstants.GLOBAL_COOLDOWN;
import static net.runelite.client.plugins.eliza.utils.ElizaConstants.MESSAGE_DELAY;
import static net.runelite.client.plugins.eliza.utils.ElizaConstants.TROLL_NAME;

@Slf4j
@Singleton
public class MessageHandler
{
    private final Queue<String> messageQueue = new LinkedList<>();
    private final AtomicBoolean isProcessingQueue = new AtomicBoolean(false);

    private long lastMessageTime = 0L;
    private long lastResponseTime = 0L;
    private String lastSentMessage = "";

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private EmoteHandler emoteHandler; // The reflection-based emote handler

    public void reset()
    {
        messageQueue.clear();
        isProcessingQueue.set(false);
        lastMessageTime = 0L;
        lastResponseTime = 0L;
        lastSentMessage = "";
    }

    /**
     * Attempts to process one queued message if enough time has passed.
     */
    public void processQueue(ClientThread clientThread)
    {
        long currentTime = System.currentTimeMillis();
        if (!messageQueue.isEmpty()
            && !isProcessingQueue.get()
            && (currentTime - lastMessageTime >= MESSAGE_DELAY || lastMessageTime == 0))
        {
            processNextMessage(clientThread);
        }
    }

    /**
     * Polls the next message from the queue and sends it in public chat.
     */
    private void processNextMessage(ClientThread clientThread)
    {
        if (messageQueue.isEmpty() || isProcessingQueue.get())
        {
            return;
        }

        isProcessingQueue.set(true);
        String message = messageQueue.poll();

        if (message != null && !message.isEmpty())
        {
            clientThread.invoke(() ->
            {
                try
                {
                    sendPublicMessage(message);
                    lastMessageTime = System.currentTimeMillis();
                }
                finally
                {
                    isProcessingQueue.set(false);
                }
            });
        }
        else
        {
            isProcessingQueue.set(false);
        }
    }

    /**
     * Called when a new incoming (player) ChatMessage is observed. We pass it
     * to the LLM via APIService; eventually, the LLM calls handleAPIResponse(...).
     */
    public void handleChatMessage(
        ChatMessage chatMessage,
        Client client,
        OtherPlayerService playerTracker,
        APIService apiService,
        ElizaConfig config
    )
    {
        String sender = chatMessage.getName();
        String message = chatMessage.getMessage();

        // Avoid echoing or responding to ourselves
        if (sender.toLowerCase().contains(TROLL_NAME) || message.equalsIgnoreCase(lastSentMessage))
        {
            log.debug("Ignoring self-message or echo: {}", message);
            return;
        }

        if (!shouldRespond(playerTracker))
        {
            log.debug("Skipping response based on player context");
            return;
        }

        log.info("Chat message received from: {}", sender);
        log.info("Message content: '{}'", message);

        // Track who last interacted
        playerTracker.updatePlayerInteraction(sender);
        lastResponseTime = System.currentTimeMillis();

        // Send the user's message to the LLM for generating a reply
        apiService.sendMessage(sender, message, client, playerTracker, config, this::handleAPIResponse);
    }

    /**
     * The callback from APIService (LLM). For each returned line, we:
     *  1) Detect if it triggers an emote
     *  2) Possibly call emoteHandler.performEmote(...)
     *  3) Queue the text for normal in-game chat
     */
    private void handleAPIResponse(List<String> messages)
    {
        for (String msg : messages)
        {
            // 1) Check for an emote in the text. 
            //    (We assume EmoteHandler now uses reflection for actual auto-click.)
            String possibleEmote = emoteHandler.detectEmoteFromMessage(msg);
            if (possibleEmote != null)
            {
                log.debug("Detected emote in AI response: {}", possibleEmote);

                // 2) Perform the emote automatically (reflection-based).
                //    Must run on the clientThread.
                clientThread.invoke(() -> emoteHandler.performEmote(possibleEmote));

                // If you prefer NOT to show the entire line in chat, you could skip:
                // continue;
                // or do some string replace to remove the emote keywords.
            }

            // 3) Queue the line for normal public chat (unless you skip it).
            messageQueue.add(msg);
        }
    }

    /**
     * Simple check to avoid spamming. If we haven't responded in a while and the 
     * random roll is successful, we respond.
     */
    private boolean shouldRespond(OtherPlayerService playerTracker)
    {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastResponseTime >= GLOBAL_COOLDOWN)
            && Math.random() < playerTracker.calculateResponseChance();
    }

    /**
     * Sends a message in public chat using ScriptID.CHAT_SEND
     * so others actually see it in game chat.
     */
    private void sendPublicMessage(String message)
    {
        if (message == null || message.isEmpty())
        {
            return;
        }

        lastSentMessage = message;
        log.info("Sending message: '{}'", message);

        // 5-arg runScript call for chat (string + 4 ints)
        client.runScript(ScriptID.CHAT_SEND, message, 0, 0, 0, 0);
    }

    /**
     * Allows manual adding of AI messages if needed
     */
    public void queueMessages(List<String> messages)
    {
        messageQueue.addAll(messages);
    }
}
