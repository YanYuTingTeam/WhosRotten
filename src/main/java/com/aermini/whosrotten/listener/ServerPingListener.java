package com.aermini.whosrotten.listener;

import com.aermini.whosrotten.WhosRotten;
import com.aermini.whosrotten.game.GameManager;
import com.aermini.whosrotten.manager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public class ServerPingListener implements Listener {
    private final WhosRotten plugin;

    public ServerPingListener(WhosRotten plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerPing(ServerListPingEvent event) {
        ConfigManager cfg = plugin.getConfigManager();
        GameManager gm = plugin.getGameManager();
        GameManager.GameState state = gm.getState();

        event.setMaxPlayers(cfg.getMaxPlayers());

        String motd;
        switch (state) {
            case STARTING:
                if (cfg.getOnlinePlayers() >= cfg.getMaxPlayers()) {
                    motd = cfg.getConfig().getString("motd.full", "");
                } else {
                    motd = cfg.getConfig().getString("motd.waiting", "");
                }
                break;
            case GAMING:
                motd = cfg.getConfig().getString("motd.gaming", "");
                break;
            case ENDING:
                motd = cfg.getConfig().getString("motd.ending", "");
                break;
            default:
                motd = cfg.getConfig().getString("motd.waiting", "");
                break;
        }

        event.setMotd(ChatColor.translateAlternateColorCodes('&', motd));
    }
}
