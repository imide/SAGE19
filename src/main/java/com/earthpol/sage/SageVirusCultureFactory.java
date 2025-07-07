package com.earthpol.sage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SageVirusCultureFactory {
    public static final String SAGEVIRUS_CULTURE_TAG = "SAGEVIRUS_CULTURE";


    public static ItemStack createVirusCulture(Plugin plugin) {
        // ItemStack for the culture
        ItemStack virusCulture = new ItemStack(Material.FERMENTED_SPIDER_EYE);
        ItemMeta meta = virusCulture.getItemMeta();

        meta.displayName(Component.text("Sagevirus Culture", NamedTextColor.RED, TextDecoration.BOLD));

        // lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("A highly contagious sample of Sagevirus", NamedTextColor.GRAY));
        lore.add(Component.translatable("I wonder what I can do with it...", NamedTextColor.GRAY, TextDecoration.ITALIC));
        meta.lore(lore);

        // custom PDT
        NamespacedKey key = new NamespacedKey(plugin, "sagevirus_culture");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, SAGEVIRUS_CULTURE_TAG);

        virusCulture.setItemMeta(meta);

        return virusCulture;
    }

    public static boolean isVirusCulture(@NotNull ItemStack item, @NotNull Plugin plugin) {
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();

        NamespacedKey key = new NamespacedKey(plugin, "sagevirus_culture");
        String itemType = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return SAGEVIRUS_CULTURE_TAG.equals(itemType);
    }

}
