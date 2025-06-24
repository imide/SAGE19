package com.earthpol.sage;

import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SAGE19 extends JavaPlugin {
    private final Set<UUID> infected = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService scanner;
    private ScheduledFuture<?> scannerTask;

    // Config values
    private int intervalSeconds;
    private double proximityRadius;
    private double proximityChance;
    private double hitChance;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        loadInfectedData();
        reapplyEffects();

        // Register listeners and commands
        getServer().getPluginManager().registerEvents(new InfectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MilkListener(this), this);
        getCommand("infect").setExecutor(new InfectCommand(this));
        getCommand("uninfect").setExecutor(new UninfectCommand(this));
        Sage19Command sageCmd = new Sage19Command(this);
        getCommand("sage19").setExecutor(sageCmd);
        getCommand("sage19").setTabCompleter(new Sage19TabCompleter());

        // Start global scanner
        scanner = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "SAGE19-GlobalScanner"));
        startScanner();
    }

    @Override
    public void onDisable() {
        if (scannerTask != null) scannerTask.cancel(true);
        if (scanner != null) scanner.shutdownNow();
    }

    private void loadConfigValues() {
        intervalSeconds = getConfig().getInt("infection-interval-seconds", 300);
        proximityRadius = getConfig().getDouble("proximity-radius", 10.0);
        proximityChance = getConfig().getDouble("proximity-chance", 0.5);
        hitChance       = getConfig().getDouble("hit-chance", 0.25);
    }

    private void startScanner() {
        if (scannerTask != null) scannerTask.cancel(false);
        scannerTask = scanner.scheduleAtFixedRate(this::runGlobalScan,
                intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void runGlobalScan() {
        Location safe = Bukkit.getWorlds().get(0).getSpawnLocation();
        RegionScheduler regionScheduler = getServer().getRegionScheduler();
        regionScheduler.run(this, safe, task -> {
            reapplyEffects();
            for (UUID id : infected) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    spreadForPlayer(p);
                    randomEvent(p);
                }
            }
        });
    }

    private void reapplyEffects() {
        for (UUID id : infected) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.addPotionEffect(new PotionEffect(
                        PotionEffectType.OOZING, Integer.MAX_VALUE, 0, true, true, true));
            }
        }
    }

    public void infectPlayer(Player p) {
        if (isWearingPumpkin(p)) return;
        if (infected.add(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You have been infected with SAGE-19.");
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.OOZING, Integer.MAX_VALUE, 0, true, true, true));
            savePlayerData(p);
        }
    }

    public void uninfectPlayer(Player p) {
        if (infected.remove(p.getUniqueId())) {
            p.sendMessage(ChatColor.GREEN + "You have been cured of SAGE-19.");
            p.removePotionEffect(PotionEffectType.OOZING);
            savePlayerData(p);
        }
    }

    public boolean isInfected(UUID id) {
        return infected.contains(id);
    }

    public double getHitChance() {
        return hitChance;
    }

    private void spreadForPlayer(Player carrier) {
        if (isWearingPumpkin(carrier)) return;
        double radiusSq = proximityRadius * proximityRadius;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(carrier)) continue;
            if (!target.getWorld().equals(carrier.getWorld())) continue;
            if (target.getLocation().distanceSquared(carrier.getLocation()) > radiusSq) continue;
            if (isWearingPumpkin(target)) continue;
            if (infected.contains(target.getUniqueId())) continue;
            if (Math.random() < proximityChance) infectPlayer(target);
        }
    }

    private void randomEvent(Player p) {
        double rand = Math.random();
        if (rand < 0.333) return;
        if (rand < 0.666) p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PANDA_SNEEZE, 0.5f, 1f);
        else p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 4 * 20, 0));
    }

    private boolean isWearingPumpkin(Player p) {
        ItemStack helm = p.getInventory().getHelmet();
        return helm != null && helm.getType() == Material.CARVED_PUMPKIN;
    }

    private void savePlayerData(Player p) {
        File dir = new File(getDataFolder(), "playerdata"); if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, p.getUniqueId() + ".yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("isInfected", infected.contains(p.getUniqueId()));
        cfg.set("infectedAt", System.currentTimeMillis());
        try { cfg.save(file); } catch (IOException ignored) {}
    }

    private void loadInfectedData() {
        File dir = new File(getDataFolder(), "playerdata");
        if (!dir.exists()) return;
        for (File f : Objects.requireNonNull(dir.listFiles((d,n)->n.endsWith(".yml")))) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            if (cfg.getBoolean("isInfected", false)) {
                try { infected.add(UUID.fromString(f.getName().replace(".yml",""))); }
                catch (IllegalArgumentException e) {}
            }
        }
    }

    private void randomEventCommand(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sage19 <interval|radius|proximitychance|hitchance> <value>");
            return;
        }
        String key = args[0].toLowerCase();
        try {
            switch (key) {
                case "interval":
                    intervalSeconds = Integer.parseInt(args[1]);
                    getConfig().set("infection-interval-seconds", intervalSeconds);
                    startScanner();
                    break;
                case "radius":
                    proximityRadius = Double.parseDouble(args[1]);
                    getConfig().set("proximity-radius", proximityRadius);
                    break;
                case "proximitychance":
                    proximityChance = Double.parseDouble(args[1]);
                    getConfig().set("proximity-chance", proximityChance);
                    break;
                case "hitchance":
                    hitChance = Double.parseDouble(args[1]);
                    getConfig().set("hit-chance", hitChance);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown setting: " + key);
                    return;
            }
            saveConfig();
            sender.sendMessage(ChatColor.GREEN + "Set " + key + " to " + args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid number: " + args[1]);
        }
    }

    private class Sage19Command implements CommandExecutor {
        private final SAGE19 plugin;
        Sage19Command(SAGE19 plugin) { this.plugin = plugin; }
        @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!sender.hasPermission("sage19.admin")) {
                sender.sendMessage(ChatColor.RED + "You lack permission.");
                return true;
            }
            randomEventCommand(sender, args);
            return true;
        }
    }

    private class Sage19TabCompleter implements TabCompleter {
        private final List<String> keys = Arrays.asList("interval","radius","proximitychance","hitchance");
        @Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length == 1) {
                return keys.stream()
                        .filter(k -> k.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }
}
