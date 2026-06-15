package com.aermini.whosrotten.listener;

import com.aermini.whosrotten.MsgFormat;
import com.aermini.whosrotten.WhosRotten;
import com.aermini.whosrotten.game.GameManager;
import com.aermini.whosrotten.game.GamePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {
    private final WhosRotten plugin;

    public PlayerConnectionListener(WhosRotten plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        player.setLevel(0);
        player.setExp(0f);
        plugin.getGameManager().handleJoin(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        plugin.getGameManager().handleQuit(player);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GamePlayer gp = plugin.getConfigManager().getGamePlayer(player.getUniqueId());
        if (gp == null) return;

        event.setCancelled(true);

        GameManager.GameState state = plugin.getGameManager().getState();
        String format;
        if (state == GameManager.GameState.GAMING) {
            format = plugin.getConfigManager().getMsg("game.chat");
        } else {
            format = plugin.getConfigManager().getMsg("chat");
        }
        if (format.isEmpty()) format = "&f<%player%&r&f> &f<message>";

        String msg = MsgFormat.msg(format, player, player)
                .replace("%player%", player.getName());
        msg = ChatColor.translateAlternateColorCodes('&', msg)
                .replace("<message>", event.getMessage());

        for (GamePlayer gp2 : plugin.getConfigManager().getAllGamePlayers().values()) {
            Player p = org.bukkit.Bukkit.getPlayer(gp2.getUuid());
            if (p != null) p.sendMessage(msg);
        }
    }
}
