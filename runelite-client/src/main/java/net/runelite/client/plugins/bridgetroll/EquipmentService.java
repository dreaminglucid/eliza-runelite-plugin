package net.runelite.client.plugins.bridgetroll;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.kit.KitType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class EquipmentService {
    @Inject
    private Client client;

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
                        log.info("Equipment {} (index {}): ID {}, Name {}", 
                               slotName, type.getIndex(), itemId, itemComposition.getName());
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

    public String getEquipmentDescription(Player player) {
        if (player == null || player.getPlayerComposition() == null) {
            return "nothing";
        }

        List<String> equippedItems = new ArrayList<>();
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

        return createEquipmentDescription(equippedItems);
    }

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
}