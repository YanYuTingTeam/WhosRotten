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
                meta.setAuthor("花雨庭");
                meta.addPage(
                    "\n\n§4§l━━━━━━━━\n" +
                    "   §c§l狼人指南\n" +
                    "§4§l━━━━━━━━\n\n" +
                    "§7你是§c§l狼人§7!\n\n" +
                    "§7你的目标是§c§l杀光所有人类§7.\n\n" +
                    "§7你可以用§c§l爪子§7攻击人类.\n\n" +
                    "§7狼人之间可以互相看到名字.\n" +
                    "§7在商店可以购买特殊道具.\n\n" +
                    "§8记住, 用谎言欺诈他们!"
                );
                break;
            case "book_h":
                meta.setAuthor("花雨庭");
                meta.addPage(
                    "\n\n§2§l━━━━━━━━\n" +
                    "   §a§l猎人指南\n" +
                    "§2§l━━━━━━━━\n\n" +
                    "§7你是§a§l猎人§7!\n\n" +
                    "§7你的目标是§a§l猎杀所有狼人§7.\n\n" +
                    "§7你拥有§a§l弓§7和箭矢.\n\n" +
                    "§7死亡后你的弓会掉落.\n" +
                    "§7平民可以捡起弓接替你的使命.\n\n" +
                    "§8找出狼人, 用弓箭制裁他们!"
                );
                break;
            case "book_s":
                meta.setAuthor("花雨庭");
                meta.addPage(
                    "\n\n§1§l━━━━━━━━\n" +
                    "   §b§l预言家指南\n" +
                    "§1§l━━━━━━━━\n\n" +
                    "§7你是§b§l预言家§7!\n\n" +
                    "§7你的目标是§b§l查验所有人§7的身份.\n\n" +
                    "§7你可以用§b§l预言魔杖§7查验玩家.\n\n" +
                    "§7查验结果只有你自己能看到.\n" +
                    "§7其他预言家的结果不互通.\n\n" +
                    "§8找出说谎的人!"
                );
                break;
            case "book_n":
                meta.setAuthor("花雨庭");
                meta.addPage(
                    "\n\n§f§l━━━━━━━━\n" +
                    "   §f§l平民指南\n" +
                    "§f§l━━━━━━━━\n\n" +
                    "§7你是§f§l平民§7!\n\n" +
                    "§7你的目标是§f§l找出并击杀狼人§7.\n\n" +
                    "§7收集§e§l宝石§7在商店购买道具.\n\n" +
                    "§7当猎人死亡后你可以捡起他的弓\n" +
                    "§7接替猎人的使命.\n\n" +
                    "§8保持警惕, 保护好自己!"
                );
                break;
            case "book":
                meta.setAuthor("花雨庭");
                meta.addPage(
                    "\n\n§6§l━━━━━━━━\n" +
                    "  §e§l狼人杀指南\n" +
                    "§6§l━━━━━━━━\n\n" +
                    "§7欢迎来到§c§l狼人杀§7!\n\n" +
                    "§7游戏中有以下职业:\n" +
                    "§c狼人 §7- 杀光人类\n" +
                    "§a猎人 §7- 用弓猎杀狼人\n" +
                    "§b预言家 §7- 查验身份\n" +
                    "§f平民 §7- 收集道具击杀狼人\n\n" +
                    "§7收集§e宝石§7购买道具!\n" +
                    "§7找出狼人并击杀他们!"
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
