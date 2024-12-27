package net.runelite.client.plugins.eliza;
// package net.runelite.client.plugins.bridgetroll;

// import lombok.extern.slf4j.Slf4j;
// import net.runelite.api.Client;
// import net.runelite.api.Player;
// import net.runelite.api.ScriptID;

// import javax.inject.Inject;
// import javax.inject.Singleton;
// import java.util.regex.Pattern;
// import java.util.regex.Matcher;
// import java.util.HashMap;
// import java.util.Map;

// @Slf4j
// @Singleton
// public class EmoteService {
//     @Inject
//     private Client client;

//     private final Map<Pattern, String> emotePatterns = new HashMap<>();

//     public EmoteService() {
//         // Initialize patterns that should trigger emotes
//         emotePatterns.put(Pattern.compile("\\b(laugh|lmao|haha|rofl)\\b", Pattern.CASE_INSENSITIVE), "laugh");
//         emotePatterns.put(Pattern.compile("\\b(cry|crying|sad|tears)\\b", Pattern.CASE_INSENSITIVE), "cry");
//         emotePatterns.put(Pattern.compile("\\b(angry|mad|furious)\\b", Pattern.CASE_INSENSITIVE), "angry");
//         emotePatterns.put(Pattern.compile("\\b(thinking|think|hmm|ponder)\\b", Pattern.CASE_INSENSITIVE), "think");
//         emotePatterns.put(Pattern.compile("\\b(wave|goodbye|bye|hello|hi)\\b", Pattern.CASE_INSENSITIVE), "wave");
//         emotePatterns.put(Pattern.compile("\\b(bow|respect|honor)\\b", Pattern.CASE_INSENSITIVE), "bow");
//         emotePatterns.put(Pattern.compile("\\b(dance|dancing|groove)\\b", Pattern.CASE_INSENSITIVE), "dance");
//         emotePatterns.put(Pattern.compile("\\b(cheer|celebrate|congrats)\\b", Pattern.CASE_INSENSITIVE), "cheer");
//         emotePatterns.put(Pattern.compile("\\b(clap|applaud|bravo)\\b", Pattern.CASE_INSENSITIVE), "clap");
//         emotePatterns.put(Pattern.compile("\\b(panic|omg|oh no)\\b", Pattern.CASE_INSENSITIVE), "panic");
//         emotePatterns.put(Pattern.compile("\\b(yawn|tired|sleepy)\\b", Pattern.CASE_INSENSITIVE), "yawn");
//         emotePatterns.put(Pattern.compile("\\b(shrug|whatever|idk)\\b", Pattern.CASE_INSENSITIVE), "shrug");
//     }

//     public String detectEmoteFromMessage(String message) {
//         for (Map.Entry<Pattern, String> entry : emotePatterns.entrySet()) {
//             Matcher matcher = entry.getKey().matcher(message);
//             if (matcher.find()) {
//                 return entry.getValue();
//             }
//         }
//         return null;
//     }

//     public void performEmote(String emotion) {
//         int emoteId = getEmoteIdForEmotion(emotion);
//         if (emoteId != -1) {
//             Player player = client.getLocalPlayer();
//             if (player != null) {
//                 client.runScript(ScriptID.CHAT_SEND, "/emote " + emoteId);
//                 log.debug("Performing emote: {} (ID: {})", emotion, emoteId);
//             }
//         }
//     }

//     private int getEmoteIdForEmotion(String emotion) {
//         if (emotion == null) return -1;
        
//         switch(emotion.toLowerCase()) {
//             case "laugh":
//                 return 2;
//             case "cry":
//                 return 3;
//             case "angry":
//                 return 4;
//             case "think":
//                 return 5;
//             case "wave":
//                 return 6;
//             case "bow":
//                 return 7;
//             case "dance":
//                 return 8;
//             case "cheer":
//                 return 9;
//             case "clap":
//                 return 10;
//             case "panic":
//                 return 11;
//             case "yawn":
//                 return 12;
//             case "joy":
//                 return 13;
//             case "shrug":
//                 return 14;
//             case "blow_kiss":
//                 return 15;
//             case "idea": 
//                 return 16;
//             case "stamp":
//                 return 17;
//             case "headbang":
//                 return 18;
//             default:
//                 return -1;
//         }
//     }
// }