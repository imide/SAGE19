package com.earthpol.sage;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class InfectionListener implements Listener {
    private final SAGE19 plugin;

    public InfectionListener(SAGE19 plugin) {
        this.plugin = plugin;
    }

    // 25% chance on hit
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent evt) {
        if (evt.getDamager() instanceof Player damager &&
                evt.getEntity() instanceof Player victim &&
                plugin.isInfected(damager.getUniqueId()) &&
                !plugin.isInfected(victim.getUniqueId()) &&
                Math.random() < plugin.getHitChance()) {
            plugin.infectPlayer(victim);
        }
    }

    // reapply effect after respawn
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent evt) {
        Player p = evt.getPlayer();
        if (plugin.isInfected(p.getUniqueId())) {
            EntityScheduler sch = p.getScheduler();
            sch.execute(plugin, () -> {
                plugin.infectPlayer(p);
            }, null, 60L);
        }
    }
}