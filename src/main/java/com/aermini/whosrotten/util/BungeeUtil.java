package com.aermini.whosrotten.util;

import com.aermini.whosrotten.WhosRotten;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;

public class BungeeUtil {
    public static void sendToServer(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);

        player.sendPluginMessage(WhosRotten.getInstance(), WhosRotten.BUNGEE_CHANNEL, out.toByteArray());

        if (WhosRotten.getInstance().getConfig().getBoolean("debug", false)) {
            WhosRotten.getInstance().getPluginLogger().info(
                    "发送玩家 " + player.getName() + " 到服务器: " + serverName
            );
        }
    }

    public static void sendEvent(Player player, String eventName, String... params) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("WhosRotten");

        StringBuilder message = new StringBuilder("event=" + eventName);
        for (String param : params) {
            message.append(",").append(param);
        }

        out.writeUTF(message.toString());

        player.sendPluginMessage(WhosRotten.getInstance(), WhosRotten.BUNGEE_CHANNEL, out.toByteArray());

        if (WhosRotten.getInstance().getConfig().getBoolean("debug", false)) {
            WhosRotten.getInstance().getPluginLogger().info(
                    "发送自定义事件: " + message.toString()
            );
        }
    }

    /**
     * 发送再来一局请求
     * 格式: event=AerJoinerR,player=玩家名,group=服务器组名
     *
     * @param player    玩家
     * @param group     服务器组名
     */
    public static void sendPlayAgain(Player player, String group) {
        sendEvent(player, "AerJoinerR", "player=" + player.getName(), "group=" + group);
    }

    /**
     * 发送玩家到大厅
     *
     * @param player    玩家
     * @param hubServer 大厅服务器名称
     */
    public static void sendToHub(Player player, String hubServer) {
        sendToServer(player, hubServer);
    }
}
