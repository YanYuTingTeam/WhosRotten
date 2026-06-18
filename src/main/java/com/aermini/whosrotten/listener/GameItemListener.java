package com.aermini.whosrotten.listener;

import com.aermini.whosrotten.MsgFormat;
import com.aermini.whosrotten.WhosRotten;
import com.aermini.whosrotten.game.GameBook;
import com.aermini.whosrotten.game.GameManager;
import com.aermini.whosrotten.game.GamePlayer;
import com.aermini.whosrotten.manager.ConfigManager;
import com.aermini.whosrotten.util.PacketUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.LargeFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GameItemListener implements Listener {
    private final WhosRotten plugin;
    private final ConfigManager cfg;

    public GameItemListener(WhosRotten plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        GameManager.GameState state = plugin.getGameManager().getState();

        if (isBook(item)) {
            handleBook(player, item);
            return;
        }

        if (item.getType() == Material.DIAMOND && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().contains("开始游戏")) {
            event.setCancelled(true);
            if (state == GameManager.GameState.WAITING || state == GameManager.GameState.STARTING) {
                plugin.getGameManager().forceStart();
            }
            return;
        }

        if (item.getType() == Material.REDSTONE_BLOCK && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().contains("离开游戏")) {
            event.setCancelled(true);
            if (state == GameManager.GameState.WAITING || state == GameManager.GameState.STARTING
                    || state == GameManager.GameState.GAMING || state == GameManager.GameState.ENDING) {
                plugin.getGameManager().handleLeaveGame(player);
            }
            return;
        }

        if (item.getType() == Material.BED && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().contains("再来一局")) {
            event.setCancelled(true);
            plugin.getGameManager().handlePlayAgain(player);
            return;
        }

        if (state != GameManager.GameState.GAMING) return;

        if (item.getType() == Material.DIAMOND_SWORD && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().contains("狼人的爪子")) {
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                    || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                handleFireball(player);
            }
            return;
        }

        if (item.getType() == Material.PAPER && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().contains("传送卷轴")) {
            event.setCancelled(true);
            handleTeleport(player);
            consumeItem(player, item);
            return;
        }

        if (item.getType() == Material.COMPASS && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().contains("狼人探测器")) {
            event.setCancelled(true);
            handleDiscover(player);
            return;
        }

        if (item.getType() == Material.COMPASS && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().contains("玩家探测器")) {
            event.setCancelled(true);
            handleTracker(player);
            return;
        }

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().contains("预言魔杖")) {
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                    || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                handleSeerItem(player);
            }
            return;
        }

        if (item.hasItemMeta()) {
            ConfigurationSection itemSec = findItemByDisplayName(item);
            if (itemSec != null) {
                event.setCancelled(true);
                if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                        || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                    String menuKey = itemSec.getString("menu");
                    plugin.getGameManager().openMenu(player, menuKey);
                }
                return;
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() == Material.AIR) return;
        if (isShopItem(item)) {
            event.setCancelled(true);
            ConfigurationSection itemSec = findItemByDisplayName(item);
            if (itemSec != null) {
                String menuKey = itemSec.getString("menu");
                if (menuKey != null) plugin.getGameManager().openMenu(player, menuKey);
            }
        }
    }

    private boolean isShopItem(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        String stripped = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        ConfigurationSection items = cfg.getItemConfig();
        for (String key : items.getKeys(false)) {
            ConfigurationSection sec = items.getConfigurationSection(key);
            if (sec == null || !sec.contains("menu")) continue;
            String cfgMat = sec.getString("material", "");
            Material matchedMat = cfgMat != null && !cfgMat.isEmpty() ? Material.matchMaterial(cfgMat) : null;
            if (matchedMat != null && item.getType() != matchedMat) continue;
            String cfgName = sec.getString("name", "");
            String cfgStripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', cfgName));
            if (cfgStripped.equals(stripped)) return true;
        }
        return false;
    }

    private boolean isBook(ItemStack item) {
        return item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.BOOK_AND_QUILL;
    }

    private void handleBook(Player player, ItemStack item) {
        String bookId = null;
        String bookName = null;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            bookName = item.getItemMeta().getDisplayName();
            String name = ChatColor.stripColor(bookName);
            if (name.contains("狼人指南")) bookId = "book_w";
            else if (name.contains("猎人指南")) bookId = "book_h";
            else if (name.contains("预言家指南")) bookId = "book_s";
            else if (name.contains("平民指南")) bookId = "book_n";
            else if (name.contains("狼人杀指南")) bookId = "book";
        }
        if (bookId != null) {
            ItemStack book = GameBook.createBook(bookId, bookName);
            if (book != null) {
                org.bukkit.inventory.PlayerInventory inv = player.getInventory();
                for (int i = 0; i < inv.getSize(); i++) {
                    ItemStack slotItem = inv.getItem(i);
                    if (slotItem != null && slotItem.isSimilar(item)) {
                        inv.setItem(i, book);
                        break;
                    }
                }
                player.updateInventory();
            }
        }
    }

    private void handleFireball(Player player) {
        GamePlayer gp = cfg.getGamePlayer(player.getUniqueId());
        if (gp == null || !gp.getKitId().equals("werewolf")) return;

        if (!gp.isCooldownReady("sword")) {
            int remain = gp.getCooldownRemain("sword");
            String msg = cfg.getMsg("cooldown").replace("%cooldown%", String.valueOf(remain));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', MsgFormat.msg(msg, player)));
            return;
        }

        Vector dir = player.getLocation().getDirection().normalize();
        Location loc = player.getEyeLocation().add(dir);
        LargeFireball fireball = player.getWorld().spawn(loc, LargeFireball.class);
        fireball.setShooter(player);
        fireball.setDirection(dir);
        fireball.setVelocity(dir);
        fireball.setIsIncendiary(false);
        fireball.setYield(0);

        ConfigurationSection swordSec = cfg.getItemSection("sword");
        long cd = swordSec != null ? swordSec.getLong("cd", 30) * 1000 : 30000;
        gp.setCooldown("sword", System.currentTimeMillis() + cd);
    }

    private void handleTeleport(Player player) {
        GameManager gm = plugin.getGameManager();
        if (gm.getCurrentMap() == null) return;
        java.util.List<Location> spawns = gm.getCurrentMap().getSpawnPoints();
        if (spawns.isEmpty()) return;
        Location target = spawns.get(new java.util.Random().nextInt(spawns.size()));
        player.teleport(target);
    }

    private void handleDiscover(Player player) {
        GamePlayer gp = cfg.getGamePlayer(player.getUniqueId());
        if (gp == null) return;

        if (!gp.isCooldownReady("discover")) {
            int remain = gp.getCooldownRemain("discover");
            String msg = cfg.getMsg("cooldown").replace("%cooldown%", String.valueOf(remain));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', MsgFormat.msg(msg, player)));
            return;
        }

        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        for (GamePlayer wgp : cfg.getAllGamePlayers().values()) {
            if (!wgp.isAlive() || !wgp.getKitId().equals("werewolf")) continue;
            Player wp = Bukkit.getPlayer(wgp.getUuid());
            if (wp == null || wp.equals(player)) continue;
            double dist = player.getLocation().distance(wp.getLocation());
            if (dist < minDist) {
                minDist = dist;
                nearest = wp;
            }
        }

        if (nearest == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c附近没有狼人"));
            return;
        }

        String msg = cfg.getMsg("item.discover");
        String formatted = ChatColor.translateAlternateColorCodes('&', MsgFormat.msg(msg, player, nearest, nearest.getLocation()));
        PacketUtil.sendActionBar(player, formatted);

        ConfigurationSection sec = cfg.getItemSection("discover");
        long cd = sec != null ? sec.getLong("cd", 10) * 1000 : 10000;
        gp.setCooldown("discover", System.currentTimeMillis() + cd);

        Player targetPlayer = nearest;
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getGameManager().removeTrackerTask(player.getUniqueId());
            if (plugin.getGameManager().getState() != GameManager.GameState.GAMING) return;
            if (!player.isOnline()) return;
            String timeoutMsg = cfg.getMsg("item.discover-timeout");
            if (!timeoutMsg.isEmpty()) {
                PacketUtil.sendActionBar(player,
                        ChatColor.translateAlternateColorCodes('&', MsgFormat.msg(timeoutMsg, player)));
            }
        }, 40L).getTaskId();
        plugin.getGameManager().putTrackerTask(player.getUniqueId(), taskId);
    }

    private void handleTracker(Player player) {
        GamePlayer gp = cfg.getGamePlayer(player.getUniqueId());
        if (gp == null) return;

        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        for (GamePlayer ngp : cfg.getAllGamePlayers().values()) {
            if (!ngp.isAlive() || ngp.getKitId().equals("werewolf")) continue;
            Player np = Bukkit.getPlayer(ngp.getUuid());
            if (np == null || np.equals(player)) continue;
            double dist = player.getLocation().distance(np.getLocation());
            if (dist < minDist) {
                minDist = dist;
                nearest = np;
            }
        }

        if (nearest == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c附近没有平民"));
            return;
        }

        plugin.getGameManager().removeTrackerTask(player.getUniqueId());

        Player targetPlayer = nearest;

        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getGameManager().getState() != GameManager.GameState.GAMING) {
                    cancel(); plugin.getGameManager().removeTrackerTask(player.getUniqueId()); return;
                }
                if (!player.isOnline() || !targetPlayer.isOnline()) {
                    cancel(); plugin.getGameManager().removeTrackerTask(player.getUniqueId()); return;
                }
                GamePlayer tg = cfg.getGamePlayer(targetPlayer.getUniqueId());
                if (tg == null || !tg.isAlive()) {
                    cancel(); plugin.getGameManager().removeTrackerTask(player.getUniqueId());
                    consumeItem(player, findTrackerItem(player));
                    return;
                }
                String msg = cfg.getMsg("item.tracker");
                String formatted = ChatColor.translateAlternateColorCodes('&',
                        MsgFormat.msg(msg, player, targetPlayer, targetPlayer.getLocation()));
                PacketUtil.sendActionBar(player, formatted);
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
        plugin.getGameManager().putTrackerTask(player.getUniqueId(), taskId);
    }

    private ItemStack findTrackerItem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS
                    && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    && item.getItemMeta().getDisplayName().contains("玩家探测器")) {
                return item;
            }
        }
        return null;
    }

    private void handleSeerItem(Player player) {
        GamePlayer gp = cfg.getGamePlayer(player.getUniqueId());
        if (gp == null || !gp.getKitId().equals("seer")) return;
        plugin.getGameManager().openSeerMenu(player);
    }

    private void consumeItem(Player player, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().remove(item);
        }
    }

    private ConfigurationSection findItemByDisplayName(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return null;
        String stripped = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        ConfigurationSection items = cfg.getItemConfig();
        if (items == null) return null;
        for (String key : items.getKeys(false)) {
            ConfigurationSection sec = items.getConfigurationSection(key);
            if (sec == null || !sec.contains("menu")) continue;
            String cfgName = sec.getString("name", "");
            String cfgStripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', cfgName));
            if (cfgStripped.equals(stripped)) return sec;
        }
        return null;
    }
}