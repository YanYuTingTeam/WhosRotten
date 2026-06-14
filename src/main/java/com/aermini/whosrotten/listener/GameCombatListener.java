package com.aermini.whosrotten.listener;

import com.aermini.whosrotten.MsgFormat;
import com.aermini.whosrotten.WhosRotten;
import com.aermini.whosrotten.game.GameManager;
import com.aermini.whosrotten.game.GamePlayer;
import com.aermini.whosrotten.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GameCombatListener implements Listener {
    private final WhosRotten plugin;
    private final ConfigManager cfg;

    public GameCombatListener(WhosRotten plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    // block all non-PvP damage (natural causes, fall, fire, etc.)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onNaturalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.isCancelled()) return;
        event.setCancelled(true);
    }

    // PvP combat handler
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.getGameManager().getState() != GameManager.GameState.GAMING) {
            event.setCancelled(true);
            return;
        }

        Player damaged = (Player) event.getEntity();
        Player attacker = null;

        // fireball
        if (event.getDamager() instanceof SmallFireball) {
            SmallFireball fireball = (SmallFireball) event.getDamager();
            if (fireball.getShooter() instanceof Player) {
                attacker = (Player) fireball.getShooter();
            }
            event.setCancelled(true);
            if (attacker != null && !attacker.equals(damaged)) {
                handleKill(damaged, attacker);
            }
            return;
        }

        // direct player attack (melee)
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
            event.setCancelled(true);
            if (attacker.equals(damaged)) return;
            // only werewolf can melee kill
            GamePlayer gpAttacker = cfg.getGamePlayer(attacker.getUniqueId());
            if (gpAttacker != null && gpAttacker.getKitId().equals("werewolf")) {
                handleKill(damaged, attacker);
            }
            return;
        }

        // projectile (arrow etc.)
        if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
            }
        }

        if (attacker == null || attacker.equals(damaged)) return;

        event.setCancelled(true);
        handleKill(damaged, attacker);
    }

    // prevent hunger loss
    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        event.setKeepInventory(true);
        Player dead = event.getEntity();

        if (plugin.getGameManager().getState() != GameManager.GameState.GAMING) {
            event.getDrops().clear();
            Bukkit.getScheduler().runTaskLater(plugin, () -> dead.spigot().respawn(), 1L);
            return;
        }

        event.setDroppedExp(0);
        event.getDrops().clear();

        GamePlayer gp = cfg.getGamePlayer(dead.getUniqueId());
        if (gp == null) return;
        gp.setAlive(false);

        if (gp.getKitId().equals("hunter")) {
            ItemStack bow = plugin.getGameManager().buildItem("bow", dead);
            if (bow != null) {
                dead.getWorld().dropItemNaturally(dead.getLocation(), bow);
            }
            String bowDropMsg = cfg.getMsg("game.bowdrop");
            if (!bowDropMsg.isEmpty()) {
                String formatted = MsgFormat.msg(bowDropMsg, null, dead, dead.getLocation());
                for (GamePlayer p : cfg.getAllGamePlayers().values()) {
                    Player pl = Bukkit.getPlayer(p.getUuid());
                    if (pl != null) pl.sendMessage(ChatColor.translateAlternateColorCodes('&', formatted));
                }
            }
        }

        plugin.getGameManager().broadcastTitle("death", dead);

        String deathMsg = cfg.getMsg("game.death");
        if (!deathMsg.isEmpty()) {
            String formatted = MsgFormat.msg(deathMsg, null, dead, dead.getLocation());
            for (GamePlayer p : cfg.getAllGamePlayers().values()) {
                Player pl = Bukkit.getPlayer(p.getUuid());
                if (pl != null) pl.sendMessage(ChatColor.translateAlternateColorCodes('&', formatted));
            }
        }

        List<String> rewards = cfg.getConfig().getStringList("reward.died");
        for (String cmd : rewards) {
            if (cmd.startsWith("[CMD]")) {
                String c = cmd.substring(5).trim().replace("%player%", dead.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
            } else if (cmd.startsWith("[MSG]")) {
                String m = cmd.substring(5).trim();
                dead.sendMessage(ChatColor.translateAlternateColorCodes('&', MsgFormat.msg(m, dead)));
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> dead.spigot().respawn(), 3L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GamePlayer gp = cfg.getGamePlayer(player.getUniqueId());
        if (gp == null) return;
        if (!gp.isAlive()) {
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        }
    }

    private void handleKill(Player damaged, Player attacker) {
        if (plugin.getGameManager().getState() != GameManager.GameState.GAMING) return;

        GamePlayer gpDamaged = cfg.getGamePlayer(damaged.getUniqueId());
        GamePlayer gpAttacker = cfg.getGamePlayer(attacker.getUniqueId());
        if (gpDamaged == null || gpAttacker == null) return;
        if (!gpDamaged.isAlive()) return;

        damaged.setHealth(0); // trigger death event
    }
}
