package net.runelite.client.plugins.eliza.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;
import net.runelite.client.plugins.eliza.state.EquipmentState;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class EquipmentService {
    @Inject
    private Client client;

    /**
     * Existing method: returns a JSON object of equipment,
     * including "slots", "empty_slots" array, and a "description" string.
     */
    public JsonObject getEquipmentState(Player player) {
        JsonObject equipment = new JsonObject();

        if (player != null && player.getPlayerComposition() != null) {
            PlayerComposition composition = player.getPlayerComposition();
            JsonObject slots = new JsonObject();
            JsonArray emptySlots = new JsonArray();
            List<String> equippedItems = new ArrayList<>();

            for (KitType type : KitType.values()) {
                int itemId = composition.getEquipmentId(type);
                int kitId = composition.getKitId(type);
                String slotName = type.name().toLowerCase();

                slots.addProperty(slotName + "Id", itemId);
                slots.addProperty(slotName + "KitId", kitId);

                if (itemId != 0) {
                    ItemComposition itemComposition = client.getItemDefinition(itemId);
                    if (itemComposition != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Equipment {} (index {}): ID {}, Name {}",
                                    slotName, type.getIndex(), itemId, itemComposition.getName());
                        }
                        equippedItems.add(itemComposition.getName());
                    }
                } else {
                    emptySlots.add(slotName);
                }
            }

            equipment.add("slots", slots);
            equipment.add("empty_slots", emptySlots);
            equipment.addProperty("description", createEquipmentDescription(equippedItems));
        }

        return equipment;
    }

    /**
     * Returns a string description of the player's equipped items,
     * e.g. "Bronze sword and Wooden shield" or "nothing."
     */
    public String getEquipmentDescription(Player player) {
        if (player == null || player.getPlayerComposition() == null) {
            return "nothing";
        }

        List<String> equippedItems = new ArrayList<>();
        PlayerComposition composition = player.getPlayerComposition();

        for (KitType type : KitType.values()) {
            int itemId = composition.getEquipmentId(type);
            if (itemId != 0) {
                ItemComposition itemComp = client.getItemDefinition(itemId);
                if (itemComp != null) {
                    equippedItems.add(itemComp.getName());
                }
            }
        }

        return createEquipmentDescription(equippedItems);
    }

    /**
     * Helper to create a user-friendly comma-and-'and' separated
     * list from the equipped items (e.g. "Bronze sword, Wooden shield and Hat").
     */
    private String createEquipmentDescription(List<String> equippedItems) {
        if (equippedItems.isEmpty()) {
            return "nothing";
        }

        StringBuilder description = new StringBuilder();
        for (int i = 0; i < equippedItems.size(); i++) {
            if (i > 0) {
                if (i == equippedItems.size() - 1) {
                    description.append(" and ");
                } else {
                    description.append(", ");
                }
            }
            description.append(equippedItems.get(i));
        }
        return description.toString();
    }

    /**
     * Existing helper to check if the player has ANY equipment.
     */
    public boolean hasEquipment(Player player) {
        if (player == null || player.getPlayerComposition() == null) {
            return false;
        }

        PlayerComposition composition = player.getPlayerComposition();
        for (KitType type : KitType.values()) {
            if (composition.getEquipmentId(type) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Existing helper to get a list of item names for what a player has equipped.
     */
    public List<String> getEquippedItems(Player player) {
        List<String> equippedItems = new ArrayList<>();
        if (player == null || player.getPlayerComposition() == null) {
            return equippedItems;
        }

        PlayerComposition composition = player.getPlayerComposition();
        for (KitType type : KitType.values()) {
            int itemId = composition.getEquipmentId(type);
            if (itemId != 0) {
                ItemComposition itemComposition = client.getItemDefinition(itemId);
                if (itemComposition != null) {
                    equippedItems.add(itemComposition.getName());
                }
            }
        }

        return equippedItems;
    }

    /**
     * NEW method for "snapshot" approach: Builds an EquipmentState object
     * with a Map of slots and a "description" string.
     *
     * This can be read in your WorldStateEndpoint without calling client APIs
     * from the HTTP thread.
     */
    public EquipmentState buildEquipmentSnapshot(Player player) {
        EquipmentState equipState = new EquipmentState();
        Map<String, Integer> slotsMap = new HashMap<>();

        if (player != null && player.getPlayerComposition() != null) {
            PlayerComposition composition = player.getPlayerComposition();

            for (KitType type : KitType.values()) {
                int itemId = composition.getEquipmentId(type);
                String slotName = type.name().toLowerCase();
                // store e.g. "headId" -> itemId
                slotsMap.put(slotName + "Id", itemId);

                // For debugging
                if (itemId != 0 && itemId != -1) {
                    ItemComposition itemComp = client.getItemDefinition(itemId);
                    if (itemComp != null && log.isDebugEnabled()) {
                        log.debug("Equipment {} (index {}): ID {}, Name {}",
                                slotName, type.getIndex(), itemId, itemComp.getName());
                    }
                }
            }
        }

        // Set the slots map
        equipState.setSlots(slotsMap);

        // Use our existing method to build a readable description
        String desc = getEquipmentDescription(player);
        equipState.setDescription(desc);

        return equipState;
    }
}