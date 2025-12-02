package me.BaddCamden.SBPCLifestealItems;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Centralized dictionary for Enchanted Branch enchantment pools.
 */
public final class EnchantPools {

    private EnchantPools() {
    }

    public static List<Enchantment> getPool(Material mat) {
        String name = mat.name();
        List<Enchantment> result = new ArrayList<>();

        if (name.endsWith("_AXE")) {
            result.add(Enchantment.SHARPNESS);
            result.add(Enchantment.UNBREAKING);
            result.add(Enchantment.BANE_OF_ARTHROPODS);
            result.add(Enchantment.SMITE);
            result.add(Enchantment.FORTUNE);
            result.add(Enchantment.EFFICIENCY);
        } else if (name.endsWith("_PICKAXE")) {
            result.add(Enchantment.EFFICIENCY);
            result.add(Enchantment.UNBREAKING);
            result.add(Enchantment.FORTUNE);
        } else if (name.endsWith("_SHOVEL")) {
            result.add(Enchantment.EFFICIENCY);
            result.add(Enchantment.UNBREAKING);
            result.add(Enchantment.FORTUNE);
        } else if (name.endsWith("_HOE")) {
            result.add(Enchantment.EFFICIENCY);
            result.add(Enchantment.UNBREAKING);
            result.add(Enchantment.SILK_TOUCH);
        } else if (name.endsWith("_SWORD")) {
            result.add(Enchantment.SHARPNESS);
            result.add(Enchantment.BANE_OF_ARTHROPODS);
            result.add(Enchantment.SMITE);
            result.add(Enchantment.KNOCKBACK);
            result.add(Enchantment.UNBREAKING);
        } else if (name.endsWith("_HELMET")) {
            result.add(Enchantment.PROTECTION);
            result.add(Enchantment.BLAST_PROTECTION);
            result.add(Enchantment.FIRE_PROTECTION);
            result.add(Enchantment.PROJECTILE_PROTECTION);
            result.add(Enchantment.UNBREAKING);
            result.add(Enchantment.AQUA_AFFINITY);
        } else if (name.endsWith("_CHESTPLATE")) {
            result.add(Enchantment.PROTECTION);
            result.add(Enchantment.BLAST_PROTECTION);
            result.add(Enchantment.FIRE_PROTECTION);
            result.add(Enchantment.PROJECTILE_PROTECTION);
            result.add(Enchantment.UNBREAKING);
            result.add(Enchantment.THORNS);
        } else if (name.endsWith("_LEGGINGS")) {
            result.add(Enchantment.PROTECTION);
            result.add(Enchantment.BLAST_PROTECTION);
            result.add(Enchantment.FIRE_PROTECTION);
            result.add(Enchantment.PROJECTILE_PROTECTION);
            result.add(Enchantment.UNBREAKING);
        } else if (name.endsWith("_BOOTS")) {
            result.add(Enchantment.PROTECTION);
            result.add(Enchantment.BLAST_PROTECTION);
            result.add(Enchantment.FIRE_PROTECTION);
            result.add(Enchantment.PROJECTILE_PROTECTION);
            result.add(Enchantment.UNBREAKING);
            result.add(Enchantment.FEATHER_FALLING);
        }

        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return result;
    }
}
