package com.aermini.whosrotten.util;

import org.bukkit.entity.Player;

public class PacketUtil {

    public static void sendTitleWithTiming(Player player, String title, String subtitle, int fadeIn, int stay, int out) {
        try {
            String nmsVersion = getNmsVersion(player);
            if (nmsVersion == null) return;
            Class<?> packetClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutTitle");
            Class<?> enumClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutTitle$EnumTitleAction");
            Class<?> chatSerializer = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent$ChatSerializer");
            Class<?> chatComponent = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent");

            Object timesPacket = packetClass.getConstructor(enumClass, chatComponent, int.class, int.class, int.class)
                    .newInstance(enumClass.getEnumConstants()[2], null, fadeIn, stay, out);

            Object titleComp = title != null ? chatSerializer.getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + title.replace("\"", "\\\"") + "\"}") : null;
            Object subtitleComp = subtitle != null ? chatSerializer.getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + subtitle.replace("\"", "\\\"") + "\"}") : null;

            Object titlePacket = packetClass.getConstructor(enumClass, chatComponent, int.class, int.class, int.class)
                    .newInstance(enumClass.getEnumConstants()[0], titleComp, fadeIn, stay, out);
            Object subtitlePacket = packetClass.getConstructor(enumClass, chatComponent, int.class, int.class, int.class)
                    .newInstance(enumClass.getEnumConstants()[1], subtitleComp, fadeIn, stay, out);

            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
            Class<?> basePacket = Class.forName("net.minecraft.server." + nmsVersion + ".Packet");
            java.lang.reflect.Method sendPacket = connection.getClass().getMethod("sendPacket", basePacket);

            sendPacket.invoke(connection, timesPacket);
            if (titleComp != null) sendPacket.invoke(connection, titlePacket);
            if (subtitleComp != null) sendPacket.invoke(connection, subtitlePacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendActionBar(Player player, String text) {
        try {
            String nmsVersion = getNmsVersion(player);
            if (nmsVersion == null) return;
            Class<?> chatSerializer = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent$ChatSerializer");
            Class<?> chatComponent = Class.forName("net.minecraft.server." + nmsVersion + ".IChatBaseComponent");
            Class<?> packetClass = Class.forName("net.minecraft.server." + nmsVersion + ".PacketPlayOutChat");

            Object component = chatSerializer.getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
            Object packet = packetClass.getConstructor(chatComponent, byte.class)
                    .newInstance(component, (byte) 2);

            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);
            Class<?> basePacket = Class.forName("net.minecraft.server." + nmsVersion + ".Packet");
            java.lang.reflect.Method sendPacket = connection.getClass().getMethod("sendPacket", basePacket);
            sendPacket.invoke(connection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getNmsVersion(Player player) {
        String pkgName = player.getClass().getPackage().getName();
        for (String part : pkgName.split("\\.")) {
            if (part.matches("v\\d+_\\d+_R\\d+")) return part;
        }
        return null;
    }
}
