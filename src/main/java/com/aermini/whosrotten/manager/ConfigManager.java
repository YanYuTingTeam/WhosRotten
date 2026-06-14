package com.aermini.whosrotten.manager;

import com.aermini.whosrotten.WhosRotten;
import com.aermini.whosrotten.game.GameMap;
import com.aermini.whosrotten.game.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ConfigManager {
    private final WhosRotten plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, GameMap> maps = new HashMap<>();
    private final Map<UUID, GamePlayer> players = new HashMap<>();

    // game state references
    private String currentGameName;
    private int startTimer;
    private int gameTimer;
    private String winner;
    private int eventCooldown;
    private String mapDisplayName;
    private int gameStartTotalPlayers;

    public ConfigManager(WhosRotten plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public void loadAll() {
        configs.clear();
        maps.clear();
        String[] files = {"config.yml", "message.yml", "game.yml", "item.yml", "kit.yml", "menu.yml"};
        for (String name : files) {
            File file = new File(plugin.getDataFolder(), name);
            if (!file.exists()) {
                plugin.saveResource(name, false);
            }
            configs.put(name, YamlConfiguration.loadConfiguration(file));
        }
        loadMaps();
    }

    public void reload() {
        loadAll();
    }

    private void loadMaps() {
        maps.clear();
        ConfigurationSection mapsSec = configs.get("game.yml").getConfigurationSection("maps");
        if (mapsSec == null) return;
        for (String key : mapsSec.getKeys(false)) {
            ConfigurationSection sec = mapsSec.getConfigurationSection(key);
            GameMap map = new GameMap(key);
            map.setDisplayName(sec.getString("display", key));
            map.setRegion(sec.getString("region", ""));
            map.setMin(sec.getInt("min", 12));
            map.setMax(sec.getInt("max", 24));
            String region = sec.getString("region", "");
            String lobbyStr = sec.getString("lobby", "");
            map.setLobby(GameMap.parseLocation(lobbyStr, region));
            List<String> spawns = sec.getStringList("spawnpoints");
            for (String s : spawns) {
                Location loc = GameMap.parseLocation(s, region);
                if (loc != null) map.addSpawnPoint(loc);
            }
            List<String> emeralds = sec.getStringList("emeralds");
            for (String s : emeralds) {
                Location loc = GameMap.parseLocation(s, region);
                if (loc != null) map.addEmeraldSpawn(loc);
            }
            maps.put(key, map);
        }
    }

    // save game.yml spawns/lobby/emeralds back to file
    public void saveMapData(GameMap map) {
        FileConfiguration gameYml = configs.get("game.yml");
        String base = "maps." + map.getGameName();
        gameYml.set(base + ".display", map.getDisplayName());
        gameYml.set(base + ".region", map.getRegion());
        gameYml.set(base + ".min", map.getMin());
        gameYml.set(base + ".max", map.getMax());
        gameYml.set(base + ".lobby", GameMap.locationToString(map.getLobby()));
        List<String> spawns = new ArrayList<>();
        for (org.bukkit.Location loc : map.getSpawnPoints()) spawns.add(GameMap.locationToString(loc));
        gameYml.set(base + ".spawnpoints", spawns);
        List<String> emeralds = new ArrayList<>();
        for (org.bukkit.Location loc : map.getEmeraldSpawns()) emeralds.add(GameMap.locationToString(loc));
        gameYml.set(base + ".emeralds", emeralds);
        try {
            gameYml.save(new File(plugin.getDataFolder(), "game.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() { return configs.get("config.yml"); }
    public FileConfiguration getMessage() { return configs.get("message.yml"); }
    public FileConfiguration getGameConfig() { return configs.get("game.yml"); }
    public FileConfiguration getItemConfig() { return configs.get("item.yml"); }
    public FileConfiguration getKitConfig() { return configs.get("kit.yml"); }
    public FileConfiguration getMenuConfig() { return configs.get("menu.yml"); }

    public GameMap getMap(String name) { return maps.get(name); }
    public Collection<GameMap> getAllMaps() { return maps.values(); }

    public GamePlayer getGamePlayer(UUID uuid) { return players.get(uuid); }
    public GamePlayer getOrCreateGamePlayer(UUID uuid) {
        return players.computeIfAbsent(uuid, GamePlayer::new);
    }
    public void removeGamePlayer(UUID uuid) { players.remove(uuid); }
    public Map<UUID, GamePlayer> getAllGamePlayers() { return players; }

    public String getMsg(String key) {
        Object val = configs.get("message.yml").get(key);
        if (val instanceof List) {
            List<String> list = (List<String>) val;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(list.get(i));
            }
            return sb.toString();
        }
        return val != null ? val.toString() : "";
    }

    public List<String> getMsgList(String key) {
        Object val = configs.get("message.yml").get(key);
        if (val instanceof List) return (List<String>) val;
        if (val != null) return Collections.singletonList(val.toString());
        return Collections.emptyList();
    }

    public ConfigurationSection getKitSection(String kitId) {
        return configs.get("kit.yml").getConfigurationSection(kitId);
    }

    public ConfigurationSection getColorSection(String colorId) {
        List<Map<?, ?>> colors = configs.get("config.yml").getMapList("colors");
        for (Map<?, ?> c : colors) {
            if (String.valueOf(c.get("id")).equals(colorId)) {
                // convert to ConfigurationSection-like access
                // we'll just return null and use map directly
                return null;
            }
        }
        return null;
    }

    // color lookup by id (returns map)
    public Map<?, ?> getColorMap(String colorId) {
        List<Map<?, ?>> colors = configs.get("config.yml").getMapList("colors");
        for (Map<?, ?> c : colors) {
            if (String.valueOf(c.get("id")).equals(colorId)) return c;
        }
        return null;
    }

    public ConfigurationSection getItemSection(String itemId) {
        return configs.get("item.yml").getConfigurationSection(itemId);
    }

    public int getStartTimer() { return startTimer; }
    public void setStartTimer(int t) { this.startTimer = t; }
    public int getGameTimer() { return gameTimer; }
    public void setGameTimer(int t) { this.gameTimer = t; }
    public String getWinner() { return winner; }
    public void setWinner(String w) { this.winner = w; }
    public int getEventCooldown() { return eventCooldown; }
    public void setEventCooldown(int cd) { this.eventCooldown = cd; }
    public String getMapDisplayName() { return mapDisplayName; }
    public void setMapDisplayName(String name) { this.mapDisplayName = name; }
    public String getCurrentGameName() { return currentGameName; }
    public void setCurrentGameName(String name) { this.currentGameName = name; }

    public int getOnlinePlayers() {
        return players.size();
    }
    public int getTotalPlayers() { return gameStartTotalPlayers; }
    public void setGameStartTotalPlayers(int n) { this.gameStartTotalPlayers = n; }
    public int getMaxPlayers() {
        GameMap map = maps.get(currentGameName);
        return map != null ? map.getMax() : 24;
    }
    public int getMinPlayers() {
        GameMap map = maps.get(currentGameName);
        return map != null ? map.getMin() : 12;
    }

    public int getKitCount(String kitId) {
        int count = 0;
        for (GamePlayer gp : players.values()) {
            if (gp.isAlive() && gp.getKitId().equals(kitId)) count++;
        }
        return count;
    }

    public int getKitGT(String kitId) {
        ConfigurationSection sec = getKitSection(kitId);
        return sec != null ? sec.getInt("gt", 0) : 0;
    }
}
