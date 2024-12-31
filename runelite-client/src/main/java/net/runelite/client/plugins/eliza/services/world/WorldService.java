package net.runelite.client.plugins.eliza.services.world;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.eliza.state.LocationState;
import net.runelite.api.coords.Angle;
import net.runelite.api.coords.Direction;
import net.runelite.client.plugins.eliza.services.player.OtherPlayerService;
import net.runelite.client.plugins.eliza.services.player.LocalPlayerService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class WorldService {
    @Inject
    private Client client;

    @Inject
    private OtherPlayerService playerTracker;

    @Inject
    private LocalPlayerService localPlayerService;

    @Inject
    private LocationDescriptionService locationDescriptionService;

    public LocationState buildLocationState(Player player, OtherPlayerService tracker) {
        LocationState locState = new LocationState();

        if (player == null) {
            locState.setDescription("No local player");
            locState.setNearbyPlayers(Collections.emptyList());
            return locState;
        }

        WorldPoint point = player.getWorldLocation();
        locState.setX(point.getX());
        locState.setY(point.getY());
        locState.setPlane(point.getPlane());
        locState.setRegionId(point.getRegionID());

        String areaDesc = locationDescriptionService.getAreaDescription(point);
        Direction facing = new Angle(player.getOrientation()).getNearestDirection();
        String desc = String.format("%s, facing %s", areaDesc, facing.name().toLowerCase());

        String nearbyPlayersDesc = tracker.getNearbyPlayersDescription(point);
        if (!nearbyPlayersDesc.isEmpty()) {
            desc += " with " + nearbyPlayersDesc;
        }
        locState.setDescription(desc);

        List<Map<String, Object>> nearList = new ArrayList<>();
        for (Player p : client.getPlayers()) {
            if (p != null && !p.equals(localPlayerService.getLocalPlayer())) {
                WorldPoint otherPoint = p.getWorldLocation();
                if (otherPoint != null) {
                    int distance = point.distanceTo(otherPoint);
                    if (distance <= 15) {
                        Map<String, Object> info = new HashMap<>();
                        info.put("name", p.getName());
                        info.put("distance", distance);
                        nearList.add(info);
                    }
                }
            }
        }
        locState.setNearbyPlayers(nearList);

        return locState;
    }

    public JsonObject getWorldLocation(Player player) {
        JsonObject location = new JsonObject();
        if (player == null) {
            return location;
        }

        WorldPoint point = player.getWorldLocation();
        location.addProperty("x", point.getX());
        location.addProperty("y", point.getY());
        location.addProperty("plane", point.getPlane());
        location.addProperty("regionId", point.getRegionID());

        playerTracker.addNearbyPlayersToJson(location, point);

        return location;
    }

    public String getLocationDescription(Player player) {
        if (player == null) {
            return "unknown location";
        }

        WorldPoint point = player.getWorldLocation();
        String areaDesc = locationDescriptionService.getAreaDescription(point);

        Direction facing = new Angle(player.getOrientation()).getNearestDirection();
        StringBuilder description = new StringBuilder(areaDesc);
        description.append(String.format(", facing %s", facing.name().toLowerCase()));

        String nearbyPlayersDesc = playerTracker.getNearbyPlayersDescription(point);
        if (!nearbyPlayersDesc.isEmpty()) {
            description.append(" with ").append(nearbyPlayersDesc);
        }

        return description.toString();
    }

    public WorldPoint getCurrentLocation() {
        return localPlayerService.getLocalPlayerLocation();
    }
}