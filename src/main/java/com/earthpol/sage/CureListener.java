package com.earthpol.sage;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import static com.earthpol.sage.SageVirusCultureFactory.createVirusCulture;

public class CureListener implements Listener {
    private final SAGE19 plugin;

    public CureListener(SAGE19 plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent evt) {
        if (!plugin.isCuringEnabled()) return;
        Player p = evt.getPlayer();
        // only care about golden apples
        if (evt.getItem().getType() != Material.GOLDEN_APPLE) return;

        // must be infected and under Weakness
        if (!plugin.isInfected(p.getUniqueId())) return;
        if (!p.hasPotionEffect(PotionEffectType.WEAKNESS)) return;

        double cureSuccessChance = plugin.getCureSuccessChance();
        double cultureDropChance = plugin.getCultureDropChance();
        if (Math.random() < cureSuccessChance) {
            plugin.uninfectPlayer(p);
            p.sendMessage(ChatColor.GOLD + "You feel a sudden surge of strength—SAGE-19 cured!");
            if (Math.random() < cultureDropChance) {
                ItemStack virusCulture = createVirusCulture(plugin);
                p.getWorld().dropItemNaturally(p.getLocation(), virusCulture);
                p.sendMessage(ChatColor.GRAY + "A live culture of the virus was extracted from you...");
            }
        } else {
            p.sendMessage(ChatColor.RED + "The cure failed—SAGE-19 persists.");
        }
    }
}
