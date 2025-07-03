package com.earthpol.sage.vaccine;

import com.earthpol.sage.SAGE19;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GiveConcentratedVirusPotionCommand implements CommandExecutor {
    private final SAGE19 plugin;

    public GiveConcentratedVirusPotionCommand(SAGE19 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("sage19.admin")) {
            sender.sendMessage(ChatColor.RED + "You lack permission.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /giveconcentratedviruspotion <player>");
            return false;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        ItemStack potion = PotionManager.createPureSagePotion(Material.POTION);

        target.getInventory().addItem(potion);
        target.sendMessage("You have been given a concentrated Sagevirus potion.");

        return true;
    }
}
