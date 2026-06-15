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
            "§c§l▸ 狼人 §r- 隐藏自己的身份，\n" +
                    "§r获得其它人的信任，暗中杀光\n" +
                    "§r所有人；狼人的剑右键可以射\n" +
                    "§r出火焰弹。"
                );
                break;
            case "book_h":
                meta.setAuthor("烟雨庭");
                meta.addPage(
            "§a§l▸ 猎人 §r- 通过自己的判断，\n" +
                    "§r找出狼人并击杀他们!"
                );
                break;
            case "book_s":
                meta.setAuthor("烟雨庭");
                meta.addPage(
            "§b§l▸ 预言家 §r- 每隔一段时间可\n" +
                    "§r以查验一名玩家的身份,将此信息告\n" +
                    "§r知友方，并取得友方信任。"
                );
                break;
            case "book_n":
                meta.setAuthor("烟雨庭");
                meta.addPage(
            "§r§l▸ 平民 §r- 躲避狼人的追杀；\n" +
                    "§r用收集的宝石兑换一只弓\n" +
                    "§r和一把箭，寻找机会反杀狼人。"
                );
                break;
            case "book":
                meta.setAuthor("烟雨庭");
                meta.addPage(
                    "§r      §l[♕] 基本玩法 [♕]\n" +
                    "§r- 你会随机成为一种角色。\n" +
                    "§r- \n" +
                    "任何误杀都会造成双方同时\n" +
                    "死亡。\n" +
                    "§r\n" +
                    "§r      §l[♞] 角色说明 [♞]\n" +
                    "§c§l▸ 狼人 §r- 隐藏自己的身份，\n" +
                    "§r获得其它人的信任，暗中杀光\n" +
                    "§r所有人；狼人的剑右键可以射\n" +
                    "§r出火焰弹。\n" +
                    "§r\n" +
                    "§a§l▸ 猎人 §r- 通过自己的判断，\n" +
                    "§r找出狼人并击杀他们!"
                );
                meta.addPage(
                    "§b§l▸ 预言家 §r- 每隔一段时间可\n" +
                    "§r以查验一名玩家的身份,将此信息告\n" +
                    "§r知友方，并取得友方信任。\n" +
                    "§r\n" +
                    "§r§l▸ 平民 §r- 躲避狼人的追杀；\n" +
                    "§r用收集的宝石兑换一只弓\n" +
                    "§r和一把箭，寻找机会反杀狼人。"
                );
                meta.addPage("§r       §l● 更新日志\n" +
                    "§r\n" +
                    "§r§l版本: &r2.0.0\n" +
                    "§r§l日期: &r9月13日\n" +
                    "§r\n" +
                    "§r §l1-新版本\n" +
                    "§r ■ 全新\"预言家\"职业\n" +
                    "§r ■ 全新灵魂商店\n" +
                    "§r ■ 全面游戏优化"
                );
                meta.addPage("§r       §l● 关于我们\n" +
                    "§r\n" +
                    "§r《烟雨庭》是由一群热爱我\n" +
                    "§r的世界的小伙伴们创建的国\n" +
                    "§r风小游戏服务器；上线伊始，\n" +
                    "§r我们与成讯云达成合作，相信\n" +
                    "§r在大家的支持下，我们会越来\n" +
                    "§r越好！《烟雨庭》只为你的\n" +
                    "§r快乐而生。\n" +
                    "§r\n" +
                    "§r §l➜ 官网: www.mcyyt.cn\n" +
                    "§r §l➜ QQ群: 892702519"
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
