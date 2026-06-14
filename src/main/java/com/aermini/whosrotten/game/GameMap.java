package com.aermini.whosrotten.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class GameMap {
    private final String gameName;
    private String displayName;
    private String region;
    private Location lobby;
    private final List<Location> spawnPoints = new ArrayList<>();
    private final List<Location> emeraldSpawns = new ArrayList<>();
    private int min;
    private int max;

    public GameMap(String gameName) {
        this.gameName = gameName;
    }

    public String getGameName() { return gameName; }
    public String getDisplayName() { return displayName != null ? displayName : gameName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public Location getLobby() { return lobby; }
    public void setLobby(Location lobby) { this.lobby = lobby; }
    public List<Location> getSpawnPoints() { return spawnPoints; }
    public List<Location> getEmeraldSpawns() { return emeraldSpawns; }
    public int getMin() { return min; }
    public void setMin(int min) { this.min = min; }
    public int getMax() { return max; }
    public void setMax(int max) { this.max = max; }

    public void addSpawnPoint(Location loc) { spawnPoints.add(loc); }
    public void removeSpawnPoint(int index) {
        if (index >= 0 && index < spawnPoints.size()) spawnPoints.remove(index);
    }
    public void addEmeraldSpawn(Location loc) { emeraldSpawns.add(loc); }
    public void removeEmeraldSpawn(int index) {
        if (index >= 0 && index < emeraldSpawns.size()) emeraldSpawns.remove(index);
    }

    public static Location parseLocation(String str) {
        return parseLocation(str, null);
    }

    public static Location parseLocation(String str, String worldName) {
        if (str == null || str.isEmpty()) return null;
        String[] parts = str.split(";");
        if (parts.length < 4) return null;
        try {
            World world = worldName != null ? Bukkit.getWorld(worldName) : Bukkit.getWorlds().get(0);
            if (world == null) world = Bukkit.getWorlds().get(0);
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            float yaw = Float.parseFloat(parts[3]);
            float pitch = parts.length >= 5 ? Float.parseFloat(parts[4]) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    public static String locationToString(Location loc) {
        if (loc == null) return "";
        return loc.getX() + ";" + loc.getY() + ";" + loc.getZ()
                + ";" + loc.getYaw() + ";" + loc.getPitch();
    }
}
