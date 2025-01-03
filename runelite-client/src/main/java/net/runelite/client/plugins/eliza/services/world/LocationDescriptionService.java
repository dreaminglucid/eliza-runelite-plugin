package net.runelite.client.plugins.eliza.services.world;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class LocationDescriptionService {
    private static final Map<Integer, Map<String, Object>> LOCATION_DATA = new HashMap<>();
    static {
        // Lumbridge & Surroundings
        LOCATION_DATA.put(12850, new HashMap<String, Object>() {
            {
                put("name", "Lumbridge Castle");
                put("description", "The iconic starting castle of RuneScape where new adventurers begin their journey");
                put("landmarks",
                        new String[] { "Castle Kitchen", "Bank", "General Store", "Duke Horacio's Room", "Chapel" });
                put("coordinates", new int[] { 3222, 3218 });
            }
        });

        LOCATION_DATA.put(12849, new HashMap<String, Object>() {
            {
                put("name", "Lumbridge Swamp North");
                put("description", "The northern section of Lumbridge Swamp, featuring a combat training area");
                put("landmarks", new String[] { "Father Urhney's House", "Combat Training Area", "Fishing Spots" });
                put("coordinates", new int[] { 3245, 3146 });
            }
        });

        LOCATION_DATA.put(12593, new HashMap<String, Object>() {
            {
                put("name", "Lumbridge Swamp South");
                put("description", "The southern reaches of Lumbridge Swamp, home to mining sites and the swamp caves");
                put("landmarks", new String[] { "Mining Site", "Swamp Caves Entrance", "Fishing Spots" });
                put("coordinates", new int[] { 3169, 3172 });
            }
        });

        // Al Kharid
        LOCATION_DATA.put(13105, new HashMap<String, Object>() {
            {
                put("name", "Al Kharid North");
                put("description", "The northern entrance to the desert city of Al Kharid");
                put("landmarks", new String[] { "Al Kharid Gate", "Toll Gate", "Palace Garden" });
                put("coordinates", new int[] { 3293, 3179 });
            }
        });

        LOCATION_DATA.put(13106, new HashMap<String, Object>() {
            {
                put("name", "Al Kharid Central");
                put("description", "The bustling center of Al Kharid featuring the famous palace");
                put("landmarks", new String[] { "Al Kharid Palace", "Bank", "Platelegs Shop", "Crafting Shop" });
                put("coordinates", new int[] { 3293, 3163 });
            }
        });

        // Varrock
        LOCATION_DATA.put(12853, new HashMap<String, Object>() {
            {
                put("name", "Varrock South Gate");
                put("description", "The southern entrance to the grand city of Varrock");
                put("landmarks", new String[] { "South Gate", "Varrock Sword Shop", "Dancing Donkey Inn" });
                put("coordinates", new int[] { 3208, 3384 });
            }
        });

        LOCATION_DATA.put(12854, new HashMap<String, Object>() {
            {
                put("name", "Varrock Center");
                put("description", "The central square of Varrock, heart of commerce and culture");
                put("landmarks",
                        new String[] { "Varrock Square", "Zaff's Staff Shop", "General Store", "Aubury's Rune Shop" });
                put("coordinates", new int[] { 3212, 3422 });
            }
        });

        LOCATION_DATA.put(12855, new HashMap<String, Object>() {
            {
                put("name", "Varrock Palace");
                put("description", "The majestic palace of Varrock and its gardens");
                put("landmarks", new String[] { "Varrock Palace", "Palace Garden", "Royal Guards", "King Roald" });
                put("coordinates", new int[] { 3214, 3463 });
            }
        });

        // Draynor
        LOCATION_DATA.put(12338, new HashMap<String, Object>() {
            {
                put("name", "Draynor Village");
                put("description", "A small, mysterious village known for its marketplace and magical trees");
                put("landmarks",
                        new String[] { "Draynor Marketplace", "Wise Old Man's House", "Bank", "Willow Trees" });
                put("coordinates", new int[] { 3093, 3243 });
            }
        });

        // Falador
        LOCATION_DATA.put(12084, new HashMap<String, Object>() {
            {
                put("name", "Falador Center");
                put("description", "The white-walled city of Falador, home to the White Knights");
                put("landmarks", new String[] { "White Knights' Castle", "Party Room", "Rising Sun Inn" });
                put("coordinates", new int[] { 2964, 3379 });
            }
        });

        LOCATION_DATA.put(12083, new HashMap<String, Object>() {
            {
                put("name", "Falador Garden");
                put("description", "The beautiful garden district of Falador");
                put("landmarks", new String[] { "Park", "Statue", "Garden Maze" });
                put("coordinates", new int[] { 2989, 3383 });
            }
        });

        // Barbarian Village
        LOCATION_DATA.put(12342, new HashMap<String, Object>() {
            {
                put("name", "Barbarian Village");
                put("description", "A village of fierce warriors and skilled fishermen");
                put("landmarks", new String[] { "Barbarian Hall", "Fishing Spot", "Stronghold of Security" });
                put("coordinates", new int[] { 3082, 3420 });
            }
        });

        // Mining Guild Area
        LOCATION_DATA.put(11937, new HashMap<String, Object>() {
            {
                put("name", "Dwarven Mine Area");
                put("description", "The entrance to the Dwarven Mine and Mining Guild");
                put("landmarks", new String[] { "Mining Guild", "Ice Mountain", "Black Knights' Fortress" });
                put("coordinates", new int[] { 3018, 3339 });
            }
        });

        // Edgeville
        LOCATION_DATA.put(12342, new HashMap<String, Object>() {
            {
                put("name", "Edgeville");
                put("description", "A frontier town on the edge of the wilderness");
                put("landmarks", new String[] { "Edgeville Bank", "General Store", "Wilderness Ditch" });
                put("coordinates", new int[] { 3087, 3496 });
            }
        });

        // Wilderness (F2P accessible parts)
        LOCATION_DATA.put(12088, new HashMap<String, Object>() {
            {
                put("name", "Lower Wilderness");
                put("description", "The dangerous borderlands of the Wilderness");
                put("landmarks", new String[] { "Chaos Temple", "Dark Warriors' Fortress", "Wilderness Ditch" });
                put("coordinates", new int[] { 3044, 3520 });
            }
        });

        // Port Sarim
        LOCATION_DATA.put(12082, new HashMap<String, Object>() {
            {
                put("name", "Port Sarim");
                put("description", "The main port of RuneScape, bustling with sailors and traders");
                put("landmarks", new String[] { "Port Sarim Jail", "Fishing Shop", "Betty's Magic Shop", "Docks" });
                put("coordinates", new int[] { 3022, 3208 });
            }
        });

        // Rimmington
        LOCATION_DATA.put(11826, new HashMap<String, Object>() {
            {
                put("name", "Rimmington");
                put("description", "A small mining town south of Falador");
                put("landmarks", new String[] { "Crafting Guild", "Mining Site", "General Store" });
                put("coordinates", new int[] { 2957, 3214 });
            }
        });

        // Wizard's Tower
        LOCATION_DATA.put(12337, new HashMap<String, Object>() {
            {
                put("name", "Wizard's Tower");
                put("description", "The mysterious tower of magical study");
                put("landmarks", new String[] { "Wizard's Tower", "Bridge", "Wizards", "Magical Altar" });
                put("coordinates", new int[] { 3109, 3159 });
            }
        });

        // Karamja F2P Area
        LOCATION_DATA.put(11570, new HashMap<String, Object>() {
            {
                put("name", "Port Sarim Docks");
                put("description", "The bustling docks connecting the mainland to Karamja");
                put("landmarks", new String[] { "Ship to Karamja", "Customs Office", "Fishing Spots" });
                put("coordinates", new int[] { 2956, 3143 });
            }
        });

        // Crandor (F2P Dragon Slayer area)
        LOCATION_DATA.put(11314, new HashMap<String, Object>() {
            {
                put("name", "Crandor");
                put("description", "The volcanic island home to Elvarg the dragon");
                put("landmarks", new String[] { "Elvarg's Lair", "Crash Site", "Volcanic Peaks" });
                put("coordinates", new int[] { 2852, 3238 });
            }
        });

        // Asgarnia Ice Dungeon Area
        LOCATION_DATA.put(11927, new HashMap<String, Object>() {
            {
                put("name", "Ice Mountain");
                put("description", "A snow-capped mountain hiding dangerous ice caves");
                put("landmarks", new String[] { "Oracle", "Ice Warriors", "Black Knights' Fortress" });
                put("coordinates", new int[] { 3008, 3452 });
            }
        });

        // Corsair Cove (F2P area)
        LOCATION_DATA.put(11570, new HashMap<String, Object>() {
            {
                put("name", "Corsair Cove");
                put("description", "A recently discovered cove full of pirates and adventure");
                put("landmarks", new String[] { "Corsair Cove Bank", "Fishing Spots", "Pirates" });
                put("coordinates", new int[] { 2567, 2858 });
            }
        });

        // Clan Camp
        LOCATION_DATA.put(12851, new HashMap<String, Object>() {
            {
                put("name", "Clan Camp");
                put("description", "The gathering place for RuneScape clans");
                put("landmarks", new String[] { "Clan Vexillum", "Portal", "Notice Board" });
                put("coordinates", new int[] { 2961, 3392 });
            }
        });
    }

    public String getAreaDescription(WorldPoint point) {
        int regionId = point.getRegionID();
        Map<String, Object> locationInfo = LOCATION_DATA.get(regionId);

        if (locationInfo != null) {
            StringBuilder desc = new StringBuilder();
            desc.append(locationInfo.get("name"));

            int[] centerCoords = (int[]) locationInfo.get("coordinates");
            int distanceFromCenter = Math.abs(point.getX() - centerCoords[0])
                    + Math.abs(point.getY() - centerCoords[1]);

            String[] landmarks = (String[]) locationInfo.get("landmarks");
            if (distanceFromCenter < 20 && landmarks != null && landmarks.length > 0) {
                desc.append(" near ");
                desc.append(landmarks[Math.min(distanceFromCenter / 10, landmarks.length - 1)]);
            }

            return desc.toString();
        }

        return String.format("at coordinates (%d, %d)", point.getX(), point.getY());
    }

    public String getLocationName(WorldPoint point) {
        int regionId = point.getRegionID();
        Map<String, Object> locationInfo = LOCATION_DATA.get(regionId);
        return locationInfo != null ? (String) locationInfo.get("name") : null;
    }

    public String getLocationDescription(WorldPoint point) {
        int regionId = point.getRegionID();
        Map<String, Object> locationInfo = LOCATION_DATA.get(regionId);
        return locationInfo != null ? (String) locationInfo.get("description") : null;
    }

    public String[] getLocationLandmarks(WorldPoint point) {
        int regionId = point.getRegionID();
        Map<String, Object> locationInfo = LOCATION_DATA.get(regionId);
        return locationInfo != null ? (String[]) locationInfo.get("landmarks") : null;
    }
}