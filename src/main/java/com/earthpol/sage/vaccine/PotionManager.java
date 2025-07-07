package com.earthpol.sage.vaccine;

import com.earthpol.sage.SAGE19;
import com.earthpol.sage.SageVirusCultureFactory;
import io.papermc.paper.potion.PotionMix;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PotionManager {
    public static NamespacedKey pureSagePotionKey;
    public static NamespacedKey vaccinePotionKey;

    private final SAGE19 plugin;

    public PotionManager(SAGE19 plugin) {
        this.plugin = plugin;
        pureSagePotionKey = new NamespacedKey(plugin, "pure_sage_potion");
        vaccinePotionKey = new NamespacedKey(plugin, "vaccine_potion");
    }

    public static @NotNull ItemStack createPureSagePotion(
            Material potionMaterial
    ) {
        ItemStack potion = new ItemStack(potionMaterial);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        meta.displayName(
                Component.text(
                        "Concentrated Sagevirus Culture",
                        NamedTextColor.RED,
                        TextDecoration.ITALIC
                )
        );

        List<Component> lore = new ArrayList<>();
        lore.add(
                Component.text(
                        "A concentrated petri dish of Sagevirus",
                        NamedTextColor.GRAY
                )
        );
        lore.add(Component.text(""));
        lore.add(
                Component.text(
                        "I think I can use this for a vaccine...",
                        NamedTextColor.GRAY,
                        TextDecoration.ITALIC
                )
        );
        meta.lore(lore);

        meta.setColor(Color.fromRGB(52, 255, 185));
        meta
                .getPersistentDataContainer()
                .set(
                        pureSagePotionKey,
                        PersistentDataType.STRING,
                        "SAGEVIRUS_CONCENTRATED_POTION"
                );

        potion.setItemMeta(meta);
        return potion;
    }

    public static @NotNull ItemStack createVaccinePotion(Material potionType) {
        ItemStack potion = new ItemStack(potionType);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.displayName(
                Component.text(
                        "Bottle of Sagevirus Vaccine",
                        NamedTextColor.GREEN,
                        TextDecoration.ITALIC
                )
        );
        meta.lore(
                List.of(
                        Component.text(
                                "A bottle of the vaccine for all known Sagevirus variants"
                        ),
                        Component.text(""),
                        Component.text("The effectiveness is 99.99%.")
                )
        );
        meta.setColor(Color.fromRGB(52, 255, 185));
        meta
                .getPersistentDataContainer()
                .set(
                        vaccinePotionKey,
                        PersistentDataType.STRING,
                        "SAGEVIRUS_VACCINE_POTION"
                );
        potion.setItemMeta(meta);
        return potion;
    }

    public void registerRecipes() {
        registerSageCultureRecipe();
        registerSplashSageCultureMix();
        registerVaccineMix();
    }

    private void registerSageCultureRecipe() {
        ItemStack waterBottle = new ItemStack(Material.POTION);
        PotionMeta waterMeta = (PotionMeta) waterBottle.getItemMeta();
        waterMeta.setBasePotionType(PotionType.WATER);
        waterBottle.setItemMeta(waterMeta);

        RecipeChoice virusCultureChoice = PotionMix.createPredicateChoice(
                item -> SageVirusCultureFactory.isVirusCulture(item, plugin)
        );

        PotionMix mix = new PotionMix(
                new NamespacedKey(plugin, "mixPureSagePotion"),
                createPureSagePotion(Material.POTION),
                new RecipeChoice.ExactChoice(waterBottle),
                virusCultureChoice
        );

        plugin.getServer().getPotionBrewer().addPotionMix(mix);
    }

    // for some reason there is no auto conversion of potions into splash, whatever
    private void registerSplashSageCultureMix() {
        PotionMix mix = new PotionMix(
                new NamespacedKey(plugin, "mixSplashPureSagePotion"),
                createPureSagePotion(Material.SPLASH_POTION),
                new RecipeChoice.ExactChoice(createPureSagePotion(Material.POTION)),
                new RecipeChoice.ExactChoice(new ItemStack(Material.GUNPOWDER))
        );

        plugin.getServer().getPotionBrewer().addPotionMix(mix);
    }

    private void registerVaccineMix() {
        RecipeChoice fourScrapsChoice = PotionMix.createPredicateChoice(
                item ->
                        item.getType() == Material.DIAMOND && item.getAmount() >= 4
        );

        PotionMix mix = new PotionMix(
                new NamespacedKey(plugin, "mixVaccinePotion"),
                createVaccinePotion(Material.POTION),
                new RecipeChoice.ExactChoice(createPureSagePotion(Material.POTION)),
                fourScrapsChoice
        );

        PotionMix splashMix = new PotionMix(
                new NamespacedKey(plugin, "mixSplashVaccinePotion"),
                createVaccinePotion(Material.SPLASH_POTION),
                new RecipeChoice.ExactChoice(
                        createPureSagePotion(Material.SPLASH_POTION)
                ),
                fourScrapsChoice
        );

        plugin.getServer().getPotionBrewer().addPotionMix(mix);
        plugin.getServer().getPotionBrewer().addPotionMix(splashMix);
    }

}
