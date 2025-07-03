package com.earthpol.sage.vaccine;

import com.earthpol.sage.SAGE19;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

public class BrewingListener implements Listener {
    private final SAGE19 plugin;

    public BrewingListener(SAGE19 plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        BrewerInventory inventory = event.getContents();
        ItemStack ingredient = inventory.getIngredient();

        if (
                ingredient == null ||
                        ingredient.getType() != Material.DIAMOND ||
                        ingredient.getAmount() < 4
        ) {
            return;
        }
        RegionScheduler sch = plugin.getServer().getRegionScheduler();
        sch.execute(
                plugin,
                event.getBlock().getLocation(),
                () -> {
                    // The brewing stand already consumes 1 item, so - 3 makes it 4. glad we know simple math
                    ingredient.setAmount(ingredient.getAmount() - 3);
                });
    }
}
