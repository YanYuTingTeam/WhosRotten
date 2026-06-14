package com.aermini.whosrotten.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GamePlayer {
    private final java.util.UUID uuid;
    private int colorId;
    private String kitId;
    private boolean alive;
    private int emeraldCount;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, Boolean> knownPlayers = new HashMap<>(); // for seer

    public GamePlayer(java.util.UUID uuid) {
        this.uuid = uuid;
        this.colorId = 0;
        this.kitId = "normal";
        this.alive = true;
        this.emeraldCount = 0;
    }

    public java.util.UUID getUuid() { return uuid; }
    public int getColorId() { return colorId; }
    public void setColorId(int colorId) { this.colorId = colorId; }
    public String getKitId() { return kitId; }
    public void setKitId(String kitId) { this.kitId = kitId; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public int getEmeraldCount() { return emeraldCount; }
    public void setEmeraldCount(int count) { this.emeraldCount = count; }
    public void addEmerald(int amount) { this.emeraldCount += amount; }
    public boolean removeEmerald(int amount) {
        if (this.emeraldCount < amount) return false;
        this.emeraldCount -= amount;
        return true;
    }

    public long getCooldown(String key) { return cooldowns.getOrDefault(key, 0L); }
    public void setCooldown(String key, long expireTime) { cooldowns.put(key, expireTime); }
    public int getCooldownRemain(String key) {
        long expire = cooldowns.getOrDefault(key, 0L);
        long remain = expire - System.currentTimeMillis();
        return remain > 0 ? (int) (remain / 1000) : 0;
    }
    public boolean isCooldownReady(String key) {
        return System.currentTimeMillis() >= cooldowns.getOrDefault(key, 0L);
    }

    public boolean isKnown(UUID target) { return knownPlayers.getOrDefault(target.toString(), false); }
    public void setKnown(UUID target, boolean known) { knownPlayers.put(target.toString(), known); }
}
