package net.runelite.client.plugins.eliza.actions.emote;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single-file EmoteHandler that:
 * 1) Detects emote keywords in text via detectEmoteFromMessage(...).
 * 2) Uses reflection to auto-click the emote in official RL by either:
 * - menuAction(...9 params...), or
 * - doAction(...10 params...).
 * 3) If it fails to find either method, no auto emote is possible.
 *
 * Usage Steps:
 * 1) In your plugin's startUp():
 * emoteHandler.initReflection();
 * 2) Whenever AI text arrives, call:
 * String e = emoteHandler.detectEmoteFromMessage(text);
 * if (e != null) { clientThread.invoke(() -> emoteHandler.performEmote(e)); }
 */
@Slf4j
@Singleton
public class EmoteHandler {
    @Inject
    private Client client;

    // ================ Reflection Fields ================
    private Method reflectionMethod; // whichever we find: menuAction or doAction
    private boolean fallbackDoAction; // if we found doAction(...) instead of menuAction(...)
    private boolean reflectionReady;

    // ================ Emote Patterns ================
    // Map of Regex -> Emote name
    private final Map<Pattern, String> emotePatterns = new HashMap<>();

    // ================ Emote -> Child Mapping ================
    // For interface=216 (the Emotes tab). Adjust child IDs for your version if
    // needed.
    private static final int EMOTES_INTERFACE_ID = 216;
    private static final int WAVE_CHILD = 7;
    private static final int DANCE_CHILD = 8;
    private static final int LAUGH_CHILD = 9;
    private static final int CRY_CHILD = 10;
    private static final int ANGRY_CHILD = 11;
    // Add more if you like

    // Usually 57 is WIDGET_FIRST_OPTION (like "Perform" as first menu option)
    private static final int OPCODE_WIDGET_FIRST_OPTION = 57;

    // Constructor: fill in your text patterns
    public EmoteHandler() {
        // We can match "haha", "lmao", "rofl" => "laugh"
        emotePatterns.put(Pattern.compile("\\b(lol|haha|lmao|rofl)\\b", Pattern.CASE_INSENSITIVE), "laugh");
        emotePatterns.put(Pattern.compile("\\b(wave|hello)\\b", Pattern.CASE_INSENSITIVE), "wave");
        emotePatterns.put(Pattern.compile("\\b(dance|dancing)\\b", Pattern.CASE_INSENSITIVE), "dance");
        emotePatterns.put(Pattern.compile("\\b(cry|sad)\\b", Pattern.CASE_INSENSITIVE), "cry");
        emotePatterns.put(Pattern.compile("\\b(angry|furious)\\b", Pattern.CASE_INSENSITIVE), "angry");
    }

    // =========================================================
    // 1) Called by your plugin's startUp() to set up reflection
    // =========================================================
    public void initReflection() {
        Class<? extends Client> klazz = client.getClass();

        // Attempt #1: "menuAction(...9 params...)"
        try {
            reflectionMethod = klazz.getDeclaredMethod(
                    "menuAction",
                    int.class, int.class, int.class, int.class,
                    String.class, String.class,
                    int.class, int.class, int.class);
            reflectionMethod.setAccessible(true);

            reflectionReady = true;
            fallbackDoAction = false;
            log.info("[EmoteHandler] Found 'menuAction(...)' with 9 params. Reflection ready!");
            return;
        } catch (NoSuchMethodException e1) {
            log.warn("[EmoteHandler] Could not find 'menuAction(...)' with 9 params. Trying doAction...");
        } catch (Exception e) {
            log.error("[EmoteHandler] Error checking 'menuAction(...)'", e);
        }

        // Attempt #2: "doAction(...10 params...)" (often the method in some RL builds)
        try {
            // doAction(int,int,int,int,String,String,int,int,int,boolean)
            reflectionMethod = klazz.getDeclaredMethod(
                    "doAction",
                    int.class, int.class, int.class, int.class,
                    String.class, String.class,
                    int.class, int.class, int.class,
                    boolean.class);
            reflectionMethod.setAccessible(true);

            reflectionReady = true;
            fallbackDoAction = true;
            log.info("[EmoteHandler] Found 'doAction(...)' with 10 params. Reflection ready!");
            return;
        } catch (NoSuchMethodException e2) {
            log.warn("[EmoteHandler] Could not find 'doAction(...)' with 10 params either.");
        } catch (Exception e) {
            log.error("[EmoteHandler] Error checking 'doAction(...)'", e);
        }

        // We found none => reflection fails
        reflectionReady = false;
        log.error("[EmoteHandler] Could not find any suitable method. No auto emotes possible.");
    }

    // =========================================================
    // 2) Returns "wave", "dance", "laugh", etc. if a pattern is matched
    // =========================================================
    public String detectEmoteFromMessage(String message) {
        for (Map.Entry<Pattern, String> entry : emotePatterns.entrySet()) {
            Matcher matcher = entry.getKey().matcher(message);
            if (matcher.find()) {
                return entry.getValue(); // e.g. "dance"
            }
        }
        return null;
    }

    // =========================================================
    // 3) Attempt to auto-click the emote in the Emotes tab
    // =========================================================
    public void performEmote(String emotion) {
        // If reflection not found
        if (!reflectionReady) {
            log.warn("[EmoteHandler] Reflection not initialized. Can't auto-click emote: {}", emotion);
            return;
        }

        // Map emotion name -> child ID
        int childId;
        switch (emotion.toLowerCase()) {
            case "wave":
                childId = WAVE_CHILD;
                break;
            case "dance":
                childId = DANCE_CHILD;
                break;
            case "laugh":
                childId = LAUGH_CHILD;
                break;
            case "cry":
                childId = CRY_CHILD;
                break;
            case "angry":
                childId = ANGRY_CHILD;
                break;
            default:
                log.warn("[EmoteHandler] Unknown or unsupported emotion: {}", emotion);
                return;
        }

        // Combine interface + child => widgetId
        int widgetId = (EMOTES_INTERFACE_ID << 16) | childId;

        // Typically param guess for emote is:
        // param0=widgetId, param1=-1, param2=1, param3=0
        // option="Perform", target="",
        // mouseX=0, mouseY=0
        // opcode=57 (WIDGET_FIRST_OPTION)
        // If doAction => add final "boolean" param

        int param0 = widgetId;
        int param1 = -1;
        int param2 = 1; // might be 0 or childId
        int param3 = 0;
        String option = "Perform"; // or "Activate" if your build uses that text
        String target = "";
        int mouseX = 0;
        int mouseY = 0;
        int opcode = OPCODE_WIDGET_FIRST_OPTION; // typically 57

        try {
            if (!fallbackDoAction) {
                // Call menuAction(...9 params...)
                reflectionMethod.invoke(
                        client,
                        param0,
                        param1,
                        param2,
                        param3,
                        option,
                        target,
                        mouseX,
                        mouseY,
                        opcode);
            } else {
                // Call doAction(...10 params...) with final boolean guess
                reflectionMethod.invoke(
                        client,
                        param0,
                        param1,
                        param2,
                        param3,
                        option,
                        target,
                        mouseX,
                        mouseY,
                        opcode,
                        false // some RL builds might want true
                );
            }

            log.debug("[EmoteHandler] Called reflection => Emote: {}", emotion);
        } catch (Exception e) {
            log.error("[EmoteHandler] Error auto-clicking emote: {}", emotion, e);
        }
    }
}
