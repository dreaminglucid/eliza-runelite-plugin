package net.runelite.client.plugins.eliza.config;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Eliza")
public interface ElizaConfig extends Config {
    String API_BASE_URL = "http://localhost:3000";

    @ConfigItem(
        keyName = "enabled",
        name = "Enable Plugin",
        description = "Enable the Bridge Troll Chatbot plugin",
        position = 1
    )
    default boolean enabled() {
        return false;
    }

    @ConfigItem(
        keyName = "showOverlay",
        name = "Show Overlay",
        description = "Show the Bridge Troll status overlay",
        position = 2
    )
    default boolean showOverlay() {
        return true;
    }

    @ConfigItem(
        keyName = "apiEndpoint",
        name = "API Endpoint",
        description = "The endpoint URL for the AI service",
        position = 3
    )
    default String apiEndpoint() {
        return API_BASE_URL;
    }
}