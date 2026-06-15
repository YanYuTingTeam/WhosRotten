package com.aermini.whosrotten.game;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class GameBook {

    public static ItemStack createBook(String kitId) {
        return createBook(kitId, null);
    }

    public static ItemStack createBook(String kitId, String name) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        switch (kitId) {
            case "book_w":
                meta.setAuthor("烟雨庭");
                meta.addPage(
            "§c§l▸ 狼人 §r§7- 隐藏自己的身份，\n" +
                    "§r§7获得其它人的信任，暗中杀光\n" +
                    "§r§7所有人；狼人的剑右键可以射\n" +
                    "§r§7出火焰弹。"
                );
                break;
            case "book_h":
                meta.setAuthor("烟雨庭");
                meta.addPage(
            "§a§l▸ 猎人 §r§7- 通过自己的判断，\n" +
                    "§r§7找出狼人并击杀他们!"
                );
                break;
            case "book_s":
                meta.setAuthor("烟雨庭");
                meta.addPage(
            "§b§l▸ 预言家 §r§7- 每隔一段时间可\n" +
                    "§r§7以查验一名玩家的身份,将此信息告\n" +
                    "§r§7知友方，并取得友方信任。"
                );
                break;
            case "book_n":
                meta.setAuthor("烟雨庭");
                meta.addPage(
            "§r§7§l▸ 平民 §r§7- 躲避狼人的追杀；\n" +
                    "§r§7用收集的宝石兑换一只弓\n" +
                    "§r§7和一把箭，寻找机会反杀狼人。"
                );
                break;
            case "book":
                meta.setAuthor("烟雨庭");
                meta.addPage(
                    "§r§7      §l[♕] 基本玩法 [♕]\n" +
                    "§r§7- 你会随机成为一种角色。\n" +
                    "§r§7- \n" +
                    "任何误杀都会造成双方同时\n" +
                    "死亡。\n" +
                    "§r§7\n" +
                    "§r§7      §l[♞] 角色说明 [♞]\n" +
                    "§c§l▸ 狼人 §r§7- 隐藏自己的身份，\n" +
                    "§r§7获得其它人的信任，暗中杀光\n" +
                    "§r§7所有人；狼人的剑右键可以射\n" +
                    "§r§7出火焰弹。\n" +
                    "§r§7\n" +
                    "§a§l▸ 猎人 §r§7- 通过自己的判断，\n" +
                    "§r§7找出狼人并击杀他们!"
                );
                meta.addPage(
                    "§b§l▸ 预言家 §r§7- 每隔一段时间可\n" +
                    "§r§7以查验一名玩家的身份,将此信息告\n" +
                    "§r§7知友方，并取得友方信任。\n" +
                    "§r§7\n" +
                    "§r§7§l▸ 平民 §r§7- 躲避狼人的追杀；\n" +
                    "§r§7用收集的宝石兑换一只弓\n" +
                    "§r§7和一把箭，寻找机会反杀狼人。"
                );
                meta.addPage("§r§7       §l● 更新日志\n" +
                    "§r§7\n" +
                    "§r§7§l版本: §r§72.0.0\n" +
                    "§r§7§l日期: §r§79月13日\n" +
                    "§r§7\n" +
                    "§r§7 §l1-新版本\n" +
                    "§r§7 ■ 全新\"预言家\"职业\n" +
                    "§r§7 ■ 全新灵魂商店\n" +
                    "§r§7 ■ 全面游戏优化"
                );
                meta.addPage("§r§7       §l● 关于我们\n" +
                    "§r§7\n" +
                    "§r§7《烟雨庭》是由一群热爱我\n" +
                    "§r§7的世界的小伙伴们创建的国\n" +
                    "§r§7风小游戏服务器；上线伊始，\n" +
                    "§r§7我们与成讯云达成合作，相信\n" +
                    "§r§7在大家的支持下，我们会越来\n" +
                    "§r§7越好！《烟雨庭》只为你的\n" +
                    "§r§7快乐而生。\n" +
                    "§r§7\n" +
                    "§r§7 §l➜ 官网: www.mcyyt.cn\n" +
                    "§r§7 §l➜ QQ群: 892702519"
                );
                break;
            default:
                return null;
        }

        if (name != null && !name.isEmpty()) {
            meta.setTitle(name);
            meta.setDisplayName(name);
        }
        book.setItemMeta(meta);
        return book;
    }
}
