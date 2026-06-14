package com.aermini.whosrotten;

import com.aermini.whosrotten.game.GameMap;
import com.aermini.whosrotten.game.GameManager;
import com.aermini.whosrotten.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class wrCommand implements CommandExecutor {
    private final WhosRotten plugin;
    private final ConfigManager cfg;

    public wrCommand(WhosRotten plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以使用此命令");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendMain(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                if (!player.hasPermission("whosrotten.admin")) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7你没有权限使用这个命令")); break; }
                sendHelp(player);
                break;
            case "setlobby":
                if (!player.hasPermission("whosrotten.admin")) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7你没有权限使用这个命令")); break; }
                if (args.length < 2) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7Usage: &f/wr &bsetlobby &7<gameName>")); break; }
                handleSetLobby(player, args[1]);
                break;
            case "addspawn":
                if (!player.hasPermission("whosrotten.admin")) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7你没有权限使用这个命令")); break; }
                if (args.length < 2) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN&r&c● &7Usage: &f/wr addspawn <gameName>")); break; }
                handleAddSpawn(player, args[1]);
                break;
            case "addspawner":
                if (!player.hasPermission("whosrotten.admin")) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7你没有权限使用这个命令")); break; }
                if (args.length < 2) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7Usage: &f/wr addspawner <gameName>")); break; }
                handleAddSpawner(player, args[1]);
                break;
            case "delspawn":
                if (!player.hasPermission("whosrotten.admin")) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7你没有权限使用这个命令")); break; }
                if (args.length < 3) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7Usage: &f/wr delspawn <gameName> <number>")); break; }
                handleDelSpawn(player, args[1], args[2]);
                break;
            case "delspawner":
                if (!player.hasPermission("whosrotten.admin")) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7你没有权限使用这个命令")); break; }
                if (args.length < 3) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7Usage: &f/wr delspawner <gameName> <number>")); break; }
                handleDelSpawner(player, args[1], args[2]);
                break;
            case "listspawn":
                if (!player.hasPermission("whosrotten.admin")) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7你没有权限使用这个命令")); break; }
                if (args.length < 2) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7Usage: &f/wr listspawn <gameName>")); break; }
                handleListSpawn(player, args[1]);
                break;
            case "listspawner":
                if (!player.hasPermission("whosrotten.admin")) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7你没有权限使用这个命令")); break; }
                if (args.length < 2) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7Usage: &f/wr listspawner <gameName>")); break; }
                handleListSpawner(player, args[1]);
                break;
            case "setmax":
                if (!player.hasPermission("whosrotten.admin")) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7你没有权限使用这个命令")); break; }
                if (args.length < 3) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7Usage: &f/wr setmax <gameName> <number>")); break; }
                handleSetMax(player, args[1], args[2]);
                break;
            case "setmin":
                if (!player.hasPermission("whosrotten.admin")) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7你没有权限使用这个命令")); break; }
                if (args.length < 3) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7Usage: &f/wr setmin <gameName> <number>")); break; }
                handleSetMin(player, args[1], args[2]);
                break;
            case "reload":
                if (!player.hasPermission("whosrotten.admin")) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7你没有权限使用这个命令")); break; }
                cfg.reload();
                player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&a● &f插件已重载"));
                break;
            default:
                player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&e● &7命令未知. Usage: &f/wr &bhelp"));
                break;
        }
        return true;
    }

    private void handleSetLobby(Player player, String gameName) {
        GameMap map = cfg.getMap(gameName);
        if (map == null) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7地图不存在")); return; }
        map.setLobby(player.getLocation());
        cfg.saveMapData(map);
        player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&a● &f大厅位置已设置在 &e&l" + map.getDisplayName()));
    }

    private void handleAddSpawn(Player player, String gameName) {
        GameMap map = cfg.getMap(gameName);
        if (map == null) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7地图不存在")); return; }
        map.addSpawnPoint(player.getLocation());
        cfg.saveMapData(map);
        int num = map.getSpawnPoints().size();
        player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&a● &f出生点 &8&l#" + num + " &r&f已&a添加&f在 &e&l" + map.getDisplayName()));
    }

    private void handleAddSpawner(Player player, String gameName) {
        GameMap map = cfg.getMap(gameName);
        if (map == null) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7地图不存在")); return; }
        map.addEmeraldSpawn(player.getLocation());
        cfg.saveMapData(map);
        int num = map.getEmeraldSpawns().size();
        player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&a● &f资源点 &8&l#" + num + " &r&f已&a添加&f在 &e&l" + map.getDisplayName()));
    }

    private void handleDelSpawn(Player player, String gameName, String numStr) {
        GameMap map = cfg.getMap(gameName);
        if (map == null) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7地图不存在")); return; }
        try {
            int num = Integer.parseInt(numStr);
            if (num < 1 || num > map.getSpawnPoints().size()) {
                player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7编号无效")); return;
            }
            map.removeSpawnPoint(num - 1);
            cfg.saveMapData(map);
            player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&a● &f出生点 &8&l#" + num + " &r&f已在 &e&l" + map.getDisplayName() + " &r&f中&c删除"));
        } catch (NumberFormatException e) {
            player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7数字无效"));
        }
    }

    private void handleDelSpawner(Player player, String gameName, String numStr) {
        GameMap map = cfg.getMap(gameName);
        if (map == null) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7地图不存在")); return; }
        try {
            int num = Integer.parseInt(numStr);
            if (num < 1 || num > map.getEmeraldSpawns().size()) {
                player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7编号无效")); return;
            }
            map.removeEmeraldSpawn(num - 1);
            cfg.saveMapData(map);
            player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&a● &f资源点 &8&l#" + num + " &r&f已在 &e&l" + map.getDisplayName() + " &r&f中&c删除"));
        } catch (NumberFormatException e) {
            player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7数字无效"));
        }
    }

    private void handleListSpawn(Player player, String gameName) {
        GameMap map = cfg.getMap(gameName);
        if (map == null) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7地图不存在")); return; }
        player.sendMessage(colorize("&4&m=============&r&6 &lWHOS ROTTEN &r&4&m============="));
        for (int i = 0; i < map.getSpawnPoints().size(); i++) {
            Location loc = map.getSpawnPoints().get(i);
            player.sendMessage(colorize("&8&l#" + (i + 1) + " &r&f" + GameMap.locationToString(loc)));
        }
        player.sendMessage(colorize("&4&m======================================="));
    }

    private void handleListSpawner(Player player, String gameName) {
        GameMap map = cfg.getMap(gameName);
        if (map == null) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7地图不存在")); return; }
        player.sendMessage(colorize("&4&m=============&r&6 &lWHOS ROTTEN &r&4&m============="));
        for (int i = 0; i < map.getEmeraldSpawns().size(); i++) {
            Location loc = map.getEmeraldSpawns().get(i);
            player.sendMessage(colorize("&8&l#" + (i + 1) + " &r&f" + GameMap.locationToString(loc)));
        }
        player.sendMessage(colorize("&4&m======================================="));
    }

    private void handleSetMax(Player player, String gameName, String numStr) {
        GameMap map = cfg.getMap(gameName);
        if (map == null) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7地图不存在")); return; }
        try {
            int max = Integer.parseInt(numStr);
            map.setMax(max);
            cfg.saveMapData(map);
            player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&a● &f最大人数已设置为 &e&l" + max));
        } catch (NumberFormatException e) {
            player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7数字无效"));
        }
    }

    private void handleSetMin(Player player, String gameName, String numStr) {
        GameMap map = cfg.getMap(gameName);
        if (map == null) { player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7地图不存在")); return; }
        try {
            int min = Integer.parseInt(numStr);
            map.setMin(min);
            cfg.saveMapData(map);
            player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&a● &f最小人数已设置为 &e&l" + min));
        } catch (NumberFormatException e) {
            player.sendMessage(colorize("&7&l▎ &6&lWHOS ROTTEN &r&c● &7数字无效"));
        }
    }

    private void sendMain(Player player) {
        String[] lines = {
                "&4&m=============&r&6 &lWHOS ROTTEN &r&4&m=============",
                "        &6&lWHOS ROTTEN &r&fv1.0 &8| &b&lYanYuTing",
                " &7Authors:",
                "    &eAerMini &8| &dhttps://github.com/AerMini2024",
                " &7Usage:",
                "    &f/wr &bhelp",
                "&4&m======================================="
        };
        for (String line : lines) player.sendMessage(colorize(line));
    }

    private void sendHelp(Player player) {
        String[] lines = {
                "&4&m=============&r&6 &lWHOS ROTTEN &r&4&m=============",
                "&f/wr &bsetlobby &7<gameName>  &f设置大厅位置",
                "&f/wr &baddspawn &7<gameName>  &f添加出生点位置",
                "&f/wr &bdelspawn &7<gameName> <number>  &f按编号删除出生点",
                "&f/wr &baddspawner &7<gameName>  &f添加资源点位置",
                "&f/wr &bdelspawner &7<gameName> <number>  &f按编号删除资源点",
                "&f/wr &blistspawn &7<gameName>  &f列出出生点",
                "&f/wr &blistspawner &7<gameName>  &f列出资源点",
                "&f/wr &bsetmax &7<gameName> <number>  &f设置游戏最大人数",
                "&f/wr &bsetmin &7<gameName> <number>  &f设置游戏最小人数",
                "&f/wr &breload  &f重载插件",
                "&4&m======================================="
        };
        for (String line : lines) player.sendMessage(colorize(line));
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}