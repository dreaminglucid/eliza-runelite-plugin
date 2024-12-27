package net.runelite.client.plugins.eliza;

import lombok.Data;

/**
 * A snapshot of your player's game state, updated every GameTick.
 * The HTTP endpoint references getLocationState() and getEquipmentState(),
 * so define them here.
 */
@Data
public class GameDataSnapshot
{
    private boolean loggedIn;
    private String playerName;

    // IMPORTANT: define these fields for location & equipment
    private LocationState locationState;
    private EquipmentState equipmentState;

    private int totalPlayers;
    private long timestamp;
}
