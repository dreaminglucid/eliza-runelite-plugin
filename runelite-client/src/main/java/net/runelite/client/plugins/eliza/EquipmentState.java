package net.runelite.client.plugins.eliza;

import java.util.Map;
import lombok.Data;

/**
 * A simple snapshot of player equipment for JSON serialization.
 * "slots" is a Map like {"headId":..., "shieldId":..., etc.}
 */
@Data
public class EquipmentState
{
    private Map<String, Integer> slots;
    private String description;
}
