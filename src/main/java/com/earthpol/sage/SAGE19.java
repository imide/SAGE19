package com.earthpol.sage;

import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SAGE19 extends JavaPlugin {
    private final Set<UUID> infected = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService scanner;

    // Config values
    private int intervalSeconds;
    private double proximityRadius;
    private double proximityChance;
    private double hitChance;

    @Override
    public void onEnable() {
        getLogger().info("Enabling SAGE-19 plugin...");
        saveDefaultConfig();
        loadConfigValues();
        loadInfectedData();

        // Register events and commands
        getServer().getPluginManager().registerEvents(new InfectionListener(this), this);
        getCommand("infect").setExecutor(new InfectCommand(this));
        getCommand("uninfect").setExecutor(new UninfectCommand(this));
        getCommand("sage19").setExecutor(new Sage19Command(this));
        getServer().getPluginManager().registerEvents(new MilkListener(this), this);

        // Start global scanner
        scanner = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "SAGE-19 GlobalScanner"));
        scanner.scheduleAtFixedRate(() -> {
            getLogger().info("Global scan: processing " + infected.size() + " infected players");
            Location safe = Bukkit.getWorlds().get(0).getSpawnLocation();
            RegionScheduler regionScheduler = getServer().getRegionScheduler();
            regionScheduler.run(this, safe, task -> {
                for (UUID id : infected) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null && p.isOnline()) {
                        spreadForPlayer(p);

                        p.addPotionEffect(new PotionEffect(
                                PotionEffectType.OOZING,
                                Integer.MAX_VALUE,
                                0,
                                true,
                                true,
                                true
                        ));

                        double rand = Math.random();
                        if (rand < 0.333) {
                            // Nothing happens
                        } else if (rand < 0.666) {
                            // Panda sneeze sound
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PANDA_SNEEZE, 0.5f, 1f);
                        } else {
                            // 4 seconds of nausea (NAUSEA)
                            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 4 * 20, 0));
                        }
                    }
                }
            });
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);

        getLogger().info(String.format(
                "SAGE-19 enabled: interval=%ds, radius=%.1f, proximityChance=%.2f, hitChance=%.2f",
                intervalSeconds, proximityRadius, proximityChance, hitChance
        ));
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling SAGE-19 plugin...");
        if (scanner != null) {
            scanner.shutdownNow();
            getLogger().info("Global scanner stopped.");
        }
    }

    private void loadConfigValues() {
        intervalSeconds = getConfig().getInt("infection-interval-seconds", 300);
        proximityRadius = getConfig().getDouble("proximity-radius", 10.0);
        proximityChance = getConfig().getDouble("proximity-chance", 0.5);
        hitChance       = getConfig().getDouble("hit-chance", 0.25);
        getLogger().info(String.format(
                "Config loaded: interval=%ds, radius=%.1f, proximityChance=%.2f, hitChance=%.2f",
                intervalSeconds, proximityRadius, proximityChance, hitChance
        ));
    }

    /** Infect a player */
    public void infectPlayer(Player p) {

        ItemStack helm = p.getInventory().getHelmet();
        if (helm != null && helm.getType() == Material.CARVED_PUMPKIN) return;

        if (infected.add(p.getUniqueId())) {
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.OOZING, Integer.MAX_VALUE, 0, true, true, true
            ));
            p.sendMessage(ChatColor.RED + "You have been infected with SAGE-19.");
            getLogger().info("Player infected: " + p.getName());
            savePlayerData(p);
        }
    }

    /** Cure a player */
    public void uninfectPlayer(Player p) {
        if (infected.remove(p.getUniqueId())) {
            p.removePotionEffect(PotionEffectType.OOZING);
            p.sendMessage(ChatColor.GREEN + "You have been cured of SAGE-19.");
            getLogger().info("Player cured: " + p.getName());
            savePlayerData(p);
        }
    }

    /** Check if a player is currently infected */
    public boolean isInfected(UUID id) {
        return infected.contains(id);
    }

    /** Get the configured hit chance for direct contact infection */
    public double getHitChance() {
        return hitChance;
    }

    /** Spread infection from a carrier to nearby targets */
    private void spreadForPlayer(Player carrier) {
        double radiusSq = proximityRadius * proximityRadius;
        int checked = 0, newInf = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            checked++;
            if (target.equals(carrier)) continue;
            if (!target.getWorld().equals(carrier.getWorld())) continue;
            if (target.getLocation().distanceSquared(carrier.getLocation()) > radiusSq) continue;
            if (infected.contains(target.getUniqueId())) continue;
            if (Math.random() < proximityChance) {
                infectPlayer(target);
                newInf++;
            }
        }
        if(newInf <= 1){
            getLogger().info(String.format(
                    "%s spread: checked=%d, new=%d", carrier.getName(), checked, newInf
            ));
        }
    }

    private void savePlayerData(Player p) {
        try {
            File dir = new File(getDataFolder(), "playerdata");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, p.getUniqueId() + ".yml");
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            cfg.set("isInfected", infected.contains(p.getUniqueId()));
            cfg.set("infectedAt", System.currentTimeMillis());
            cfg.save(file);
        } catch (IOException ex) {
            getLogger().severe("Failed to save data for " + p.getName());
        }
    }

    private void loadInfectedData() {
        File dir = new File(getDataFolder(), "playerdata");
        if (!dir.exists()) return;
        int count = 0;
        for (File f : Objects.requireNonNull(dir.listFiles((d, n) -> n.endsWith(".yml")))) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            if (cfg.getBoolean("isInfected", false)) {
                try {
                    infected.add(UUID.fromString(f.getName().replace(".yml", "")));
                    count++;
                } catch (IllegalArgumentException ignored) {}
            }
        }
        getLogger().info("Loaded " + count + " infected from disk.");
    }

    private void randomEventCommand(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sage19 <setting> <value>");
            return;
        }
        String key = args[0].toLowerCase();
        try {
            switch (key) {
                case "interval":
                    intervalSeconds = Integer.parseInt(args[1]);
                    getConfig().set("infection-interval-seconds", intervalSeconds);
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
        public Sage19Command(SAGE19 plugin) { this.plugin = plugin; }

        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!sender.hasPermission("sage19.admin")) {
                sender.sendMessage(ChatColor.RED + "You lack permission.");
                return true;
            }
            randomEventCommand(sender, args);
            return true;
        }
    }
}
