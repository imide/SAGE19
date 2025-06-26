package com.earthpol.sage;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffectType;

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

        double chance = plugin.getCureSuccessChance();
        if (Math.random() < chance) {
            plugin.uninfectPlayer(p);
            p.sendMessage(ChatColor.GOLD + "You feel a sudden surge of strength—SAGE-19 cured!");
        } else {
            p.sendMessage(ChatColor.RED + "The cure failed—SAGE-19 persists.");
        }
    }
}
