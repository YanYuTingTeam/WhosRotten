package com.aermini.whosrotten.game;

import com.aermini.whosrotten.MsgFormat;
import com.aermini.whosrotten.WhosRotten;
import com.aermini.whosrotten.manager.ConfigManager;
import com.aermini.whosrotten.util.BungeeUtil;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GameManager {
    private final WhosRotten plugin;
    private final ConfigManager cfg;

    public enum GameState { WAITING, STARTING, GAMING, ENDING }
    private GameState state = GameState.WAITING;
    private GameMap currentMap;

    private int startCountdown;
    private int gameCountdown;
    private int emeraldTimer;
    private int endingTimer;
    private int mainTaskId = -1;
    private int fireworkTaskId = -1;
    private final List<Integer> kitTasks = new ArrayList<>();

    public GameManager(WhosRotten plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    public GameState getState() { return state; }
    public GameMap getCurrentMap() { return currentMap; }

    private void startMainLoop() {
        if (mainTaskId != -1) Bukkit.getScheduler().cancelTask(mainTaskId);
        mainTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                switch (state) {
                    case WAITING: tickWaiting(); break;
                    case STARTING: tickStarting(); break;
                    case GAMING: tickGaming(); break;
                    case ENDING: tickEnding(); break;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    private void stopMainLoop() {
        if (mainTaskId != -1) {
            Bukkit.getScheduler().cancelTask(mainTaskId);
            mainTaskId = -1;
        }
    }

    public void startWaiting(String gameName) {
        currentMap = cfg.getMap(gameName);
        if (currentMap == null) {
            plugin.getPluginLogger().warning("地图 " + gameName + " 不存在!");
            return;
        }
        cfg.setCurrentGameName(gameName);
        cfg.setMapDisplayName(currentMap.getDisplayName());
        state = GameState.WAITING;
        startMainLoop();
    }

    private void tickWaiting() {
        updateScoreboard();
        int now = cfg.getOnlinePlayers();
        int min = cfg.getMinPlayers();
        if (now >= min) {
            startCountdown();
        }
    }

    public void handleJoin(Player player) {
        if (state != GameState.WAITING && state != GameState.STARTING) {
            player.kickPlayer("游戏已开始");
            return;
        }
        GamePlayer gp = cfg.getOrCreateGamePlayer(player.getUniqueId());
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();

        if (currentMap != null && currentMap.getLobby() != null) {
            player.teleport(currentMap.getLobby());
        }
        giveBeforeStartItems(player);
        broadcastMsg("join", player);

        if (cfg.getOnlinePlayers() < cfg.getMinPlayers()) {
            sendMsg(player, "needmore");
        }

        if (state == GameState.WAITING && cfg.getOnlinePlayers() >= cfg.getMinPlayers()) {
            startCountdown();
        }
        updateScoreboard();
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        GamePlayer gp = cfg.getGamePlayer(uuid);
        if (gp == null) return;

        if (state == GameState.GAMING) {
            String quitKit = gp.getKitId();
            gp.setAlive(false);
            cfg.removeGamePlayer(uuid);
            quitDuringGame.put(uuid, quitKit);
            broadcastMsg("leave", player);
            boolean noSameTeamLeft = true;
            for (GamePlayer other : cfg.getAllGamePlayers().values()) {
                if (other.getKitId().equals(quitKit)) {
                    noSameTeamLeft = false;
                    break;
                }
            }
            if (noSameTeamLeft) {
                if (quitKit.equals("werewolf")) endGame("人类");
                else endGame("狼人");
            }
        } else if (state == GameState.ENDING) {
            cfg.removeGamePlayer(uuid);
        } else {
            cfg.removeGamePlayer(uuid);
            broadcastMsg("leave", player);
            if (state == GameState.STARTING && cfg.getOnlinePlayers() < cfg.getMinPlayers()) {
                state = GameState.WAITING;
                startCountdown = 0;
                broadcastMsg("countcancel");
            }
        }

        if (cfg.getOnlinePlayers() == 0 && state != GameState.WAITING) {
            resetGame();
        }
        updateScoreboard();
    }

    private void startCountdown() {
        state = GameState.STARTING;
        startCountdown = cfg.getConfig().getInt("starttime", 30);
        broadcastMsg("counting-start");
    }

    private void tickStarting() {
        if (cfg.getOnlinePlayers() < cfg.getMinPlayers()) {
            state = GameState.WAITING;
            startCountdown = 0;
            broadcastMsg("countcancel");
            updateScoreboard();
            return;
        }
        startCountdown--;
        cfg.setStartTimer(startCountdown);
        if (startCountdown <= 0) {
            startGame();
            return;
        }
        broadcastMsg("counting");
        broadcastTitle("countstart");
        updateScoreboard();
    }

    private void startGame() {
        state = GameState.GAMING;
        gameCountdown = cfg.getConfig().getInt("gametime", 600);
        emeraldTimer = 0;
        cfg.setGameTimer(gameCountdown);
        cfg.setGameStartTotalPlayers(cfg.getOnlinePlayers());

        Player anyPlayer = null;
        org.bukkit.World gameWorld = getGameWorld();
        for (org.bukkit.entity.Entity ent : gameWorld.getEntities()) {
            if (ent instanceof Player) {
                anyPlayer = (Player) ent;
                break;
            }
        }
        if (anyPlayer != null) {
            Bukkit.dispatchCommand(anyPlayer, "removecorpse 500");
        }

        List<GamePlayer> playerList = new ArrayList<>(cfg.getAllGamePlayers().values());
        Collections.shuffle(playerList);
        List<Location> spawns = new ArrayList<>(currentMap.getSpawnPoints());
        Collections.shuffle(spawns);

        List<String> availableKits = buildKitList(playerList.size());
        Collections.shuffle(availableKits);

        for (int i = 0; i < playerList.size(); i++) {
            GamePlayer gp = playerList.get(i);
            gp.setColorId(i + 1);
            if (i < availableKits.size()) {
                gp.setKitId(availableKits.get(i));
            }

            Player player = Bukkit.getPlayer(gp.getUuid());
            if (player != null && !spawns.isEmpty()) {
                Location spawn = spawns.get(i % spawns.size());
                player.teleport(spawn);
            }
        }

        for (GamePlayer gp : playerList) {
            Player player = Bukkit.getPlayer(gp.getUuid());
            if (player == null) continue;
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            giveGamingItems(player);
            giveColorArmor(player, gp);
            giveKitStartItems(player, gp);
            hideNameTag(player);
            showRoleInfo(player, gp);
        }

        setupWerewolfTeams();

        scheduleKitTasks();

        broadcastMsg("started");
        broadcastTitle("started");
        updateScoreboard();
    }

    private void tickGaming() {
        gameCountdown--;
        cfg.setGameTimer(gameCountdown);
        emeraldTimer++;

        org.bukkit.World gameWorld = getGameWorld();
        if (gameWorld != null) {
            gameWorld.setStorm(false);
            gameWorld.setThundering(false);
            gameWorld.setWeatherDuration(0);
        }

        int refreshInterval = cfg.getConfig().getInt("emerald-refresh", 10);
        if (emeraldTimer >= refreshInterval) {
            emeraldTimer = 0;
            spawnEmeralds();
        }

        updateScoreboard();
        checkWinCondition();

        if (gameCountdown <= 0) {
            endGame("人类");
        }
    }

    private void endGame(String winnerTeam) {
        state = GameState.ENDING;
        cfg.setWinner(winnerTeam);
        endingTimer = cfg.getConfig().getInt("bungee.restart-time", 10);

        boolean isLeaveWin = isLeaveWin();

        for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
            Player player = Bukkit.getPlayer(gp.getUuid());
            if (player == null) continue;

            boolean isWinner = isPlayerWinner(gp, winnerTeam);
            if (isWinner) {
                sendTitleWithTiming(player,
                    colorize(MsgFormat.msg(cfg.getConfig().getString("title.win.title", "&e&l大吉大利!你获胜了!"), player)),
                    colorize(MsgFormat.msg(cfg.getConfig().getString("title.win.subtitle", "&2&l" + winnerTeam + "方获胜!"), player)),
                    cfg.getConfig().getInt("title.win.in", 20),
                    cfg.getConfig().getInt("title.win.stay", 80),
                    cfg.getConfig().getInt("title.win.out", 20)
                );
                if (!isLeaveWin) {
                    executeReward(player, "reward.winner");
                } else {
                    executeReward(player, "reward.leave");
                }
            } else {
                sendTitleWithTiming(player,
                    colorize(MsgFormat.msg(cfg.getConfig().getString("title.lose.title", "&e&l很遗憾!你输了.."), player)),
                    colorize(MsgFormat.msg(cfg.getConfig().getString("title.lose.subtitle", "&2&l" + winnerTeam + "方获胜了.."), player)),
                    cfg.getConfig().getInt("title.lose.in", 20),
                    cfg.getConfig().getInt("title.lose.stay", 80),
                    cfg.getConfig().getInt("title.lose.out", 20)
                );

                Bukkit.getScheduler().runTaskLater(plugin, () -> openLoseMenu(player), 60L);
            }
        }

        broadcastMsg("win");
        broadcastMsg("ending");
        startFireworkLoop();

        updateScoreboard();
    }

    private void tickEnding() {
        endingTimer--;
        cfg.setGameTimer(endingTimer);
        updateScoreboard();
        if (endingTimer <= 0) {
            handleRestart();
        }
    }

    private void handleRestart() {
        boolean fullRestart = cfg.getConfig().getBoolean("bungee.full-restart", false);
        if (fullRestart) {

            String lobbyServer = cfg.getConfig().getString("bungee.lobby", "lobby");
            for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
                Player player = Bukkit.getPlayer(gp.getUuid());
                if (player != null) BungeeUtil.sendToServer(player, lobbyServer);
            }
            new BukkitRunnable() {
                @Override
                public void run() { Bukkit.shutdown(); }
            }.runTaskLater(plugin, 60L);
        } else {
            resetGame();
        }
    }

    public void resetGame() {
        stopMainLoop();
        stopFireworkLoop();
        for (int id : kitTasks) Bukkit.getScheduler().cancelTask(id);
        kitTasks.clear();
        cancelAllTrackerTasks();

        if (cfg.getConfig().getBoolean("endclear", true)) {
            for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
                Player player = Bukkit.getPlayer(gp.getUuid());
                if (player != null) {
                    player.getInventory().clear();
                    resetNameTag(player);
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
            }
        }
        cfg.getAllGamePlayers().clear();
        cfg.setWinner(null);
        quitDuringGame.clear();
        state = GameState.WAITING;

        Player anyPlayer = null;
        org.bukkit.World gameWorld = getGameWorld();
        for (org.bukkit.entity.Entity ent : gameWorld.getEntities()) {
            if (ent instanceof Player) {
                anyPlayer = (Player) ent;
                break;
            }
        }
        if (anyPlayer != null) {
            Bukkit.dispatchCommand(anyPlayer, "removecorpse 500");
        }
    }

    private List<String> buildKitList(int playerCount) {
        List<String> kits = new ArrayList<>();
        ConfigurationSection kitCfg = cfg.getKitConfig();

        int werewolfMax = kitCfg.getInt("werewolf.max", 2);
        int hunterMax = kitCfg.getInt("hunter.max", 2);
        int seerMax = kitCfg.getInt("seer.max", 2);

        int werewolfMin, hunterMin, seerMin;
        if (playerCount <= 12) {
            werewolfMin = 1; hunterMin = 1; seerMin = 1;
        } else if (playerCount < 16) {
            werewolfMin = 1; hunterMin = 1; seerMin = 1;
        } else if (playerCount < 20) {
            werewolfMin = 2; hunterMin = 2; seerMin = 1;
        } else {
            werewolfMin = 2; hunterMin = 2; seerMin = 2;
        }

        werewolfMin = Math.min(werewolfMin, werewolfMax);
        hunterMin = Math.min(hunterMin, hunterMax);
        seerMin = Math.min(seerMin, seerMax);

        for (int i = 0; i < werewolfMin; i++) kits.add("werewolf");
        for (int i = 0; i < hunterMin; i++) kits.add("hunter");
        for (int i = 0; i < seerMin; i++) kits.add("seer");
        while (kits.size() < playerCount) kits.add("normal");
        while (kits.size() > playerCount) kits.remove(kits.size() - 1);

        return kits;
    }

    private void scheduleKitTasks() {
        String[] specialKits = {"werewolf", "hunter", "seer"};
        for (String kitId : specialKits) {
            int gt = cfg.getKitGT(kitId);
            if (gt <= 0) continue;
            int taskId = new BukkitRunnable() {
                @Override
                public void run() {
                    if (state != GameState.GAMING) return;
                    for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
                        if (!gp.isAlive() || !gp.getKitId().equals(kitId)) continue;
                        Player p = Bukkit.getPlayer(gp.getUuid());
                        if (p == null) continue;
                        giveKitGTItems(p, gp);
                        sendMsg(p, "kit.get-item." + kitId);
                        sendTitle(p, "getitem");
                    }
                }
            }.runTaskLater(plugin, gt * 20L).getTaskId();
            kitTasks.add(taskId);
        }
    }

    private void giveBeforeStartItems(Player player) {
        List<String> items = cfg.getConfig().getStringList("item.beforestart");
        for (String entry : items) {
            String[] parts = entry.split(",");
            if (parts.length < 2) continue;
            int slot = Integer.parseInt(parts[0].trim());
            String itemId = parts[1].trim();
            ItemStack item = buildItem(itemId, player);
            if (item != null) player.getInventory().setItem(slot, item);
        }
    }

    private void giveGamingItems(Player player) {
        List<String> items = cfg.getConfig().getStringList("item.gaming");
        for (String entry : items) {
            String[] parts = entry.split(",");
            if (parts.length < 2) continue;
            int slot = Integer.parseInt(parts[0].trim());
            String itemId = parts[1].trim();
            ItemStack item = buildItem(itemId, player);
            if (item != null) player.getInventory().setItem(slot, item);
        }
    }

    private void giveColorArmor(Player player, GamePlayer gp) {
        java.util.Map<?, ?> colorMap = cfg.getColorMap(String.valueOf(gp.getColorId()));
        if (colorMap == null) return;
        String hex = String.valueOf(colorMap.get("color") != null ? colorMap.get("color") : "#FFFFFF");
        int rgb;
        try {
            rgb = Integer.parseInt(hex.replace("#", ""), 16);
        } catch (Exception e) {
            rgb = 0xFFFFFF;
        }
        org.bukkit.Color bukkitColor = org.bukkit.Color.fromRGB(rgb);
        Material[] armorMats = {
            Material.LEATHER_BOOTS,
            Material.LEATHER_LEGGINGS,
            Material.LEATHER_CHESTPLATE,
            Material.LEATHER_HELMET
        };
        int[] armorSlots = {36, 37, 38, 39};
        for (int i = 0; i < armorMats.length; i++) {
            ItemStack armor = new ItemStack(armorMats[i], 1);
            LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
            meta.setColor(bukkitColor);
            armor.setItemMeta(meta);
            player.getInventory().setItem(armorSlots[i], armor);
        }
    }

    private void giveKitStartItems(Player player, GamePlayer gp) {
        sendMsg(player, "kit.get-item-tip." + gp.getKitId());
        int gt = cfg.getKitGT(gp.getKitId());
        if (gt <= 0) {
            giveKitGTItems(player, gp);
        }
    }

    private void giveKitGTItems(Player player, GamePlayer gp) {
        ConfigurationSection kitSec = cfg.getKitSection(gp.getKitId());
        if (kitSec == null) return;
        List<String> items = kitSec.getStringList("items");
        for (String entry : items) {
            String[] parts = entry.split(",");
            if (parts.length < 2) continue;
            int slot = Integer.parseInt(parts[0].trim());
            String itemId = parts[1].trim();
            ItemStack item = buildItem(itemId, player);
            if (item != null) player.getInventory().setItem(slot, item);
        }
    }

    public ItemStack buildItem(String itemId, Player player) {
        ConfigurationSection sec = cfg.getItemSection(itemId);
        if (sec == null) return null;

        String matName = sec.getString("material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE;

        int amount = sec.getInt("amount", 1);
        short data = (short) sec.getInt("data", 0);

        ItemStack item;
        if (mat == Material.WRITTEN_BOOK && itemId.startsWith("book")) {
            String bookName = sec.getString("name", "");
            String colorizedName = (bookName != null && !bookName.isEmpty()) ? colorize(MsgFormat.msg(bookName, player)) : null;
            item = GameBook.createBook(itemId, colorizedName);
            if (item == null) item = new ItemStack(mat, amount, data);
        } else {
            item = new ItemStack(mat, amount, data);
        }

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = sec.getString("name", "");
        if (name != null && !name.isEmpty()) {
            meta.setDisplayName(colorize(MsgFormat.msg(name, player)));
        }

        List<String> lore = sec.getStringList("lore");
        if (lore != null && !lore.isEmpty()) {
            List<String> newLore = new ArrayList<>();
            for (String line : lore) {
                newLore.add(colorize(MsgFormat.msg(line, player)));
            }
            meta.setLore(newLore);
        }

        String enchStr = sec.getString("enchant", "");
        if (enchStr != null && !enchStr.isEmpty()) {
            for (String ench : enchStr.split(";")) {
                String[] ep = ench.split(",");
                if (ep.length >= 2) {
                    try {
                        org.bukkit.enchantments.Enchantment e = org.bukkit.enchantments.Enchantment.getByName(ep[0].trim());
                        int level = Integer.parseInt(ep[1].trim());
                        if (e != null) meta.addEnchant(e, level, true);
                    } catch (Exception ignored) {}
                }
            }
        }

        if (meta instanceof LeatherArmorMeta && sec.contains("color")) {
            String colorStr = sec.getString("color", "");
            colorStr = colorize(MsgFormat.msg(colorStr, player));
            colorStr = colorStr.replace("§", "");

            try {
                int rgb = Integer.parseInt(colorStr.replace("#", ""), 16);
                ((LeatherArmorMeta) meta).setColor(Color.fromRGB(rgb));
            } catch (Exception ignored) {}
        }

        item.setItemMeta(meta);
        return item;
    }

    private void hideNameTag(Player player) {
        org.bukkit.scoreboard.Scoreboard board = player.getScoreboard();
        org.bukkit.scoreboard.Team team = board.getTeam("wr_hide");
        if (team == null) {
            team = board.registerNewTeam("wr_hide");
            team.setNameTagVisibility(org.bukkit.scoreboard.NameTagVisibility.NEVER);
        }
        team.addEntry(player.getName());
    }

    private void resetNameTag(Player player) {
        org.bukkit.scoreboard.Scoreboard board = player.getScoreboard();
        org.bukkit.scoreboard.Team team = board.getTeam("wr_hide");
        if (team != null) team.removeEntry(player.getName());
    }

    private void setupWerewolfTeams() {
        for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
            Player player = Bukkit.getPlayer(gp.getUuid());
            if (player == null) continue;
            org.bukkit.scoreboard.Scoreboard board = player.getScoreboard();
            org.bukkit.scoreboard.Team wolfTeam = board.getTeam("wr_wolf");
            if (wolfTeam == null) {
                wolfTeam = board.registerNewTeam("wr_wolf");
                wolfTeam.setPrefix(ChatColor.GREEN + "");
                wolfTeam.setNameTagVisibility(org.bukkit.scoreboard.NameTagVisibility.ALWAYS);
            }
            if (gp.getKitId().equals("werewolf")) {
                wolfTeam.addEntry(player.getName());
            }
        }
    }

    private void showRoleInfo(Player player, GamePlayer gp) {
        ConfigurationSection kitSec = cfg.getKitSection(gp.getKitId());
        if (kitSec == null) return;
        String format = kitSec.getString("format", "");
        String describe = kitSec.getString("describe", "");
        String task = kitSec.getString("task", "");
        sendTitleWithTiming(player,
            colorize(MsgFormat.msg("&f本局你是..&l" + format + "!", player)),
            colorize(MsgFormat.msg(describe, player)),
            20, 60, 20
        );
    }

    private void spawnEmeralds() {
        if (currentMap == null) return;
        for (Location loc : currentMap.getEmeraldSpawns()) {

            for (org.bukkit.entity.Entity ent : loc.getWorld().getNearbyEntities(loc, 0.5, 0.5, 0.5)) {
                if (ent.getType() == EntityType.DROPPED_ITEM) {
                    org.bukkit.entity.Item item = (org.bukkit.entity.Item) ent;
                    if (item.getItemStack().getType() == Material.EMERALD) {
                        ent.remove();
                    }
                }
            }

            ItemStack emerald = buildItem("emerald", null);
            if (emerald != null) {
                loc.getWorld().dropItemNaturally(loc, emerald);
            }
        }
    }

    private void startFireworkLoop() {
        fireworkTaskId = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (ticks > endingTimer * 2) { cancel(); return; }
                for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
                    if (!gp.isAlive()) continue;
                    Player p = Bukkit.getPlayer(gp.getUuid());
                    if (p == null) continue;

                    if (isPlayerWinner(gp, cfg.getWinner())) {
                        spawnFirework(p.getLocation());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L).getTaskId();
    }

    private void stopFireworkLoop() {
        if (fireworkTaskId != -1) {
            Bukkit.getScheduler().cancelTask(fireworkTaskId);
            fireworkTaskId = -1;
        }
    }

    private void spawnFirework(Location loc) {
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK);
        org.bukkit.inventory.meta.FireworkMeta fmeta = fw.getFireworkMeta();
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.PURPLE, Color.ORANGE};
        Color c1 = colors[new Random().nextInt(colors.length)];
        Color c2 = colors[new Random().nextInt(colors.length)];
        fmeta.addEffect(FireworkEffect.builder()
            .with(FireworkEffect.Type.BALL_LARGE)
            .withColor(c1).withFade(c2).trail(true).flicker(true).build());
        fmeta.setPower(1);
        fw.setFireworkMeta(fmeta);
        fw.detonate();
    }

    private void checkWinCondition() {
        if (state != GameState.GAMING) return;
        boolean hasWerewolf = false;
        boolean hasHuman = false;
        for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
            if (!gp.isAlive()) continue;
            if (gp.getKitId().equals("werewolf")) hasWerewolf = true;
            else hasHuman = true;
        }
        if (!hasWerewolf) endGame("人类");
        else if (!hasHuman) endGame("狼人");
    }

    private boolean isLeaveWin() {
        boolean hasWerewolf = false;
        boolean hasHuman = false;
        for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
            if (gp.getKitId().equals("werewolf")) hasWerewolf = true;
            else hasHuman = true;
        }
        if (!hasWerewolf) {
            for (Map.Entry<UUID, String> entry : quitDuringGame.entrySet()) {
                if (entry.getValue().equals("werewolf")) return true;
            }
        }
        if (!hasHuman) {
            for (Map.Entry<UUID, String> entry : quitDuringGame.entrySet()) {
                if (!entry.getValue().equals("werewolf")) return true;
            }
        }
        return false;
    }

    private boolean isPlayerWinner(GamePlayer gp, String winnerTeam) {
        if (!gp.isAlive()) return false;
        if (winnerTeam.equals("狼人")) return gp.getKitId().equals("werewolf");
        return !gp.getKitId().equals("werewolf");
    }

    private void executeReward(Player player, String rewardPath) {
        List<String> rewards = cfg.getConfig().getStringList(rewardPath);
        for (String cmd : rewards) {
            if (cmd.startsWith("[CMD]")) {
                String c = cmd.substring(5).trim().replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
            } else if (cmd.startsWith("[MSG]")) {
                String m = cmd.substring(5).trim();
                player.sendMessage(colorize(MsgFormat.msg(m, player)));
            }
        }
    }

    private void updateScoreboard() {
        String path;
        switch (state) {
            case WAITING: path = "scoreboard.waiting"; break;
            case STARTING: path = "scoreboard.starting"; break;
            case GAMING: path = "scoreboard.gaming"; break;
            case ENDING: path = "scoreboard.ending"; break;
            default: path = "scoreboard.waiting";
        }

        String gamingPath = path;
        String title = cfg.getConfig().getString(path + ".title", "");
        List<String> content = cfg.getConfig().getStringList(path + ".content");

        for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
            Player player = Bukkit.getPlayer(gp.getUuid());
            if (player == null) continue;
            String usePath = gamingPath;
            if (state == GameState.GAMING && !gp.isAlive()) {
                usePath = "scoreboard.spectating";
            }

            String pTitle = colorize(MsgFormat.msg(cfg.getConfig().getString(usePath + ".title", ""), player));
            List<String> pContent = new ArrayList<>();
            for (String line : cfg.getConfig().getStringList(usePath + ".content")) {
                pContent.add(colorize(MsgFormat.msg(line, player)));
            }

            GameScoreboard sb = getOrCreateScoreboard(player, pTitle);
            sb.setTitle(pTitle);
            sb.updateLines(pContent);
            sb.send(player);
        }
    }

    private final Map<UUID, String> quitDuringGame = new HashMap<>();

    private final Map<UUID, GameScoreboard> scoreboards = new HashMap<>();
    private final Map<UUID, Integer> trackerTasks = new HashMap<>();

    private GameScoreboard getOrCreateScoreboard(Player player, String title) {
        return scoreboards.computeIfAbsent(player.getUniqueId(), k -> new GameScoreboard(title));
    }

    private org.bukkit.World getGameWorld() {
        if (currentMap != null && currentMap.getRegion() != null && !currentMap.getRegion().isEmpty()) {
            org.bukkit.World world = Bukkit.getWorld(currentMap.getRegion());
            if (world != null) return world;
        }
        return Bukkit.getWorlds().get(0);
    }

    public void broadcastMsg(String key) {
        broadcastMsg(key, null);
    }

    public void broadcastMsg(String key, Player eventPlayer) {
        String msg = cfg.getMsg(key);
        if (msg.isEmpty()) return;
        for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p != null) {
                p.sendMessage(colorize(MsgFormat.msg(msg, p, eventPlayer)));
            }
        }
    }

    public void sendMsg(Player player, String key) {
        String msg = cfg.getMsg(key);
        if (!msg.isEmpty()) {
            player.sendMessage(colorize(MsgFormat.msg(msg, player)));
        }
    }

    public void broadcastTitle(String titleKey) {
        broadcastTitle(titleKey, null, null);
    }

    public void broadcastTitle(String titleKey, Player eventPlayer) {
        broadcastTitle(titleKey, eventPlayer, null);
    }

    public void broadcastTitle(String titleKey, Player eventPlayer, org.bukkit.Location eventLocation) {
        String path = "title." + titleKey;
        String title = cfg.getConfig().getString(path + ".title", "");
        String subtitle = cfg.getConfig().getString(path + ".subtitle", "");
        int in = cfg.getConfig().getInt(path + ".in", 20);
        int stay = cfg.getConfig().getInt(path + ".stay", 60);
        int out = cfg.getConfig().getInt(path + ".out", 20);
        for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p != null) {
                sendTitleWithTiming(p,
                    colorize(MsgFormat.msg(title, p, eventPlayer, eventLocation)),
                    colorize(MsgFormat.msg(subtitle, p, eventPlayer, eventLocation)),
                    in, stay, out
                );
            }
        }
    }

    public void sendTitle(Player player, String titleKey) {
        String path = "title." + titleKey;
        String title = cfg.getConfig().getString(path + ".title", "");
        String subtitle = cfg.getConfig().getString(path + ".subtitle", "");
        int in = cfg.getConfig().getInt(path + ".in", 20);
        int stay = cfg.getConfig().getInt(path + ".stay", 60);
        int out = cfg.getConfig().getInt(path + ".out", 20);
        sendTitleWithTiming(player,
            colorize(MsgFormat.msg(title, player)),
            colorize(MsgFormat.msg(subtitle, player)),
            in, stay, out
        );
    }

    public void handlePlayAgain(Player player) {
        String group = cfg.getConfig().getString("bungee.group", "whosrotten");
        BungeeUtil.sendPlayAgain(player, group);
    }

    public void handleLeaveGame(Player player) {
        String lobby = cfg.getConfig().getString("bungee.lobby", "lobby");
        BungeeUtil.sendToServer(player, lobby);
    }

    public void openMenu(Player player, String menuKey) {
        ConfigurationSection menuSec = cfg.getMenuConfig().getConfigurationSection(menuKey);
        if (menuSec == null) return;
        String title = menuSec.getString("title", menuKey);
        int slot = menuSec.getInt("slot", 27);
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, slot,
            ChatColor.translateAlternateColorCodes('&', title));

        ConfigurationSection closeSec = cfg.getMenuConfig().getConfigurationSection("close");
        if (closeSec != null) {
            Material closeMat = Material.matchMaterial(closeSec.getString("material", "STAINED_GLASS_PANE"));
            short closeData = (short) closeSec.getInt("data", 14);
            ItemStack closeItem = new ItemStack(closeMat != null ? closeMat : Material.STAINED_GLASS_PANE, 1, closeData);
            org.bukkit.inventory.meta.ItemMeta closeMeta = closeItem.getItemMeta();
            closeMeta.setDisplayName(colorize(closeSec.getString("name", "&c关闭")));
            closeItem.setItemMeta(closeMeta);
            inv.setItem(slot - 1, closeItem);
        }

        ConfigurationSection fillerSec = cfg.getMenuConfig().getConfigurationSection("filler");
        if (fillerSec != null) {
            Material fillMat = Material.matchMaterial(fillerSec.getString("material", "STAINED_GLASS_PANE"));
            short fillData = (short) fillerSec.getInt("data", 15);
            ItemStack filler = new ItemStack(fillMat != null ? fillMat : Material.STAINED_GLASS_PANE, 1, fillData);
            for (int i = 0; i < slot; i++) {
                if (inv.getItem(i) == null) inv.setItem(i, filler);
            }
        }

        List<String> items = menuSec.getStringList("items");
        ConfigurationSection shopPrice = cfg.getMenuConfig().getConfigurationSection("shop.price");
        String priceLore = shopPrice != null ? shopPrice.getString("price", "&a价格&e&l%price%&r&a宝石") : "&a价格&e&l%price%&r&a宝石";

        for (String entry : items) {
            String[] parts = entry.split(",");
            if (parts.length < 3) continue;
            int itemSlot = Integer.parseInt(parts[0].trim());
            String itemId = parts[1].trim();
            int price = Integer.parseInt(parts[2].trim());

            ItemStack item = buildItem(itemId, player);
            if (item == null) continue;

            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                java.util.List<String> lore = meta.hasLore() ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
                lore.add(colorize(priceLore.replace("%price%", String.valueOf(price))));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(itemSlot, item);
        }

        player.openInventory(inv);
    }

    public void openSeerMenu(Player player) {
        ConfigurationSection seerMenu = cfg.getMenuConfig().getConfigurationSection("item.seer");
        if (seerMenu == null) return;

        String title = seerMenu.getString("title", "&5请选择你要查验身份的人");
        int slot = seerMenu.getInt("slot", 27);
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, slot,
            ChatColor.translateAlternateColorCodes('&', title));

        GamePlayer seerGp = cfg.getGamePlayer(player.getUniqueId());
        if (seerGp == null) return;

        ConfigurationSection unknownSec = seerMenu.getConfigurationSection("player_unknown");
        ConfigurationSection knownSec = seerMenu.getConfigurationSection("player_known");

        int index = 0;
        for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
            if (gp.getUuid().equals(player.getUniqueId())) continue;
            if (!gp.isAlive()) continue;
            if (index >= slot - 1) break;

            boolean known = seerGp.isKnown(gp.getUuid());
            ConfigurationSection useSec = known ? knownSec : unknownSec;
            if (useSec == null) continue;

            Player target = Bukkit.getPlayer(gp.getUuid());
            if (target == null) continue;

            Material mat = Material.matchMaterial(useSec.getString("material", "LEATHER_CHESTPLATE"));
            ItemStack item = new ItemStack(mat != null ? mat : Material.LEATHER_CHESTPLATE);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

            String name = useSec.getString("name", "");
            name = name.replace("%player%", target.getName());

            java.util.Map<?, ?> colorMap = cfg.getColorMap(String.valueOf(gp.getColorId()));
            String cText = colorMap != null ? String.valueOf(colorMap.containsKey("text") ? colorMap.get("text") : "") : "";
            String c16 = colorMap != null ? String.valueOf(colorMap.containsKey("color") ? colorMap.get("color") : "#FFFFFF") : "#FFFFFF";
            String cFormat = colorMap != null ? String.valueOf(colorMap.containsKey("format") ? colorMap.get("format") : "") : "";
            ConfigurationSection kitSec = cfg.getKitSection(gp.getKitId());
            String kitText = kitSec != null ? kitSec.getString("name", "") : "";
            String kitFormat = kitSec != null ? kitSec.getString("format", "") : "";
            String kitDesc = kitSec != null ? kitSec.getString("describe", "") : "";

            name = name.replace("%player.color.text%", cText);
            name = name.replace("%player.color.16%", c16);
            name = name.replace("%player.color.format%", cFormat);
            name = name.replace("%player.kit.text%", kitText);
            name = name.replace("%player.kit.format%", kitFormat);
            name = name.replace("%player.kit.describe%", kitDesc);
            meta.setDisplayName(colorize(name));

            java.util.List<String> lore = useSec.getStringList("lore");
            java.util.List<String> newLore = new java.util.ArrayList<>();
            for (String line : lore) {
                line = line.replace("%player%", target.getName());
                line = line.replace("%player.color.text%", cText);
                line = line.replace("%player.color.16%", c16);
                line = line.replace("%player.color.format%", cFormat);
                line = line.replace("%player.kit.text%", kitText);
                line = line.replace("%player.kit.format%", kitFormat);
                line = line.replace("%player.kit.describe%", kitDesc);
                newLore.add(colorize(line));
            }
            meta.setLore(newLore);

            if (meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta) {
                try {
                    int rgb = MsgFormat.hexToDec(c16);
                    ((org.bukkit.inventory.meta.LeatherArmorMeta) meta).setColor(org.bukkit.Color.fromRGB(rgb));
                } catch (Exception ignored) {}
            }

            if (known && useSec.contains("enchant")) {
                String enchStr = useSec.getString("enchant", "");
                for (String ench : enchStr.split(";")) {
                    String[] ep = ench.split(",");
                    if (ep.length >= 2) {
                        try {
                            org.bukkit.enchantments.Enchantment e = org.bukkit.enchantments.Enchantment.getByName(ep[0].trim());
                            int level = Integer.parseInt(ep[1].trim());
                            if (e != null) meta.addEnchant(e, level, true);
                        } catch (Exception ignored) {}
                    }
                }
            }

            item.setItemMeta(meta);
            inv.setItem(index, item);
            index++;
        }

        ConfigurationSection fillerSec = cfg.getMenuConfig().getConfigurationSection("filler");
        if (fillerSec != null) {
            Material fillMat = Material.matchMaterial(fillerSec.getString("material", "STAINED_GLASS_PANE"));
            short fillData = (short) fillerSec.getInt("data", 15);
            ItemStack filler = new ItemStack(fillMat != null ? fillMat : Material.STAINED_GLASS_PANE, 1, fillData);
            for (int i = index; i < slot; i++) inv.setItem(i, filler);
        }

        ConfigurationSection closeSec = cfg.getMenuConfig().getConfigurationSection("close");
        if (closeSec != null) {
            Material closeMat = Material.matchMaterial(closeSec.getString("material", "STAINED_GLASS_PANE"));
            short closeData = (short) closeSec.getInt("data", 14);
            ItemStack closeItem = new ItemStack(closeMat != null ? closeMat : Material.STAINED_GLASS_PANE, 1, closeData);
            org.bukkit.inventory.meta.ItemMeta closeMeta = closeItem.getItemMeta();
            closeMeta.setDisplayName(colorize(closeSec.getString("name", "&c关闭")));
            closeItem.setItemMeta(closeMeta);
            inv.setItem(slot - 1, closeItem);
        }

        player.openInventory(inv);
    }

    public void sendAllTitle(String titleKey, Player eventPlayer) {
        String path = "title." + titleKey;
        String title = cfg.getConfig().getString(path + ".title", "");
        String subtitle = cfg.getConfig().getString(path + ".subtitle", "");
        int in = cfg.getConfig().getInt(path + ".in", 20);
        int stay = cfg.getConfig().getInt(path + ".stay", 60);
        int out = cfg.getConfig().getInt(path + ".out", 20);
        for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
            Player p = Bukkit.getPlayer(gp.getUuid());
            if (p != null) {
                sendTitleWithTiming(p,
                    colorize(MsgFormat.msg(title, p, eventPlayer)),
                    colorize(MsgFormat.msg(subtitle, p, eventPlayer)),
                    in, stay, out
                );
            }
        }
    }

    public void openLoseMenu(Player player) {
        ConfigurationSection loseSec = cfg.getConfig().getConfigurationSection("lose");
        if (loseSec == null) return;
        String title = loseSec.getString("title", "游戏结束");
        int slot = loseSec.getInt("slot", 27);
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, slot,
            ChatColor.translateAlternateColorCodes('&', title));

        ConfigurationSection specSec = loseSec.getConfigurationSection("spec");
        if (specSec != null) {
            Material mat = Material.matchMaterial(specSec.getString("material", "EYE_OF_ENDER"));
            ItemStack item = new ItemStack(mat != null ? mat : Material.EYE_OF_ENDER);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(colorize(specSec.getString("name", "继续旁观")));
            item.setItemMeta(meta);
            inv.setItem(specSec.getInt("slot", 10), item);
        }

        ConfigurationSection againSec = loseSec.getConfigurationSection("again");
        if (againSec != null) {
            Material mat = Material.matchMaterial(againSec.getString("material", "SLIME_BALL"));
            ItemStack item = new ItemStack(mat != null ? mat : Material.SLIME_BALL);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(colorize(againSec.getString("name", "再来一局")));
            item.setItemMeta(meta);
            inv.setItem(againSec.getInt("slot", 13), item);
        }

        ConfigurationSection leaveSec = loseSec.getConfigurationSection("leave");
        if (leaveSec != null) {
            Material mat = Material.matchMaterial(leaveSec.getString("material", "BED"));
            ItemStack item = new ItemStack(mat != null ? mat : Material.BED);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(colorize(leaveSec.getString("name", "离开游戏")));
            item.setItemMeta(meta);
            inv.setItem(leaveSec.getInt("slot", 16), item);
        }

        ConfigurationSection fillerSec = cfg.getMenuConfig().getConfigurationSection("filler");
        if (fillerSec != null) {
            Material fillMat = Material.matchMaterial(fillerSec.getString("material", "STAINED_GLASS_PANE"));
            short fillData = (short) fillerSec.getInt("data", 15);
            ItemStack filler = new ItemStack(fillMat != null ? fillMat : Material.STAINED_GLASS_PANE, 1, fillData);
            for (int i = 0; i < slot; i++) {
                if (inv.getItem(i) == null) inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    private void sendTitleWithTiming(Player player, String title, String subtitle, int fadeIn, int stay, int out) {
        try {
            String pkgName = player.getClass().getPackage().getName();
            String nmsVersion = null;
            for (String part : pkgName.split("\\.")) {
                if (part.matches("v\\d+_\\d+_R\\d+")) { nmsVersion = part; break; }
            }
            if (nmsVersion == null) return;
            Class<?> packetClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutTitle");
            Class<?> enumClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutTitle$EnumTitleAction");
            Class<?> chatSerializer = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent$ChatSerializer");
            Class<?> chatComponent = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent");

            Object timesPacket = packetClass.getConstructor(enumClass, chatComponent, int.class, int.class, int.class)
                    .newInstance(enumClass.getEnumConstants()[2], null, fadeIn, stay, out);

            Object titleComp = title != null ? chatSerializer.getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + title.replace("\"", "\\\"") + "\"}") : null;
            Object subtitleComp = subtitle != null ? chatSerializer.getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + subtitle.replace("\"", "\\\"") + "\"}") : null;

            Object titlePacket = packetClass.getConstructor(enumClass, chatComponent, int.class, int.class, int.class)
                    .newInstance(enumClass.getEnumConstants()[0], titleComp, fadeIn, stay, out);
            Object subtitlePacket = packetClass.getConstructor(enumClass, chatComponent, int.class, int.class, int.class)
                    .newInstance(enumClass.getEnumConstants()[1], subtitleComp, fadeIn, stay, out);

            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
            Class<?> basePacket = Class.forName("net.minecraft.server." + nmsVersion + ".Packet");
            java.lang.reflect.Method sendPacket = connection.getClass().getMethod("sendPacket", basePacket);

            sendPacket.invoke(connection, timesPacket);
            if (titleComp != null) sendPacket.invoke(connection, titlePacket);
            if (subtitleComp != null) sendPacket.invoke(connection, subtitlePacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendActionBar(Player player, String text) {
        try {
            String pkgName = player.getClass().getPackage().getName();
            String nmsVersion = null;
            for (String part : pkgName.split("\\.")) {
                if (part.matches("v\\d+_\\d+_R\\d+")) { nmsVersion = part; break; }
            }
            if (nmsVersion == null) return;
            Class<?> chatSerializer = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent$ChatSerializer");
            Class<?> chatComponent = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent");
            Class<?> packetClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutChat");

            Object component = chatSerializer.getMethod("a", String.class)
                .invoke(null, "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
            Object packet = packetClass.getConstructor(chatComponent, byte.class)
                .newInstance(component, (byte) 2);

            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
            Class<?> basePacket = Class.forName("net.minecraft.server." + nmsVersion + ".Packet");
            java.lang.reflect.Method sendPacket = connection.getClass().getMethod("sendPacket", basePacket);
            sendPacket.invoke(connection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void putTrackerTask(UUID uuid, int taskId) {
        Integer old = trackerTasks.remove(uuid);
        if (old != null) Bukkit.getScheduler().cancelTask(old);
        trackerTasks.put(uuid, taskId);
    }

    public void removeTrackerTask(UUID uuid) {
        Integer old = trackerTasks.remove(uuid);
        if (old != null) Bukkit.getScheduler().cancelTask(old);
    }

    public void cancelAllTrackerTasks() {
        for (int taskId : trackerTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        trackerTasks.clear();
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
