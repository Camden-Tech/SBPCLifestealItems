package me.BaddCamden.SBPCLifestealItems;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Loads/saves per-player usage data from/to individual YAML files.
 * All disk I/O is done during plugin enable/disable only.
 */
public class PlayerUsageStore {

    private final JavaPlugin plugin;
    private final File folder;
    private final Map<UUID, PlayerUsage> usageMap = new HashMap<>();

    /**
     * Creates a new persistent store using the plugin's data directory for
     * reading and writing usage YAML files.
     */
    public PlayerUsageStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "players");
    }

    /**
     * Loads all usage files from disk into memory, skipping malformed entries
     * but keeping the plugin online.
     */
    public void loadAll() {
        usageMap.clear();
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            if (!name.toLowerCase().endsWith(".yml")) continue;
            String uuidPart = name.substring(0, name.length() - 4);
            try {
                UUID uuid = UUID.fromString(uuidPart);
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                PlayerUsage usage = new PlayerUsage(uuid);
                usage.setTrackingCompassUses(cfg.getInt("trackingCompassUses", 0));
                usage.setArmorPaddingUses(cfg.getInt("armorPaddingUses", 0));
                usage.setEnchantedBranchUses(cfg.getInt("enchantedBranchUses", 0));
                usage.setHeartMedsUses(cfg.getInt("heartMedsUses", 0));
                usageMap.put(uuid, usage);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Skipping invalid player usage file: " + name);
            }
        }
    }

    /**
     * Persists every tracked player's usage statistics to individual YAML
     * files inside the plugin data folder.
     */
    public void saveAll() {
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create players data folder: " + folder.getPath());
            return;
        }
        for (Map.Entry<UUID, PlayerUsage> entry : usageMap.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerUsage usage = entry.getValue();
            File file = new File(folder, uuid.toString() + ".yml");
            FileConfiguration cfg = new YamlConfiguration();
            cfg.set("trackingCompassUses", usage.getTrackingCompassUses());
            cfg.set("armorPaddingUses", usage.getArmorPaddingUses());
            cfg.set("enchantedBranchUses", usage.getEnchantedBranchUses());
            cfg.set("heartMedsUses", usage.getHeartMedsUses());
            try {
                cfg.save(file);
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to save usage data for " + uuid + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Retrieves the usage record for a player, instantiating a new empty entry
     * when none is currently cached.
     */
    public PlayerUsage getOrCreate(UUID uuid) {
        PlayerUsage usage = usageMap.get(uuid);
        if (usage == null) {
            usage = new PlayerUsage(uuid);
            usageMap.put(uuid, usage);
        }
        return usage;
    }
}
