package com.aermini.whosrotten.util;

import org.bukkit.enchantments.Enchantment;

public class EnchantUtil {

    public static Enchantment getEnchant(String name) {
        Enchantment e = Enchantment.getByName(name.toUpperCase());
        if (e != null) return e;
        switch (name.toLowerCase()) {
            case "infinity": case "arrow_infinite":
                return Enchantment.ARROW_INFINITE;
            case "sharpness": case "damage_all":
                return Enchantment.DAMAGE_ALL;
            case "smite": case "damage_undead":
                return Enchantment.DAMAGE_UNDEAD;
            case "bane": case "bane_of_arthropods": case "damage_arthropods":
                return Enchantment.DAMAGE_ARTHROPODS;
            case "protection": case "protection_environmental":
                return Enchantment.PROTECTION_ENVIRONMENTAL;
            case "fire_protection": case "protection_fire":
                return Enchantment.PROTECTION_FIRE;
            case "feather_falling": case "protection_fall":
                return Enchantment.PROTECTION_FALL;
            case "blast_protection": case "protection_explosions":
                return Enchantment.PROTECTION_EXPLOSIONS;
            case "projectile_protection": case "protection_projectile":
                return Enchantment.PROTECTION_PROJECTILE;
            case "respiration":
                return Enchantment.OXYGEN;
            case "aqua_affinity":
                return Enchantment.WATER_WORKER;
            case "thorns":
                return Enchantment.THORNS;
            case "fire_aspect":
                return Enchantment.FIRE_ASPECT;
            case "looting": case "looting_bonus":
                return Enchantment.LOOT_BONUS_MOBS;
            case "fortune":
                return Enchantment.LOOT_BONUS_BLOCKS;
            case "power": case "arrow_damage":
                return Enchantment.ARROW_DAMAGE;
            case "punch": case "arrow_knockback":
                return Enchantment.ARROW_KNOCKBACK;
            case "flame": case "arrow_fire":
                return Enchantment.ARROW_FIRE;
            case "luck_of_the_sea": case "luck":
                return Enchantment.LUCK;
            case "lure":
                return Enchantment.LURE;
            case "efficiency": case "dig_speed":
                return Enchantment.DIG_SPEED;
            case "silk_touch":
                return Enchantment.SILK_TOUCH;
            case "unbreaking": case "durability":
                return Enchantment.DURABILITY;
            case "knockback":
                return Enchantment.KNOCKBACK;
            default:
                return null;
        }
    }
}
