package com.earthpol.sage.vaccine;

import com.earthpol.sage.SAGE19;
import com.earthpol.sage.SageVirusCultureFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BrewingListener implements Listener {
    private static NamespacedKey pureSagePotionKey;
    private static NamespacedKey vaccinePotionKey;
    private final SAGE19 plugin;

    public BrewingListener(SAGE19 plugin) {
        this.plugin = plugin;
        pureSagePotionKey = new NamespacedKey(plugin, "pure_sage_potion");
        vaccinePotionKey = new NamespacedKey(plugin, "vaccine_potion");
    }

    public static @NotNull ItemStack createPureSagePotion(Material potionType) {
        ItemStack potion = new ItemStack(potionType);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        meta.displayName(Component.text("Concentrated Sagevirus Culture", NamedTextColor.RED, TextDecoration.ITALIC));

        List<Component> lore = new ArrayList<>();

        lore.add(Component.text("A concentrated petri dish of Sagevirus", NamedTextColor.GRAY));
        lore.add(Component.text(""));
        lore.add(Component.text("I think I can use this for a vaccine...", NamedTextColor.GRAY, TextDecoration.ITALIC));
        meta.lore(lore);

        meta.setColor(Color.fromRGB(52, 255, 185));

        meta.getPersistentDataContainer().set(pureSagePotionKey, PersistentDataType.STRING, "SAGEVIRUS_CONCENTRATED_POTION");

        potion.setItemMeta(meta);
        return potion;
    }

    public static @NotNull ItemStack createVaccinePotion(Material potionType) {
        ItemStack potion = new ItemStack(potionType);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        List<Component> lore = new ArrayList<>();

        meta.displayName(Component.text("Bottle of Sagevirus Vaccine", NamedTextColor.GREEN, TextDecoration.ITALIC));

        lore.add(Component.text("A bottle of the vaccine for all known Sagevirus variants"));
        lore.add(Component.text(""));
        lore.add(Component.text("The effectiveness is 99.99%."));
        meta.lore(lore);

        meta.setColor(Color.fromRGB(52, 255, 185));

        meta.getPersistentDataContainer().set(vaccinePotionKey, PersistentDataType.STRING, "SAGEVIRUS_VACCINE_POTION");

        potion.setItemMeta(meta);
        return potion;
    }

    @EventHandler
    public void onBrew(@NotNull BrewEvent event) {
        BrewerInventory inventory = event.getContents();
        ItemStack ingredient = inventory.getIngredient();
        List<ItemStack> results = event.getResults();

        assert ingredient != null;
        if (SageVirusCultureFactory.isVirusCulture(ingredient, plugin)) {

            for (int i = 0; i < results.size(); i++) {
                ItemStack originalPotion = results.get(i);
                if (originalPotion != null && originalPotion.getType() == Material.POTION) {
                    results.set(i, createPureSagePotion(Material.POTION));
                }
            }
            return;
        }

        if (ingredient.getType() == Material.GUNPOWDER) {
            boolean customPotionFound = false;
            for (int i = 0; i < results.size(); i++) {
                ItemStack originalPotion = results.get(i);
                if (originalPotion != null && originalPotion.getType() == Material.POTION &&
                        "SAGEVIRUS_CONCENTRATED_POTION".equals(originalPotion.getItemMeta().getPersistentDataContainer().get(pureSagePotionKey, PersistentDataType.STRING))
                ) {
                    results.set(i, createPureSagePotion(Material.SPLASH_POTION));
                    customPotionFound = true;
                }
            }

            if (customPotionFound) event.setCancelled(false);
            return;
        }

        if (ingredient.getType() == Material.NETHERITE_SCRAP) {
            boolean customPotionFound = false;
            for (int i = 0; i < results.size(); i++) {
                ItemStack originalPotion = results.get(i);
                if (originalPotion != null && (originalPotion.getType() == Material.POTION || originalPotion.getType() == Material.SPLASH_POTION) &&
                        "SAGEVIRUS_CONCENTRATED_POTION".equals(originalPotion.getItemMeta().getPersistentDataContainer().get(pureSagePotionKey, PersistentDataType.STRING))
                ) {
                    Material vaccineResultMaterial = originalPotion.getType();
                    results.set(i, createVaccinePotion(vaccineResultMaterial));
                    customPotionFound = true;
                }
            }
            if (customPotionFound) event.setCancelled(false);
        }
    }

    @EventHandler
    public void onPotionConsume(@NotNull PlayerItemConsumeEvent event) {
        ItemStack consumedItem = event.getItem();
        Player player = event.getPlayer();

        if (!consumedItem.hasItemMeta()) return;

        ItemMeta meta = consumedItem.getItemMeta();
        String potionType = meta.getPersistentDataContainer().get(pureSagePotionKey, PersistentDataType.STRING);

        if ("SAGEVIRUS_CONCENTRATED_POTION".equals(potionType)) {
            plugin.pureInfectPlayer(player);
        }
    }

    @EventHandler
    public void onPotionSplash(@NotNull PotionSplashEvent event) {
        ItemStack potionItem = event.getPotion().getItem();

        if (!potionItem.hasItemMeta()) return;

        ItemMeta meta = potionItem.getItemMeta();
        // itemTag is for the concentrated sagevirus, not the vaccine
        String itemTag = meta.getPersistentDataContainer().get(pureSagePotionKey, PersistentDataType.STRING);
        String vaccineTag = meta.getPersistentDataContainer().get(vaccinePotionKey, PersistentDataType.STRING);

        for (LivingEntity affected : event.getAffectedEntities()) {
            if (affected instanceof Player affectedPlayer) {
                if ("SAGEVIRUS_CONCENTRATED_POTION".equals(itemTag)) {
                    // 25% chance
                    double splashEffectChance = 0.25;
                    if (Math.random() < splashEffectChance) {
                        plugin.pureInfectPlayer(affectedPlayer);
                    }
                } else if ("SAGEVIRUS_VACCINE_POTION".equals(vaccineTag)) {
                    plugin.vaccinatePlayer(affectedPlayer);
                }
            }
        }
    }


}
