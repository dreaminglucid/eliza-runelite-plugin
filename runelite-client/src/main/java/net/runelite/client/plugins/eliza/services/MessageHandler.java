package net.runelite.client.plugins.eliza.services;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.eliza.api.APIService;
import net.runelite.client.plugins.eliza.config.ElizaConfig;

import javax.inject.Inject;
import javax.inject.Singleton;

import static net.runelite.client.plugins.eliza.utils.ElizaConstants.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Singleton
public class MessageHandler {
    private final Queue<String> messageQueue = new LinkedList<>();
    private final AtomicBoolean isProcessingQueue = new AtomicBoolean(false);
    private long lastMessageTime = 0L;
    private long lastResponseTime = 0L;
    private String lastSentMessage = "";

    @Inject
    private Client client;

    public void reset() {
        messageQueue.clear();
        isProcessingQueue.set(false);
        lastMessageTime = 0L;
        lastResponseTime = 0L;
        lastSentMessage = "";
    }

    public void processQueue(ClientThread clientThread) {
        long currentTime = System.currentTimeMillis();
        if (!messageQueue.isEmpty() && !isProcessingQueue.get() &&
                (currentTime - lastMessageTime >= MESSAGE_DELAY || lastMessageTime == 0)) {
            processNextMessage(clientThread);
        }
    }

    private void processNextMessage(ClientThread clientThread) {
        if (messageQueue.isEmpty() || isProcessingQueue.get()) {
            return;
        }

        isProcessingQueue.set(true);
        String message = messageQueue.poll();

        if (message != null && !message.isEmpty()) {
            clientThread.invoke(() -> {
                try {
                    sendPublicMessage(message);
                    lastMessageTime = System.currentTimeMillis();
                } finally {
                    isProcessingQueue.set(false);
                }
            });
        } else {
            isProcessingQueue.set(false);
        }
    }

    public void handleChatMessage(ChatMessage chatMessage, Client client,
            OtherPlayerTracker playerTracker, APIService apiService,
            ElizaConfig config) {
        String sender = chatMessage.getName();
        String message = chatMessage.getMessage();

        if (sender.toLowerCase().contains(TROLL_NAME) || message.equalsIgnoreCase(lastSentMessage)) {
            log.debug("Ignoring self-message or echo: {}", message);
            return;
        }

        if (!shouldRespond(playerTracker)) {
            log.debug("Skipping response based on player context");
            return;
        }

        log.info("Chat message received from: {}", sender);
        log.info("Message content: '{}'", message);

        playerTracker.updatePlayerInteraction(sender);
        lastResponseTime = System.currentTimeMillis();

        apiService.sendMessage(sender, message, client, playerTracker, config, this::handleAPIResponse);
    }

    private void handleAPIResponse(List<String> messages) {
        messageQueue.addAll(messages);
    }

    private boolean shouldRespond(OtherPlayerTracker playerTracker) {
        long currentTime = System.currentTimeMillis();
        return currentTime - lastResponseTime >= GLOBAL_COOLDOWN &&
                Math.random() < playerTracker.calculateResponseChance();
    }

    private void sendPublicMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        lastSentMessage = message;
        log.info("Sending message: '{}'", message);
        client.runScript(ScriptID.CHAT_SEND, message, 0, 0, 0, 0);
    }

    public void queueMessages(List<String> messages) {
        messageQueue.addAll(messages);
    }
}