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
import org.bukkit.entity.LargeFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameCombatListener implements Listener {
    private final WhosRotten plugin;
    private final ConfigManager cfg;
    private final Map<UUID, org.bukkit.Location> deathLocations = new HashMap<>();
    private final Map<UUID, Integer> wolfParticleTasks = new HashMap<>();

    private static final java.lang.reflect.Constructor<?> nmsParticlePacketConstructor;
    private static final java.lang.reflect.Method nmsSendMethod;
    private static final Object particleRedstone;
    static {
        java.lang.reflect.Constructor<?> pCon = null;
        java.lang.reflect.Method sMet = null;
        Object pRed = null;
        try {
            String ver = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> packetClass = Class.forName("net.minecraft.server." + ver + ".PacketPlayOutWorldParticles");
            Class<?> enumClass = Class.forName("net.minecraft.server." + ver + ".EnumParticle");
            pRed = enumClass.getMethod("valueOf", String.class).invoke(null, "REDSTONE");
            for (java.lang.reflect.Constructor<?> c : packetClass.getDeclaredConstructors()) {
                Class<?>[] pts = c.getParameterTypes();
                if (pts.length >= 9 && pts[0].isEnum()) {
                    pCon = c;
                    pCon.setAccessible(true);
                    break;
                }
            }
            Class<?> connClass = Class.forName("net.minecraft.server." + ver + ".PlayerConnection");
            for (java.lang.reflect.Method m : connClass.getDeclaredMethods()) {
                if (m.getName().equals("sendPacket") && m.getParameterTypes().length == 1) { sMet = m; sMet.setAccessible(true); break; }
            }
        } catch (Throwable ignored) {
        }
        nmsParticlePacketConstructor = pCon;
        nmsSendMethod = sMet;
        particleRedstone = pRed;
    }

    public GameCombatListener(WhosRotten plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNaturalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.isCancelled()) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.getGameManager().getState() != GameManager.GameState.GAMING) {
            event.setCancelled(true);
            return;
        }

        Player damaged = (Player) event.getEntity();
        Player attacker = null;

        if (event.getDamager() instanceof LargeFireball) {
            LargeFireball fireball = (LargeFireball) event.getDamager();
            if (fireball.getShooter() instanceof Player) {
                attacker = (Player) fireball.getShooter();
            }
            event.setCancelled(true);
            if (attacker != null && !attacker.equals(damaged)) {
                handleKill(damaged, attacker);
            }
            return;
        }

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
            event.setCancelled(true);
            if (attacker.equals(damaged)) return;

            GamePlayer gpAttacker = cfg.getGamePlayer(attacker.getUniqueId());
            if (gpAttacker != null && gpAttacker.getKitId().equals("werewolf")) {
                ItemStack inHand = attacker.getItemInHand();
                if (inHand != null && inHand.getType() == Material.DIAMOND_SWORD
                        && inHand.hasItemMeta() && inHand.getItemMeta().hasDisplayName()
                        && inHand.getItemMeta().getDisplayName().contains("狼人的爪子")) {
                    handleKill(damaged, attacker);
                }
            }
            return;
        }

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

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (plugin.getGameManager().getState() != GameManager.GameState.GAMING) return;
        Player player = (Player) event.getEntity();
        GamePlayer gp = cfg.getGamePlayer(player.getUniqueId());
        if (gp == null) return;

        ItemStack bow = event.getBow();
        if (bow == null || !bow.hasItemMeta() || !bow.getItemMeta().hasDisplayName()) return;
        String bowName = bow.getItemMeta().getDisplayName();

        String bowItemId = null;
        for (String key : cfg.getItemConfig().getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection sec = cfg.getItemConfig().getConfigurationSection(key);
            if (sec == null) continue;
            if (!"BOW".equalsIgnoreCase(sec.getString("material", ""))) continue;
            String cfgName = sec.getString("name", "");
            if (cfgName.isEmpty()) continue;
            String colorizedName = org.bukkit.ChatColor.translateAlternateColorCodes('&', cfgName);
            if (bowName.equals(colorizedName)) { bowItemId = key; break; }
        }
        if (bowItemId == null) return;

        org.bukkit.configuration.ConfigurationSection bowSec = cfg.getItemSection(bowItemId);
        if (bowSec == null || !bowSec.contains("cd")) return;
        long cd = bowSec.getLong("cd", 0) * 1000;

        if (!gp.isCooldownReady("bow")) {
            event.setCancelled(true);
            int remain = gp.getCooldownRemain("bow");
            String msg = cfg.getMsg("cooldown").replace("%cooldown%", String.valueOf(remain));
            if (!msg.isEmpty()) player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', MsgFormat.msg(msg, player)));
            return;
        }
        gp.setCooldown("bow", System.currentTimeMillis() + cd);
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
        deathLocations.put(dead.getUniqueId(), dead.getLocation().clone());

        if (gp.getKitId().equals("hunter") || hasHunterBow(dead)) {
            ItemStack bow = plugin.getGameManager().buildItem("bow", dead);
            if (bow != null) {
                org.bukkit.entity.Item bowDrop = dead.getWorld().dropItemNaturally(dead.getLocation(), bow);
                plugin.getGameManager().onHunterBowDrop(bowDrop);
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

        plugin.getGameManager().broadcastTitle("death", dead, dead.getLocation());

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
            org.bukkit.Location deathLoc = deathLocations.remove(player.getUniqueId());
            if (deathLoc != null) {
                event.setRespawnLocation(deathLoc);
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.teleport(deathLoc), 1L);
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                org.bukkit.configuration.ConfigurationSection loseSec = cfg.getConfig().getConfigurationSection("lose");
                if (loseSec != null) {
                    org.bukkit.configuration.ConfigurationSection againSec = loseSec.getConfigurationSection("again");
                    if (againSec != null) {
                        org.bukkit.Material mat = org.bukkit.Material.matchMaterial(againSec.getString("material", "SLIME_BALL"));
                        ItemStack againItem = new ItemStack(mat != null ? mat : org.bukkit.Material.SLIME_BALL);
                        org.bukkit.inventory.meta.ItemMeta meta = againItem.getItemMeta();
                        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', againSec.getString("name", "再来一局")));
                        againItem.setItemMeta(meta);
                        player.getInventory().setItem(6, againItem);
                    }
                    org.bukkit.configuration.ConfigurationSection leaveSec = loseSec.getConfigurationSection("leave");
                    if (leaveSec != null) {
                        org.bukkit.Material mat = org.bukkit.Material.matchMaterial(leaveSec.getString("material", "BED"));
                        ItemStack leaveItem = new ItemStack(mat != null ? mat : org.bukkit.Material.BED);
                        org.bukkit.inventory.meta.ItemMeta meta = leaveItem.getItemMeta();
                        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', leaveSec.getString("name", "离开游戏")));
                        leaveItem.setItemMeta(meta);
                        player.getInventory().setItem(7, leaveItem);
                    }
                }
                plugin.getGameManager().openLoseMenu(player);
            }, 60L);
        }
    }

    private boolean hasHunterBow(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == Material.BOW
                    && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    && item.getItemMeta().getDisplayName().contains("猎人的弓 ")) {
                return true;
            }
        }
        return false;
    }

    private void handleKill(Player damaged, Player attacker) {
        if (plugin.getGameManager().getState() != GameManager.GameState.GAMING) return;

        GamePlayer gpDamaged = cfg.getGamePlayer(damaged.getUniqueId());
        GamePlayer gpAttacker = cfg.getGamePlayer(attacker.getUniqueId());
        if (gpDamaged == null || gpAttacker == null) return;
        if (!gpDamaged.isAlive()) return;

        if (gpAttacker.getKitId().equals("werewolf")) {
            damaged.getWorld().playSound(damaged.getLocation(), org.bukkit.Sound.HURT_FLESH, 1.0f, 1.0f);
            startWolfParticleCircle(attacker);
        }

        damaged.setHealth(0);
    }

    public void cancelAllWolfParticleTasks() {
        for (int taskId : wolfParticleTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        wolfParticleTasks.clear();
    }

    private void startWolfParticleCircle(Player wolf) {
        UUID uid = wolf.getUniqueId();
        Integer oldTask = wolfParticleTasks.remove(uid);
        if (oldTask != null) Bukkit.getScheduler().cancelTask(oldTask);

        if (nmsParticlePacketConstructor == null || nmsSendMethod == null || particleRedstone == null) {
            return;
        }

        int taskId = new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (!wolf.isOnline() || count >= 20) {
                    wolfParticleTasks.remove(uid);
                    cancel();
                    return;
                }
                org.bukkit.Location eyeLoc = wolf.getEyeLocation();
                Vector dir = eyeLoc.getDirection().normalize().multiply(0.5);
                org.bukkit.Location handLoc = eyeLoc.add(dir).add(0, -0.3, 0);
                double radius = 0.5;
                int points = 8;
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = handLoc.getX() + radius * Math.cos(angle);
                    double y = handLoc.getY();
                    double z = handLoc.getZ() + radius * Math.sin(angle);
                    spawnRedstoneDust(handLoc.getWorld(), x, y, z);
                }
                count++;
            }
        }.runTaskTimer(plugin, 0L, 5L).getTaskId();
        wolfParticleTasks.put(uid, taskId);
    }

    @SuppressWarnings("deprecation")
    private void spawnRedstoneDust(org.bukkit.World world, double x, double y, double z) {
        if (nmsParticlePacketConstructor == null || nmsSendMethod == null || particleRedstone == null) return;
        try {
            int len = nmsParticlePacketConstructor.getParameterTypes().length;
            Object packet;
            if (len == 11) {
                packet = nmsParticlePacketConstructor.newInstance(
                        particleRedstone, true,
                        (float) x, (float) y, (float) z,
                        0f, 0f, 0f, 0f, 1, new int[0]
                );
            } else if (len == 10) {
                packet = nmsParticlePacketConstructor.newInstance(
                        particleRedstone, true,
                        (float) x, (float) y, (float) z,
                        0f, 0f, 0f, 0f, 1
                );
            } else {
                packet = nmsParticlePacketConstructor.newInstance(
                        particleRedstone,
                        (float) x, (float) y, (float) z,
                        0f, 0f, 0f, 0f, 1
                );
            }
            Object nmsWorld = world.getClass().getMethod("getHandle").invoke(world);
            Object playerList = nmsWorld.getClass().getField("players").get(nmsWorld);
            for (Object ep : (java.util.List<?>) playerList) {
                Object conn = ep.getClass().getField("playerConnection").get(ep);
                nmsSendMethod.invoke(conn, packet);
            }
        } catch (Exception ignored) {
        }
    }
}