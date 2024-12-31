package net.runelite.client.plugins.eliza.state;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * A snapshot of player's location for JSON serialization.
 * "nearbyPlayers" is a list of objects: [{"name":"Bob","distance":2}, ...]
 */
@Data
public class LocationState
{
    private int x;
    private int y;
    private int plane;
    private int regionId;
    private String description;
    private List<Map<String, Object>> nearbyPlayers;
}
