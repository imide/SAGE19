package com.earthpol.sage;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class MilkListener implements Listener {
    private final SAGE19 plugin;

    public MilkListener(SAGE19 plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent evt) {
        if (evt.getItem().getType() == Material.MILK_BUCKET &&
                plugin.isInfected(evt.getPlayer().getUniqueId())) {
            //evt.getPlayer().sendMessage(ChatColor.RED + "You cannot cure SAGE19 with milk!");
            evt.setCancelled(true);
        }
    }
}
