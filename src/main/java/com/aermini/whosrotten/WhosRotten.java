package com.aermini.whosrotten;

import com.aermini.whosrotten.game.GameManager;
import com.aermini.whosrotten.listener.*;
import com.aermini.whosrotten.manager.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class WhosRotten extends JavaPlugin {
    private static WhosRotten instance;
    private GameCombatListener combatListener;
    private Logger logger;
    private ConfigManager configManager;
    private GameManager gameManager;
    public static final String BUNGEE_CHANNEL = "BungeeCord";

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        getServer().getMessenger().registerOutgoingPluginChannel(this, BUNGEE_CHANNEL);

        configManager = new ConfigManager(this);
        gameManager = new GameManager(this);

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        combatListener = new GameCombatListener(this);
        getServer().getPluginManager().registerEvents(combatListener, this);
        getServer().getPluginManager().registerEvents(new GameItemListener(this), this);
        getServer().getPluginManager().registerEvents(new GameShopListener(this), this);
        getServer().getPluginManager().registerEvents(new GameEmeraldListener(this), this);
        getServer().getPluginManager().registerEvents(new GameItemProtectListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerPingListener(this), this);

        getCommand("whosrotten").setExecutor(new com.aermini.whosrotten.wrCommand(this));

        String defaultMap = "default";
        if (!configManager.getAllMaps().isEmpty()) {
            defaultMap = configManager.getAllMaps().iterator().next().getGameName();
        }
        gameManager.startWaiting(defaultMap);

        logger.info("WhosRotten 插件已启用!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.resetGame();
        logger.info("WhosRotten 插件已禁用!");
    }

    public static WhosRotten getInstance() { return instance; }
    public Logger getPluginLogger() { return logger; }
    public ConfigManager getConfigManager() { return configManager; }
    public GameManager getGameManager() { return gameManager; }
    public GameCombatListener getCombatListener() { return combatListener; }

    public void sendBungeeMessage(org.bukkit.entity.Player player, String subchannel, String... data) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subchannel);
        for (String item : data) out.writeUTF(item);
        player.sendPluginMessage(this, BUNGEE_CHANNEL, out.toByteArray());
    }
}
