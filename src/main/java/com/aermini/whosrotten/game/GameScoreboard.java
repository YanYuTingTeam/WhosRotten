package com.aermini.whosrotten.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class GameScoreboard {
    private final Scoreboard board;
    private final Objective objective;
    private final List<String> entryKeys = new ArrayList<>();
    private static final int MAX_SIDEBAR_ENTRIES = 15;

    public GameScoreboard(String title) {
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();
        String objectiveId = "sb_" + UUID.randomUUID().toString().substring(0, 8);
        this.objective = board.registerNewObjective(objectiveId, "dummy");
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        setTitle(title);
    }

    public Scoreboard getBoard() {
        return board;
    }

    public void setTitle(String title) {
        objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));
    }

    public void updateLines(List<String> lines) {
        int count = Math.min(lines.size(), MAX_SIDEBAR_ENTRIES);
        ensureEntries(count);
        for (int i = 0; i < count; i++) {
            updateTeamDisplay(i, lines.get(i));
        }
    }

    public void updateLines(String... lines) {
        updateLines(Arrays.asList(lines));
    }

    public void send(Player player) {
        player.setScoreboard(board);
    }

    public void reset(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void destroy() {
        clearAllEntries();
        if (objective != null) {
            objective.unregister();
        }
    }

    private void clearAllEntries() {
        for (String key : entryKeys) {
            Team t = board.getTeam("sb_" + key);
            if (t != null) {
                board.resetScores(key);
                t.unregister();
            }
        }
        entryKeys.clear();
    }

    private String generateInvisibleKey(int index) {
        StringBuilder key = new StringBuilder();
        int i = index;
        do {
            key.insert(0, ChatColor.COLOR_CHAR + "" + (char) ('0' + (i % 16)));
            i = i / 16 - 1;
        } while (i >= 0);
        return key.toString();
    }

    private void ensureEntries(int count) {
        count = Math.min(count, MAX_SIDEBAR_ENTRIES);
        while (entryKeys.size() > count) {
            String key = entryKeys.remove(entryKeys.size() - 1);
            Team t = board.getTeam("sb_" + key);
            if (t != null) {
                board.resetScores(key);
                t.unregister();
            }
        }
        while (entryKeys.size() < count) {
            String key = generateInvisibleKey(entryKeys.size());
            Team t = board.registerNewTeam("sb_" + key);
            t.addEntry(key);
            entryKeys.add(key);
        }
    }

    private void updateTeamDisplay(int index, String display) {
        if (index >= entryKeys.size()) return;
        String key = entryKeys.get(index);
        Team t = board.getTeam("sb_" + key);
        if (t == null) return;
        display = ChatColor.translateAlternateColorCodes('&', display);
        if (display.length() <= 16) {
            t.setPrefix(display);
            t.setSuffix("");
        } else {
            int split = 16;
            if (display.charAt(split - 1) == ChatColor.COLOR_CHAR) split--;
            String prefix = display.substring(0, split);
            String suffix = display.substring(split);
            String lastColor = ChatColor.getLastColors(prefix);
            if (!lastColor.isEmpty() && (suffix.isEmpty() || suffix.charAt(0) != ChatColor.COLOR_CHAR)) suffix=lastColor+suffix;
            t.setPrefix(prefix);
            t.setSuffix(suffix.length() > 16 ? suffix.substring(0, 16) : suffix);
        }
        objective.getScore(key).setScore(entryKeys.size() - index);
    }
}