package com.earthpol.sage;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.*;
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
    private double cureSuccessChance;
    private boolean curingEnabled;
    private double maskInfectionChance;

    public boolean isCuringEnabled() {
        return curingEnabled;
    }

    public double getCureSuccessChance() {
        return cureSuccessChance;
    }

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
        getServer().getPluginManager().registerEvents(new CureListener(this), this);

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
        cureSuccessChance = getConfig().getDouble("cure-success-chance", 0.95);
        curingEnabled      = getConfig().getBoolean("curing-enabled", false);
        maskInfectionChance  = getConfig().getDouble("mask-infection-chance", 0.05);
    }

    private void startScanner() {
        if (scannerTask != null) scannerTask.cancel(false);
        scannerTask = scanner.scheduleAtFixedRate(this::runGlobalScan,
                intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void runGlobalScan() {
        Location safe = Bukkit.getWorlds().get(0).getSpawnLocation();
        RegionScheduler regionScheduler = getServer().getRegionScheduler();

        // Region lock to safely iterate 'infected'
        regionScheduler.run(this, safe, task -> {
            for (UUID id : infected) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    // Schedule *all* entity operations on that player's thread:
                    EntityScheduler sch = p.getScheduler();
                    sch.execute(this, () -> {
                        // 1) Spread logic (this may call infectPlayer, which you already schedule)
                        spreadForPlayer(p);

                        // 2) Re-apply infinite OOZING
                        p.addPotionEffect(new PotionEffect(
                                PotionEffectType.OOZING, Integer.MAX_VALUE, 0, true, true, true
                        ));

                        // 3) Trigger a random event.
                        randomEvent(p);
                    }, null, 0L);
                }
            }
        });
    }

    private void reapplyEffects() {
        for (UUID id : infected) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                EntityScheduler scheduler = p.getScheduler();
                scheduler.execute(this, () -> {
                    p.addPotionEffect(new PotionEffect(
                            PotionEffectType.OOZING, Integer.MAX_VALUE, 0, true, true, true));
                }, null, 20);
            }
        }
    }

    public void infectPlayer(Player p) {
        boolean wearingPumpkin = isWearingPumpkin(p);
        if (wearingPumpkin && Math.random() > maskInfectionChance) return;
        if (infected.add(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You have been infected with SAGE-19.");
            EntityScheduler scheduler = p.getScheduler();
            scheduler.execute(this, () -> {
                p.addPotionEffect(new PotionEffect(
                        PotionEffectType.OOZING, Integer.MAX_VALUE, 0, true, true, true));
            }, null, 20);
            savePlayerData(p);
        }
    }

    public void uninfectPlayer(Player p) {
        if (infected.remove(p.getUniqueId())) {
            p.sendMessage(ChatColor.GREEN + "You have been cured of SAGE-19.");
            EntityScheduler scheduler = p.getScheduler();
            scheduler.execute(this, () -> {
                p.removePotionEffect(PotionEffectType.OOZING);
            }, null, 20);
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
        if (isWearingPumpkin(carrier) && Math.random() > maskInfectionChance) return;
        double radiusSq = proximityRadius * proximityRadius;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(carrier)) continue;
            if (!target.getWorld().equals(carrier.getWorld())) continue;
            if (target.getLocation().distanceSquared(carrier.getLocation()) > radiusSq) continue;
            boolean targetWearing = isWearingPumpkin(target);
            if (targetWearing && Math.random() > maskInfectionChance) continue;
            if (infected.contains(target.getUniqueId())) continue;
            if (Math.random() < proximityChance) infectPlayer(target);
        }
    }

    private void randomEvent(Player p) {
        double rand = Math.random();
        // Approximately equal 1-in-6 chance for each event
        if (rand < 1.0/6) {
            // Nothing happens
        } else if (rand < 2.0/6) {
            // Panda sneeze sound
            p.playSound(p.getLocation(), Sound.ENTITY_PANDA_SNEEZE, 0.5f, 1f);
        } else if (rand < 3.0/6) {
            // Nausea (Confusion) for 4 seconds, always show particles
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.NAUSEA,
                    4 * 20,
                    0,
                    true,
                    false,
                    true
            ));
        } else if (rand < 4.0/6) {
            // Blindness level 5 for 2 seconds, always show particles
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS,
                    2 * 20,
                    5,
                    true,
                    false,
                    true
            ));
        } else if (rand < 5.0/6) {
            // Hunger for 5 seconds, always show particles
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.HUNGER,
                    5 * 20,
                    0,
                    true,
                    false,
                    true
            ));
        } else {
            // Regeneration for 7 seconds and Poison for 5 seconds, both always show particles
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION,
                    7 * 20,
                    3,
                    true,
                    false,
                    false
            ));
            p.addPotionEffect(new PotionEffect(
                    PotionEffectType.POISON,
                    5 * 20,
                    0,
                    true,
                    true,
                    true
            ));
        }
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
                case "curesuccesschance":
                    cureSuccessChance = Double.parseDouble(args[1]);
                    getConfig().set("cure-success-chance", cureSuccessChance);
                    break;
                case "maskchance":
                    maskInfectionChance = Double.parseDouble(args[1]);
                    getConfig().set("mask-infection-chance", maskInfectionChance);
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
            if (args.length == 0) {
                int total = plugin.infected.size();
                long online = plugin.infected.stream()
                        .filter(id -> {
                            Player p = Bukkit.getPlayer(id);
                            return p != null && p.isOnline();
                        })
                        .count();
                sender.sendMessage(ChatColor.GREEN + String.format(
                        "SAGE-19 infected players: %d total, %d online", total, online
                ));
                return true;
            }

            if (!sender.hasPermission("sage19.admin")) {
                sender.sendMessage(ChatColor.RED + "You lack permission.");
                return true;
            }

            String key = args[0].toLowerCase();
            // 2-arg toggle handlers:
            if (args.length == 2 && key.equals("enable") && args[1].equalsIgnoreCase("curing")) {
                curingEnabled = true;
                getConfig().set("curing-enabled", true);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Curing enabled.");
                return true;
            }
            if (args.length == 2 && key.equals("disable") && args[1].equalsIgnoreCase("curing")) {
                curingEnabled = false;
                getConfig().set("curing-enabled", false);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Curing disabled.");
                return true;
            }


            randomEventCommand(sender, args);
            return true;
        }
    }

    private class Sage19TabCompleter implements TabCompleter {
        private final List<String> keys = Arrays.asList("interval","radius","proximitychance","hitchance", "curesuccesschance", "maskchance", "enable", "disable");
        @Override public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
            if (args.length == 1) {
                return keys.stream()
                        .filter(k -> k.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args.length == 2 && (args[0].equalsIgnoreCase("enable")
                    || args[0].equalsIgnoreCase("disable"))) {
                return Collections.singletonList("curing");
            }

            return Collections.emptyList();
        }
    }
}
