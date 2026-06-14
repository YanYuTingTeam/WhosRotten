package com.aermini.whosrotten.listener;

import com.aermini.whosrotten.WhosRotten;
import com.aermini.whosrotten.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class GameItemProtectListener implements Listener {
    private final WhosRotten plugin;
    private final ConfigManager cfg;

    public GameItemProtectListener(WhosRotten plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        String title = event.getView().getTitle();
        if (title != null && (title.contains("[商店]") || title.contains("查验") || title.contains("游戏结束"))) return;

        if (current != null && current.getType() != Material.AIR && isProtected(current, (Player) event.getWhoClicked())) {
            event.setCancelled(true);
            return;
        }
        if (cursor != null && cursor.getType() != Material.AIR && isProtected(cursor, (Player) event.getWhoClicked())) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
            ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if (hotbarItem != null && hotbarItem.getType() != Material.AIR && isProtected(hotbarItem, (Player) event.getWhoClicked())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        for (ItemStack item : event.getNewItems().values()) {
            if (isProtected(item, (Player) event.getWhoClicked())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    private boolean isProtected(ItemStack item, Player player) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.getType() == Material.LEATHER_BOOTS
                || item.getType() == Material.LEATHER_LEGGINGS
                || item.getType() == Material.LEATHER_CHESTPLATE
                || item.getType() == Material.LEATHER_HELMET) {
            return true;
        }
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        ConfigurationSection items = cfg.getItemConfig();
        for (String key : items.getKeys(false)) {
            ConfigurationSection sec = items.getConfigurationSection(key);
            if (sec == null || !sec.contains("can-move")) continue;
            if (sec.getBoolean("can-move", true)) continue;

            String cfgMatName = sec.getString("material", "");
            if (cfgMatName != null && !cfgMatName.isEmpty() && item.getType() == Material.matchMaterial(cfgMatName)) {
                return true;
            }

            String cfgName = sec.getString("name", "");
            if (cfgName != null && !cfgName.isEmpty()) {
                String strippedCfg = ChatColor.stripColor(cfgName);
                if (strippedCfg.equals(name)) return true;
            }
        }
        return false;
    }
}
