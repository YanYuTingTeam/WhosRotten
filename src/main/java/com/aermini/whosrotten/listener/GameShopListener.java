package com.aermini.whosrotten.listener;

import com.aermini.whosrotten.MsgFormat;
import com.aermini.whosrotten.WhosRotten;
import com.aermini.whosrotten.game.GameManager;
import com.aermini.whosrotten.game.GamePlayer;
import com.aermini.whosrotten.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GameShopListener implements Listener {
    private final WhosRotten plugin;
    private final ConfigManager cfg;

    public GameShopListener(WhosRotten plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        if (title == null) return;

        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && currentItem.getType() == Material.STAINED_GLASS_PANE
                && currentItem.hasItemMeta() && currentItem.getItemMeta().hasDisplayName()) {
            String stripped = ChatColor.stripColor(currentItem.getItemMeta().getDisplayName());
            if ("关闭".equals(stripped)) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }
        }

        if (title.contains("[商店]")) {
            event.setCancelled(true);
            handleShopClick(player, event.getRawSlot(), event.getCurrentItem());
            return;
        }

        if (title.contains("查验身份") || title.contains("选择")) {
            event.setCancelled(true);
            handleSeerClick(player, event.getRawSlot(), event.getCurrentItem());
            return;
        }

        if (title.contains("游戏结束")) {
            event.setCancelled(true);
            handleLoseMenuClick(player, event.getRawSlot());
            return;
        }
    }

    private void handleShopClick(Player player, int slot, ItemStack clicked) {
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.STAINED_GLASS_PANE) return;

        String shopKey = findShopKey(player);
        if (shopKey == null) return;

        ConfigurationSection shopSec = cfg.getMenuConfig().getConfigurationSection("shop." + shopKey);
        if (shopSec == null) return;

        List<String> shopItems = shopSec.getStringList("items");
        for (String entry : shopItems) {
            String[] parts = entry.split(",");
            if (parts.length < 3) continue;
            int itemSlot = Integer.parseInt(parts[0].trim());
            String itemId = parts[1].trim();
            int price = Integer.parseInt(parts[2].trim());

            if (itemSlot != slot) continue;

            int have = 0;
            for (ItemStack inv : player.getInventory().getContents()) {
                if (inv != null && inv.getType() == Material.EMERALD) have += inv.getAmount();
            }
            if (have < price) {
                String noMoney = cfg.getMsg("game.nomoney");
                if (!noMoney.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', MsgFormat.msg(noMoney, player)));
                }
                return;
            }

            int left = price;
            for (int i = 0; i < player.getInventory().getSize() && left > 0; i++) {
                ItemStack inv = player.getInventory().getItem(i);
                if (inv != null && inv.getType() == Material.EMERALD) {
                    if (inv.getAmount() <= left) {
                        left -= inv.getAmount();
                        player.getInventory().clear(i);
                    } else {
                        inv.setAmount(inv.getAmount() - left);
                        left = 0;
                    }
                }
            }

            ItemStack item = plugin.getGameManager().buildItem(itemId, player);
            if (item != null) {
                player.getInventory().addItem(item);
            }

            if (itemId.equals("bow")) {
                ItemStack arrow = plugin.getGameManager().buildItem("arrow", player);
                if (arrow != null) player.getInventory().addItem(arrow);
            }

            int remain = 0;
            for (ItemStack inv : player.getInventory().getContents()) {
                if (inv != null && inv.getType() == Material.EMERALD) remain += inv.getAmount();
            }
            break;
        }
    }

    private void handleSeerClick(Player player, int slot, ItemStack clicked) {
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.STAINED_GLASS_PANE) return;

        GamePlayer seerGp = cfg.getGamePlayer(player.getUniqueId());
        if (seerGp == null || !seerGp.getKitId().equals("seer")) return;

        if (!seerGp.isCooldownReady("seer")) {
            int remain = seerGp.getCooldownRemain("seer");
            String msg = cfg.getMsg("cooldown").replace("%cooldown%", String.valueOf(remain));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', MsgFormat.msg(msg, player)));
            player.closeInventory();
            return;
        }

        if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            String targetName = null;
            if (name.contains("]")) {
                targetName = name.substring(name.indexOf("]") + 1);
            }
            if (targetName == null || targetName.isEmpty()) return;

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) return;

            GamePlayer targetGp = cfg.getGamePlayer(target.getUniqueId());
            if (targetGp == null) return;

            seerGp.setKnown(target.getUniqueId(), true);

            String seerMsg = cfg.getMsg("item.seer");
            if (!seerMsg.isEmpty()) {
                String formatted = MsgFormat.msg(seerMsg, player, target);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', formatted));
            }

            String broadcastMsg = cfg.getMsg("kit.seer");
            if (!broadcastMsg.isEmpty()) {
                for (GamePlayer gp : cfg.getAllGamePlayers().values()) {
                    Player p = Bukkit.getPlayer(gp.getUuid());
                    if (p != null) {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                MsgFormat.msg(broadcastMsg, p, target)));
                    }
                }

                plugin.getGameManager().sendAllTitle("seer", target);
            }

            ConfigurationSection seerSec = cfg.getItemSection("seer");
            long cd = seerSec != null ? seerSec.getLong("cd", 150) * 1000 : 150000;
            seerGp.setCooldown("seer", System.currentTimeMillis() + cd);

            player.closeInventory();
        }
    }

    private void handleLoseMenuClick(Player player, int slot) {
        ConfigurationSection loseSec = cfg.getConfig().getConfigurationSection("lose");
        if (loseSec == null) return;

        int specSlot = loseSec.getInt("spec.slot", 10);
        int againSlot = loseSec.getInt("again.slot", 13);
        int leaveSlot = loseSec.getInt("leave.slot", 16);

        if (slot == specSlot) {

            player.closeInventory();
        } else if (slot == againSlot) {
            player.closeInventory();
            plugin.getGameManager().handlePlayAgain(player);
        } else if (slot == leaveSlot) {
            player.closeInventory();
            plugin.getGameManager().handleLeaveGame(player);
        }
    }

    private String findShopKey(Player player) {

        GamePlayer gp = cfg.getGamePlayer(player.getUniqueId());
        if (gp == null) return "normal";
        if (gp.getKitId().equals("werewolf")) return "werewolf";
        return "normal";
    }
}