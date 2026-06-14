package com.aermini.whosrotten;

import com.aermini.whosrotten.game.GamePlayer;
import com.aermini.whosrotten.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class MsgFormat {

    public static String msg(String text, Player receiver, Player eventPlayer, Location eventLoc) {
        if (text == null || text.isEmpty()) return text;

        ConfigManager cfg = WhosRotten.getInstance().getConfigManager();

        int now = cfg.getOnlinePlayers();
        int total = cfg.getTotalPlayers();
        int max = cfg.getMaxPlayers();
        int min = cfg.getMinPlayers();
        int need = Math.max(0, min - now);

        text = text.replace("%now%", String.valueOf(now));
        text = text.replace("%total%", String.valueOf(total));
        text = text.replace("%max%", String.valueOf(max));
        text = text.replace("%min%", String.valueOf(min));
        text = text.replace("%need%", String.valueOf(need));

        if (text.contains("%t.start%")) {
            text = text.replace("%t.start%", String.valueOf(cfg.getStartTimer()));
        }
        if (text.contains("%t.game%") || text.contains("%t.game.sec%")) {
            int sec = cfg.getGameTimer();
            text = text.replace("%t.game.sec%", String.valueOf(sec));
            text = text.replace("%t.game%", String.valueOf(sec));
        }
        if (text.contains("%t.game.format#")) {
            int gameSec = cfg.getGameTimer();
            text = formatGameTime(text, gameSec);
        }

        if (text.contains("%winner%")) {
            text = text.replace("%winner%", cfg.getWinner() != null ? cfg.getWinner() : "");
        }

        if (text.contains("%map%")) {
            text = text.replace("%map%", cfg.getMapDisplayName());
        }

        if (text.contains("%cooldown%") && cfg.getEventCooldown() > 0) {
            text = text.replace("%cooldown%", String.valueOf(cfg.getEventCooldown()));
        }

        if (text.contains("%k.")) {
            text = replaceKitCounts(text, cfg);
        }

        if (eventPlayer != null && text.contains("%player")) {
            text = replacePlayerHolders(text, "player", eventPlayer);
        }

        if (receiver != null && text.contains("%p")) {
            text = replacePlayerHolders(text, "p", receiver);
        }

        if (text.contains("%distance.") && eventLoc != null && receiver != null) {
            text = replaceDistance(text, receiver.getLocation(), eventLoc);
        }

        if (eventLoc != null) {
            if (text.contains("%posX%"))
                text = text.replace("%posX%", formatDouble(eventLoc.getX()));
            if (text.contains("%posY%"))
                text = text.replace("%posY%", formatDouble(eventLoc.getY()));
            if (text.contains("%posZ%"))
                text = text.replace("%posZ%", formatDouble(eventLoc.getZ()));
        }

        if (text.contains("%config.")) {
            text = replaceConfigRefs(text, cfg);
        }
        if (text.contains("%message.")) {
            text = replaceMessageRefs(text, cfg);
        }

        return text;
    }

    public static String msg(String text) {
        return msg(text, null, null, null);
    }

    public static String msg(String text, Player receiver) {
        return msg(text, receiver, null, null);
    }

    public static String msg(String text, Player receiver, Player eventPlayer) {
        return msg(text, receiver, eventPlayer, null);
    }

    private static String replaceKitCounts(String text, ConfigManager cfg) {
        int werewolfCount = cfg.getKitCount("werewolf");
        int hunterCount = cfg.getKitCount("hunter");
        int seerCount = cfg.getKitCount("seer");
        int normalCount = cfg.getKitCount("normal");
        int unwolfCount = hunterCount + seerCount + normalCount;

        text = text.replace("%k.werewolf.count%", String.valueOf(werewolfCount));
        text = text.replace("%k.hunter.count%", String.valueOf(hunterCount));
        text = text.replace("%k.seer.count%", String.valueOf(seerCount));
        text = text.replace("%k.normal.count%", String.valueOf(normalCount));
        text = text.replace("%k.unwolf.count%", String.valueOf(unwolfCount));

        if (text.contains("%k.werewolf.gt%")) {
            int gt = cfg.getKitGT("werewolf");
            text = text.replace("%k.werewolf.gt%", String.valueOf(gt));
        }
        if (text.contains("%k.hunter.gt%")) {
            int gt = cfg.getKitGT("hunter");
            text = text.replace("%k.hunter.gt%", String.valueOf(gt));
        }
        if (text.contains("%k.seer.gt%")) {
            int gt = cfg.getKitGT("seer");
            text = text.replace("%k.seer.gt%", String.valueOf(gt));
        }
        return text;
    }

    private static String replacePlayerHolders(String text, String target, Player player) {
        ConfigManager cfg = WhosRotten.getInstance().getConfigManager();
        GamePlayer gp = cfg.getGamePlayer(player.getUniqueId());
        String colorId = gp != null ? String.valueOf(gp.getColorId()) : "0";
        String kitId = gp != null ? gp.getKitId() : "normal";

        String kitPrefix = "%" + target + ".kit.";
        if (text.contains(kitPrefix)) {
            ConfigurationSection kitSec = cfg.getKitSection(kitId);
            String kitText = kitSec != null ? kitSec.getString("name", "") : "";
            String kitFormat = kitSec != null ? kitSec.getString("format", "") : "";
            String kitDesc = kitSec != null ? kitSec.getString("describe", "") : "";
            String kitTask = kitSec != null ? kitSec.getString("task", "") : "";

            text = text.replace(kitPrefix + "text%", kitText);
            text = text.replace(kitPrefix + "format%", kitFormat);
            text = text.replace(kitPrefix + "describe%", kitDesc);
            text = text.replace(kitPrefix + "task%", kitTask);
        }

        String colorPrefix = "%" + target + ".color.";
        if (text.contains(colorPrefix)) {
            java.util.Map<?, ?> colorMap = cfg.getColorMap(colorId);
            String cText = colorMap != null ? String.valueOf(colorMap.containsKey("text") ? colorMap.get("text") : "") : "";
            String c16 = colorMap != null ? String.valueOf(colorMap.containsKey("color") ? colorMap.get("color") : "#FFFFFF") : "#FFFFFF";
            String cFormat = colorMap != null ? String.valueOf(colorMap.containsKey("format") ? colorMap.get("format") : "") : "";

            text = text.replace(colorPrefix + "text%", cText);
            text = text.replace(colorPrefix + "16%", c16);
            text = text.replace(colorPrefix + "10%", String.valueOf(hexToDec(c16)));
            text = text.replace(colorPrefix + "format%", cFormat);
        }

        text = text.replace("%" + target + "%", player.getName());

        return text;
    }

    private static String replaceDistance(String text, Location from, Location to) {
        int start = 0;
        while (true) {
            int idx = text.indexOf("%distance.", start);
            if (idx == -1) break;
            int endIdx = text.indexOf("%", idx + 10);
            if (endIdx == -1) break;
            String decStr = text.substring(idx + 10, endIdx);
            try {
                int decimals = Integer.parseInt(decStr);
                double dist = from.distance(to);
                text = text.substring(0, idx) + formatDouble(dist, decimals)
                        + text.substring(endIdx + 1);
                start = idx + formatDouble(dist, decimals).length();
            } catch (Exception e) {
                start = endIdx + 1;
            }
        }
        return text;
    }

    private static String formatGameTime(String text, int totalSec) {
        int start = 0;
        while (true) {
            int idx = text.indexOf("%t.game.format#", start);
            if (idx == -1) break;
            int endIdx = text.indexOf("%", idx + 16);
            if (endIdx == -1) break;
            String format = text.substring(idx + 16, endIdx);
            String result;
            int h = totalSec / 3600;
            int m = (totalSec % 3600) / 60;
            int s = totalSec % 60;
            switch (format) {
                case "hh:mm:ss":
                    result = String.format("%02d:%02d:%02d", h, m, s);
                    break;
                case "mm:ss":
                    result = String.format("%02d:%02d", h * 60 + m, s);
                    break;
                case "s":
                    result = String.valueOf(totalSec);
                    break;
                case "m:s":
                    result = (h * 60 + m) + ":" + s;
                    break;
                default:
                    result = String.format("%02d:%02d", h * 60 + m, s);
                    break;
            }
            text = text.substring(0, idx) + result + text.substring(endIdx + 1);
            start = idx + result.length();
        }
        return text;
    }

    private static String replaceConfigRefs(String text, ConfigManager cfg) {
        int start = 0;
        while (true) {
            int idx = text.indexOf("%config.", start);
            if (idx == -1) break;
            int endIdx = text.indexOf("%", idx + 8);
            if (endIdx == -1) break;
            String key = text.substring(idx + 8, endIdx);
            String val = cfg.getConfig().getString(key, "");
            text = text.substring(0, idx) + val + text.substring(endIdx + 1);
            start = idx + val.length();
        }
        return text;
    }

    private static String replaceMessageRefs(String text, ConfigManager cfg) {
        int start = 0;
        while (true) {
            int idx = text.indexOf("%message.", start);
            if (idx == -1) break;
            int endIdx = text.indexOf("%", idx + 9);
            if (endIdx == -1) break;
            String key = text.substring(idx + 9, endIdx);
            String val = cfg.getMsg(key);
            text = text.substring(0, idx) + val + text.substring(endIdx + 1);
            start = idx + val.length();
        }
        return text;
    }

    public static int hexToDec(String hex) {
        try {
            hex = hex.replace("#", "");
            return Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String formatDouble(double val) {
        return formatDouble(val, 1);
    }

    public static String formatDouble(double val, int decimals) {
        if (val == (long) val) return String.valueOf((long) val);
        return String.format("%." + decimals + "f", val);
    }
}
