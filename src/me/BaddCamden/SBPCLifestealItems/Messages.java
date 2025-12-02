package me.BaddCamden.SBPCLifestealItems;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Loads all necessary messages from config.yml during plugin enable
 * and provides colorized lookups without touching config at runtime.
 */
public class Messages {

    private final JavaPlugin plugin; // NEW

    private final Map<String, String> strings = new HashMap<>();
    private final Map<String, List<String>> lists = new HashMap<>();

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration cfg = plugin.getConfig();

        // Tracking Compass messages
        loadString(cfg, "messages.tracking-compass.name");
        loadList(cfg,   "messages.tracking-compass.lore");
        loadString(cfg, "messages.tracking-compass.not-unlocked");
        loadString(cfg, "messages.tracking-compass.already-active");
        loadString(cfg, "messages.tracking-compass.no-targets");
        loadString(cfg, "messages.tracking-compass.locked-onto");
        loadString(cfg, "messages.tracking-compass.target-left");
        loadString(cfg, "messages.tracking-compass.shattered");

        // Armor Padding messages
        loadString(cfg, "messages.armor-padding.name");
        loadList(cfg,   "messages.armor-padding.lore");
        loadString(cfg, "messages.armor-padding.lore-format");

        // Enchanted Branch messages
        loadString(cfg, "messages.enchanted-branch.name");
        loadList(cfg,   "messages.enchanted-branch.lore");
        loadString(cfg, "messages.enchanted-branch.not-unlocked");
        loadString(cfg, "messages.enchanted-branch.apply-success");
        loadString(cfg, "messages.enchanted-branch.no-valid-enchant");

        // Heart Meds messages
        loadString(cfg, "messages.heart-meds.name");
        loadList(cfg,   "messages.heart-meds.lore");
        loadString(cfg, "messages.heart-meds.not-unlocked");
        loadString(cfg, "messages.heart-meds.already-used");
        loadString(cfg, "messages.heart-meds.not-missing");
        loadString(cfg, "messages.heart-meds.consumed");
    }


    private void loadString(FileConfiguration cfg, String path) {
        if (!cfg.isSet(path)) {
            plugin.getLogger().warning("[Messages] Missing string in config.yml at '" + path + "'");
        }
        String value = cfg.getString(path, "");
        strings.put(path, color(value));
    }

    private void loadList(FileConfiguration cfg, String path) {
        if (!cfg.isList(path)) {
            plugin.getLogger().warning("[Messages] Missing or non-list value in config.yml at '" + path + "'");
        }
        List<String> raw = cfg.getStringList(path);
        List<String> colored = new ArrayList<>();
        for (String line : raw) {
            colored.add(color(line));
        }
        lists.put(path, colored);
    }


    private String color(String input) {
        if (input == null) return "";
        return input.replace('&', '§');
    }

    public String get(String path) {
        String v = strings.get(path);
        if (v == null || v.isEmpty()) {
            // Visible fallback so items/messages are never fully blank
            plugin.getLogger().warning("[Messages] No value loaded for path '" + path + "'");
            return "§c[" + path + "]";
        }
        return v;
    }

    public List<String> getList(String path) {
        List<String> v = lists.get(path);
        if (v == null) return Collections.emptyList();
        return v;
    }

    public String format(String path, Map<String, String> params) {
        String msg = get(path);
        for (Map.Entry<String, String> e : params.entrySet()) {
            msg = msg.replace("{" + e.getKey() + "}", e.getValue());
        }
        return msg;
    }
}
