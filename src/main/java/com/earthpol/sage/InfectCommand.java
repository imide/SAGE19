package com.earthpol.sage;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InfectCommand implements CommandExecutor {
    private final SAGE19 plugin;

    public InfectCommand(SAGE19 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command cmd,
                             String label,
                             String[] args) {
        if (!sender.hasPermission("sage19.admin")) {
            sender.sendMessage(ChatColor.RED + "You lack permission.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /infect <player>");
            return false;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        if (plugin.isVaccinated(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "This player is vaccinated and cannot be infected.");
            return true;
        }

        plugin.infectPlayer(target);
        sender.sendMessage(ChatColor.GREEN + "Infected " + target.getName() + ".");
        return true;
    }
}