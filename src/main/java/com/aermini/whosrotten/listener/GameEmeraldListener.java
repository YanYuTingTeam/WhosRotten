package com.aermini.whosrotten.listener;

import com.aermini.whosrotten.WhosRotten;
import com.aermini.whosrotten.game.GameManager;
import com.aermini.whosrotten.game.GamePlayer;
import com.aermini.whosrotten.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class GameEmeraldListener implements Listener {
    private final WhosRotten plugin;
    private final ConfigManager cfg;

    public GameEmeraldListener(WhosRotten plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (plugin.getGameManager().getState() != GameManager.GameState.GAMING) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();

        GamePlayer gp = cfg.getGamePlayer(player.getUniqueId());
        if (gp == null) return;

        if (item.getType() == Material.EMERALD) {
            event.setCancelled(true);
            event.getItem().remove();
            ItemStack emeraldItem = plugin.getGameManager().buildItem("emerald", player);
            if (emeraldItem != null) {
                player.getInventory().addItem(emeraldItem);
            }
            gp.addEmerald(1);
            return;
        }

        if (item.getType() == Material.BOW && item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().contains("猎人的弓")) {
            if (gp.getKitId().equals("werewolf") || gp.getKitId().equals("hunter")) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            event.getItem().remove();
            gp.setKitId("hunter");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&l你接替了猎人的使命! 你现在是猎人!"));

            ItemStack bow = plugin.getGameManager().buildItem("bow", player);
            if (bow != null) player.getInventory().addItem(bow);
            ItemStack arrow = plugin.getGameManager().buildItem("arrow", player);
            if (arrow != null) player.getInventory().addItem(arrow);
            return;
        }

        if (item.getType() == Material.ARROW) {
            return;
        }
    }
}