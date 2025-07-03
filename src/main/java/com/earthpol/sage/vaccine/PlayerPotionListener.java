package com.earthpol.sage.vaccine;

import com.earthpol.sage.SAGE19;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class PlayerPotionListener implements Listener {
    private final SAGE19 plugin;

    public PlayerPotionListener(SAGE19 plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPotionConsume(@NotNull PlayerItemConsumeEvent event) {
        ItemStack consumed = event.getItem();
        if (!consumed.hasItemMeta()) return;

        ItemMeta meta = consumed.getItemMeta();
        String pureTag = meta
                .getPersistentDataContainer()
                .get(PotionManager.pureSagePotionKey, PersistentDataType.STRING);
        String vaccineTag = meta
                .getPersistentDataContainer()
                .get(PotionManager.vaccinePotionKey, PersistentDataType.STRING);

        if ("SAGEVIRUS_CONCENTRATED_POTION".equals(pureTag)) {
            plugin.pureInfectPlayer(event.getPlayer());
        } else if ("SAGEVIRUS_VACCINATED_POTION".equals(vaccineTag)) {
            plugin.vaccinatePlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onPotionSplash(@NotNull PotionSplashEvent event) {
        ItemStack potion = event.getPotion().getItem();
        if (!potion.hasItemMeta()) return;

        ItemMeta meta = potion.getItemMeta();
        String pureTag = meta
                .getPersistentDataContainer()
                .get(PotionManager.pureSagePotionKey, PersistentDataType.STRING);
        String vaccineTag = meta
                .getPersistentDataContainer()
                .get(PotionManager.vaccinePotionKey, PersistentDataType.STRING);
        for (LivingEntity affected : event.getAffectedEntities()) {
            if (affected instanceof Player affectedPlayer) {
                if ("SAGEVIRUS_CONCENTRATED_POTION".equals(pureTag)) {
                    if (Math.random() < plugin.getPureInfectSplashChance()) {
                        plugin.pureInfectPlayer(affectedPlayer);
                    }
                } else if ("SAGEVIRUS_VACCINE_POTION".equals(vaccineTag)) {
                    plugin.vaccinatePlayer(affectedPlayer);
                }
            }
        }
    }
}
