package me.BaddCamden.SBPCLifestealItems;

import me.BaddCamden.SBPC.api.SbpcAPI;
import me.BaddCamden.SBPC.events.UnlockItemEvent;
import me.BaddCamden.SBPC.progress.PlayerProgress;
import me.BaddCamden.SBPC.progress.ProgressEntry;
import me.BaddCamden.SBPC.progress.ProgressEntry.EntryKind;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class SBPCLifestealItemsPlugin extends JavaPlugin implements Listener {

    private Messages messages;
    private PlayerUsageStore usageStore;

    // Cached settings
    private int trackingCompassDurationSeconds;
    private int trackingCompassSoundIntervalSeconds;
    private int armorPaddingUnitsPerItem;
    private int armorPaddingMaxPerPiece;
    private int heartMedsHeartsRestoredHearts;
    private Set<Material> allowedArmorMaterials = Collections.emptySet();

    // SBPC custom item keys
    private static final String KEY_TRACKING_COMPASS = "tracking_compass";
    private static final String KEY_ARMOR_PADDING = "armor_padding";
    private static final String KEY_ENCHANTED_BRANCH = "enchanted_branch";
    private static final String KEY_HEART_MEDS = "heart_meds";

    // PersistentDataContainer keys
    private NamespacedKey trackingCompassKey;
    private NamespacedKey armorPaddingKey;
    private NamespacedKey armorPaddingLevelKey;
    private NamespacedKey enchantedBranchKey;
    private NamespacedKey heartMedsKey;
    private NamespacedKey trailMixKey;



    // Recipe keys
    private NamespacedKey recipeTrackingCompassKey;
    private NamespacedKey recipeArmorPaddingKey;
    private NamespacedKey recipeEnchantedBranchKey;
    private NamespacedKey recipeHeartMedsKey;
    private NamespacedKey recipeTrailMixKey;
    private NamespacedKey recipeRecoveryCompassKey;
    // Active tracking-compass tasks
    private final Map<UUID, BukkitTask> activeTracking = new HashMap<>();

    private final Random random = new Random();

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("SBPC") == null) {
            getLogger().severe("SBPC not found; disabling SBPCLifestealItems.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        reloadConfig(); // only here, not during runtime

        messages = new Messages(this);
        usageStore = new PlayerUsageStore(this);
        usageStore.loadAll();

        loadSettings();

        trackingCompassKey = new NamespacedKey(this, "tracking_compass");
        armorPaddingKey = new NamespacedKey(this, "armor_padding");
        armorPaddingLevelKey = new NamespacedKey(this, "armor_padding_level");
        enchantedBranchKey = new NamespacedKey(this, "enchanted_branch");
        heartMedsKey = new NamespacedKey(this, "heart_meds");
        trailMixKey = new NamespacedKey(this, "trail_mix");

        recipeTrailMixKey = new NamespacedKey(this, "trail_mix_recipe");
        recipeRecoveryCompassKey = new NamespacedKey(this, "recovery_compass_recipe");
        recipeTrackingCompassKey = new NamespacedKey(this, "tracking_compass_recipe");
        recipeArmorPaddingKey = new NamespacedKey(this, "armor_padding_recipe");
        recipeEnchantedBranchKey = new NamespacedKey(this, "enchanted_branch_recipe");
        recipeHeartMedsKey = new NamespacedKey(this, "heart_meds_recipe");

        registerRecipes();

        Bukkit.getPluginManager().registerEvents(this, this);

        for (Player p : Bukkit.getOnlinePlayers()) {
            discoverRecipesForPlayer(p);
        }

        getLogger().info("SBPCLifestealItems enabled.");
    }

    @Override
    public void onDisable() {
        for (BukkitTask task : activeTracking.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        activeTracking.clear();

        usageStore.saveAll();

        getLogger().info("SBPCLifestealItems disabled.");
    }

    private void loadSettings() {
        trackingCompassDurationSeconds = getConfig().getInt("settings.tracking-compass.duration-seconds", 15);
        trackingCompassSoundIntervalSeconds = getConfig().getInt("settings.tracking-compass.sound-interval-seconds", 3);
        armorPaddingUnitsPerItem = getConfig().getInt("settings.armor-padding.units-per-item", 2);
        armorPaddingMaxPerPiece = getConfig().getInt("settings.armor-padding.max-per-piece", 5);
        heartMedsHeartsRestoredHearts = getConfig().getInt("settings.heart-meds.hearts-restored", 1);

        // Load allowed armor materials from config for padding logic
        List<String> armorList = getConfig().getStringList("settings.armor-padding.allowed-armor");
        Set<Material> tmp = new HashSet<>();
        for (String name : armorList) {
            try {
                Material mat = Material.valueOf(name);
                tmp.add(mat);
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Invalid armor material in config: " + name);
            }
        }
        allowedArmorMaterials = Collections.unmodifiableSet(tmp);
    }

    // ------------------------------------------------------------------------
    // Item factories
    // ------------------------------------------------------------------------
    private ItemStack createTrailMixItem(int amount) {
        // Use DRIED_KELP as base so it uses the fast eating animation
        ItemStack stack = new ItemStack(Material.DRIED_KELP, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Trail Mix");
            meta.setLore(Arrays.asList(
                    "§7A quick snack for adventurers.",
                    "§7Restores §e2 hunger §7and §e1 saturation."
            ));
            meta.getPersistentDataContainer().set(trailMixKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack createTrackingCompass() {
        ItemStack stack = new ItemStack(Material.COMPASS, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = messages.get("messages.tracking-compass.name");
            if (name == null || name.isEmpty()) {
                name = "§bTracking Compass";
            }

            List<String> lore = messages.getList("messages.tracking-compass.lore");
            if (lore == null || lore.isEmpty()) {
                lore = Arrays.asList(
                        "§7A special compass that tracks",
                        "§7nearby players when activated."
                );
            }

            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(trackingCompassKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack createArmorPaddingItem(int amount) {
        ItemStack stack = new ItemStack(Material.RABBIT_HIDE, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = messages.get("messages.armor-padding.name");
            if (name == null || name.isEmpty()) {
                name = "§eArmor Padding";
            }

            List<String> lore = messages.getList("messages.armor-padding.lore");
            if (lore == null || lore.isEmpty()) {
                lore = Arrays.asList(
                        "§7Apply to armor to gain",
                        "§7extra padding (extra health)."
                );
            }

            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(armorPaddingKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }


    private ItemStack createEnchantedBranch() {
        ItemStack stack = new ItemStack(Material.STICK, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = messages.get("messages.enchanted-branch.name");
            if (name == null || name.isEmpty()) {
                name = "§9Enchanted Branch";
            }

            List<String> lore = messages.getList("messages.enchanted-branch.lore");
            if (lore == null || lore.isEmpty()) {
                lore = Arrays.asList(
                        "§7Swap with another item",
                        "§7to apply a random enchantment."
                );
            }

            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(enchantedBranchKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack createHeartMeds() {
        ItemStack stack = new ItemStack(Material.RABBIT_FOOT, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = messages.get("messages.heart-meds.name");
            if (name == null || name.isEmpty()) {
                name = "§dHeart Meds";
            }

            List<String> lore = messages.getList("messages.heart-meds.lore");
            if (lore == null || lore.isEmpty()) {
                lore = Arrays.asList(
                        "§7Consume to restore §c+1 heart§7",
                        "§7of max health (once per player)."
                );
            }

            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(heartMedsKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }


    // ------------------------------------------------------------------------
    // PDC helpers
    // ------------------------------------------------------------------------

    private boolean hasKey(ItemStack stack, NamespacedKey key) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        Byte flag = meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return flag != null && flag == (byte) 1;
    }

    private boolean isTrackingCompass(ItemStack stack) {
        return hasKey(stack, trackingCompassKey);
    }

    private boolean isArmorPaddingItem(ItemStack stack) {
        return hasKey(stack, armorPaddingKey);
    }

    private boolean isEnchantedBranch(ItemStack stack) {
        return hasKey(stack, enchantedBranchKey);
    }

    private boolean isHeartMeds(ItemStack stack) {
        return hasKey(stack, heartMedsKey);
    }
    private boolean isTrailMix(ItemStack stack) {
        if (stack == null || stack.getType() != Material.DRIED_KELP) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(trailMixKey, PersistentDataType.BYTE);
    }


    private boolean isArmorMaterial(Material type) {
        return allowedArmorMaterials.contains(type);
    }

    private boolean isPaddedArmor(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        if (!isArmorMaterial(stack.getType())) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        Integer lvl = meta.getPersistentDataContainer().get(armorPaddingLevelKey, PersistentDataType.INTEGER);
        return lvl != null && lvl > 0;
    }

    private int getPaddingLevel(ItemStack armor) {
        if (!isPaddedArmor(armor)) return 0;
        ItemMeta meta = armor.getItemMeta();
        if (meta == null) return 0;
        Integer lvl = meta.getPersistentDataContainer().get(armorPaddingLevelKey, PersistentDataType.INTEGER);
        return (lvl == null) ? 0 : lvl;
    }

    private void setPaddingLevel(ItemStack armor, int level) {
        level = Math.max(0, Math.min(armorPaddingMaxPerPiece, level));
        ItemMeta meta = armor.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (level <= 0) {
            pdc.remove(armorPaddingLevelKey);
            meta.setLore(null);
        } else {
            pdc.set(armorPaddingLevelKey, PersistentDataType.INTEGER, level);
            String line = messages.format("messages.armor-padding.lore-format",
                    Collections.singletonMap("amount", String.valueOf(level)));
            List<String> lore = new ArrayList<>();
            lore.add(line);
            meta.setLore(lore);
        }
        armor.setItemMeta(meta);
    }

    // ------------------------------------------------------------------------
    // Recipe registration + discovery
    // ------------------------------------------------------------------------

    private void registerRecipes() {
        // Tracking Compass: compass, apple, rotten flesh x2
        ItemStack compass = createTrackingCompass();
        ShapedRecipe trackingRecipe = new ShapedRecipe(recipeTrackingCompassKey, compass);
        trackingRecipe.shape("GAG", ".D.", "...");
        trackingRecipe.setIngredient('G', Material.ROTTEN_FLESH);
        trackingRecipe.setIngredient('A', Material.APPLE);
        trackingRecipe.setIngredient('D', Material.COMPASS);
        Bukkit.addRecipe(trackingRecipe);

        // Armor Padding: 1 wool, 1 stick, 2 iron ingots -> 3 padding
        ItemStack paddingItem = createArmorPaddingItem(3);
        ShapedRecipe paddingRecipe = new ShapedRecipe(recipeArmorPaddingKey, paddingItem);
        paddingRecipe.shape("WI ", " S ", " I ");
        paddingRecipe.setIngredient('W', Material.WHITE_WOOL);
        paddingRecipe.setIngredient('S', Material.STICK);
        paddingRecipe.setIngredient('I', Material.IRON_INGOT);
        Bukkit.addRecipe(paddingRecipe);

        // Enchanted Branch: 3 lapis, 1 stick, 1 diamond
        ItemStack branch = createEnchantedBranch();
        ShapedRecipe branchRecipe = new ShapedRecipe(recipeEnchantedBranchKey, branch);
        branchRecipe.shape("LLL", " S ", " D ");
        branchRecipe.setIngredient('L', Material.LAPIS_LAZULI);
        branchRecipe.setIngredient('S', Material.STICK);
        branchRecipe.setIngredient('D', Material.DIAMOND);
        Bukkit.addRecipe(branchRecipe);
     // Heart Meds: 1 diamond block, 1 golden apple, 2 ghast tears
        ItemStack meds = createHeartMeds();
        ShapedRecipe medsRecipe = new ShapedRecipe(recipeHeartMedsKey, meds);
        // Use spaces for empty slots and avoid AIR ingredients
        medsRecipe.shape("GTG", "B B", "GGG");
        medsRecipe.setIngredient('G', Material.GHAST_TEAR);
        medsRecipe.setIngredient('T', Material.GOLDEN_APPLE);
        medsRecipe.setIngredient('B', Material.DIAMOND_BLOCK);
        Bukkit.addRecipe(medsRecipe);

        ItemStack trailMix = createTrailMixItem(16);
        ShapelessRecipe trailMixRecipe = new ShapelessRecipe(recipeTrailMixKey, trailMix);
        trailMixRecipe.addIngredient(2, Material.WHEAT_SEEDS);
        trailMixRecipe.addIngredient(new RecipeChoice.MaterialChoice(
                Material.GLOW_BERRIES,
                Material.SWEET_BERRIES,
                Material.APPLE
        ));
        Bukkit.addRecipe(trailMixRecipe);

        ItemStack recoveryCompass = new ItemStack(Material.RECOVERY_COMPASS, 1);
        ShapelessRecipe recoveryRecipe = new ShapelessRecipe(recipeRecoveryCompassKey, recoveryCompass);
        recoveryRecipe.addIngredient(4, Material.IRON_NUGGET);
        recoveryRecipe.addIngredient(Material.INK_SAC);
        Bukkit.addRecipe(recoveryRecipe);

    }

    private void discoverRecipesForPlayer(Player player) {
        UUID id = player.getUniqueId();
        if (SbpcAPI.isCustomUnlocked(id, KEY_TRACKING_COMPASS)) {
            player.discoverRecipe(recipeTrackingCompassKey);
        }
        if (SbpcAPI.isCustomUnlocked(id, KEY_ARMOR_PADDING)) {
            player.discoverRecipe(recipeArmorPaddingKey);
        }
        if (SbpcAPI.isCustomUnlocked(id, KEY_ENCHANTED_BRANCH)) {
            player.discoverRecipe(recipeEnchantedBranchKey);
        }
        if (SbpcAPI.isCustomUnlocked(id, KEY_HEART_MEDS)) {
            player.discoverRecipe(recipeHeartMedsKey);
        }
    }

    @EventHandler
    public void onUnlockItem(UnlockItemEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        ProgressEntry entry = event.getEntry();
        if (entry == null) return;
        if (entry.getKind() != EntryKind.CUSTOM_ITEM) return;

        String customKey = entry.getCustomKey();
        if (customKey == null) return;

        if (KEY_TRACKING_COMPASS.equalsIgnoreCase(customKey) ||
            KEY_ARMOR_PADDING.equalsIgnoreCase(customKey) ||
            KEY_ENCHANTED_BRANCH.equalsIgnoreCase(customKey) ||
            KEY_HEART_MEDS.equalsIgnoreCase(customKey)) {
            discoverRecipesForPlayer(player);
        }
    }
    @EventHandler
    public void onTrailMixConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (!isTrailMix(item)) {
            return;
        }

        Player player = event.getPlayer();

        // Cancel vanilla food effect and apply our custom one
        event.setCancelled(true);

        // Manually consume one Trail Mix from the correct hand
        EquipmentSlot hand = event.getHand();
        if (hand == EquipmentSlot.HAND) {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (isTrailMix(inHand)) {
                int amt = inHand.getAmount();
                if (amt <= 1) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    inHand.setAmount(amt - 1);
                }
            }
        } else if (hand == EquipmentSlot.OFF_HAND) {
            ItemStack inOffHand = player.getInventory().getItemInOffHand();
            if (isTrailMix(inOffHand)) {
                int amt = inOffHand.getAmount();
                if (amt <= 1) {
                    player.getInventory().setItemInOffHand(null);
                } else {
                    inOffHand.setAmount(amt - 1);
                }
            }
        }

        // Apply 2 hunger and 1 saturation (clamped)
        int currentFood = player.getFoodLevel();
        int newFood = Math.min(20, currentFood + 2); // +2 "hunger" (food points)
        player.setFoodLevel(newFood);

        float currentSat = player.getSaturation();
        // Saturation cannot exceed current food level; clamp appropriately
        float maxSat = newFood;
        float newSat = Math.min(maxSat, currentSat + 1.0f);
        player.setSaturation(newSat);

        // Play eat sound so it feels like normal food
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        discoverRecipesForPlayer(event.getPlayer());
    }

    // ------------------------------------------------------------------------
    // Crafting restrictions + Armor Padding recipe
    // ------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();
        HumanEntity viewer = event.getViewers().isEmpty() ? null : event.getViewers().get(0);
        if (!(viewer instanceof Player player)) return;

        UUID uuid = player.getUniqueId();

        if (handleArmorPaddingApply(inv, player)) {
            return;
        }

        // Ensure custom items can't be crafted if not unlocked
        if (result != null && result.getType() != Material.AIR) {
            if (isTrackingCompass(result) && !SbpcAPI.isCustomUnlocked(uuid, KEY_TRACKING_COMPASS)) {
                inv.setResult(null);
                return;
            }
            if (isArmorPaddingItem(result) && !SbpcAPI.isCustomUnlocked(uuid, KEY_ARMOR_PADDING)) {
                inv.setResult(null);
                return;
            }
            if (isEnchantedBranch(result) && !SbpcAPI.isCustomUnlocked(uuid, KEY_ENCHANTED_BRANCH)) {
                inv.setResult(null);
                return;
            }
            if (isHeartMeds(result) && !SbpcAPI.isCustomUnlocked(uuid, KEY_HEART_MEDS)) {
                inv.setResult(null);
                return;
            }
        }

        // Custom items must not be used as ingredients for unintended recipes
        boolean hasCustomIngredient = false;
        for (ItemStack stack : inv.getMatrix()) {
            if (isTrackingCompass(stack) ||
                isArmorPaddingItem(stack) ||
                isEnchantedBranch(stack) ||
                isHeartMeds(stack)) {
                hasCustomIngredient = true;
                break;
            }
        }

        if (hasCustomIngredient &&
            (result == null || result.getType() == Material.AIR ||
             (!isTrackingCompass(result) &&
              !isArmorPaddingItem(result) &&
              !isEnchantedBranch(result) &&
              !isHeartMeds(result)))) {
            inv.setResult(null);
        }
    }

    /**
     * Armor + exactly 1 Armor Padding item => padded armor.
     * Uses config-defined allowed armor materials.
     */
    private boolean handleArmorPaddingApply(CraftingInventory inv, Player player) {
        ItemStack[] matrix = inv.getMatrix();

        ItemStack armor = null;
        int paddingItems = 0;

        for (ItemStack s : matrix) {
            if (s == null || s.getType() == Material.AIR) continue;

            if (isArmorPaddingItem(s)) {
                paddingItems += s.getAmount();
                continue;
            }

            if (isArmorMaterial(s.getType())) {
                if (armor == null) {
                    armor = s;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        if (armor == null || paddingItems == 0) return false;

        // Require exactly 1 Armor Padding item (fixes "doubling" behavior)
        if (paddingItems != 1) {
            return false;
        }

        if (!SbpcAPI.isCustomUnlocked(player.getUniqueId(), KEY_ARMOR_PADDING)) {
            inv.setResult(null);
            return true;
        }

        ItemStack result = armor.clone();
        int current = getPaddingLevel(result);
        if (current >= armorPaddingMaxPerPiece) {
            inv.setResult(null);
            return true;
        }

        int newLevel = current + armorPaddingUnitsPerItem;
        if (newLevel > armorPaddingMaxPerPiece) newLevel = armorPaddingMaxPerPiece;
        setPaddingLevel(result, newLevel);
        result.setAmount(1);
        inv.setResult(result);

        PlayerUsage usage = usageStore.getOrCreate(player.getUniqueId());
        usage.incrementArmorPaddingUses();

        return true;
    }

    // ------------------------------------------------------------------------
    // Tracking Compass
    // ------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onTrackingCompassUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!isTrackingCompass(item)) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!SbpcAPI.isCustomUnlocked(uuid, KEY_TRACKING_COMPASS)) {
            player.sendMessage(messages.get("messages.tracking-compass.not-unlocked"));
            event.setCancelled(true);
            return;
        }

        if (activeTracking.containsKey(uuid)) {
            player.sendMessage(messages.get("messages.tracking-compass.already-active"));
            event.setCancelled(true);
            return;
        }

        List<Player> candidates = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(player))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            player.sendMessage(messages.get("messages.tracking-compass.no-targets"));
            event.setCancelled(true);
            return;
        }

        Player target = candidates.get(random.nextInt(candidates.size()));
        player.sendMessage(messages.format(
                "messages.tracking-compass.locked-onto",
                Collections.singletonMap("player", target.getName())
        ));

        PlayerUsage usage = usageStore.getOrCreate(uuid);
        usage.incrementTrackingCompassUses();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelSelf();
                    return;
                }
                if (!target.isOnline()) {
                    player.sendMessage(messages.get("messages.tracking-compass.target-left"));
                    cancelSelf();
                    removeOneTrackingCompass(player);
                    return;
                }

                player.setCompassTarget(target.getLocation());

                if (ticks % (trackingCompassSoundIntervalSeconds * 20) == 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 1.0f);
                }

                ticks += 20;
                if (ticks >= trackingCompassDurationSeconds * 20) {
                    player.sendMessage(messages.get("messages.tracking-compass.shattered"));
                    removeOneTrackingCompass(player);
                    cancelSelf();
                }
            }

            private void cancelSelf() {
                BukkitTask t = activeTracking.remove(uuid);
                if (t != null) {
                    t.cancel();
                }
            }
        }, 0L, 20L);

        activeTracking.put(uuid, task);
        event.setCancelled(true);
    }

    private void removeOneTrackingCompass(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isTrackingCompass(s)) {
                if (s.getAmount() <= 1) {
                    inv.setItem(i, null);
                } else {
                    s.setAmount(s.getAmount() - 1);
                    inv.setItem(i, s);
                }
                break;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Armor Padding damage reduction
    // ------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDamageWithPadding(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        double damage = event.getDamage();
        if (damage <= 0) return;

        ItemStack[] armor = player.getInventory().getArmorContents();
        int[] padding = new int[armor.length];
        int totalPadding = 0;

        for (int i = 0; i < armor.length; i++) {
            ItemStack piece = armor[i];
            if (piece != null && isPaddedArmor(piece)) {
                int lvl = getPaddingLevel(piece);
                padding[i] = lvl;
                totalPadding += lvl;
            }
        }

        if (totalPadding <= 0) return;

        int paddingToConsume = (int) Math.min(totalPadding, Math.ceil(damage));
        double newDamage = damage - totalPadding;
        if (newDamage < 0) newDamage = 0;
        event.setDamage(newDamage);

        int remainingToConsume = paddingToConsume;
        for (int i = 0; i < armor.length && remainingToConsume > 0; i++) {
            if (padding[i] <= 0 || armor[i] == null) continue;
            int take = Math.min(padding[i], remainingToConsume);
            int newLevel = padding[i] - take;
            setPaddingLevel(armor[i], newLevel);
            remainingToConsume -= take;
        }

        player.getInventory().setArmorContents(armor);

        boolean anyPaddingLeft = false;
        for (ItemStack piece : armor) {
            if (piece != null && getPaddingLevel(piece) > 0) {
                anyPaddingLeft = true;
                break;
            }
        }
        if (!anyPaddingLeft) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }
    }

    // ------------------------------------------------------------------------
    // Enchanted Branch
    // ------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        boolean cursorBranch = isEnchantedBranch(cursor);
        boolean slotBranch = isEnchantedBranch(current);

        if (!cursorBranch && !slotBranch) return;

        if (!SbpcAPI.isCustomUnlocked(uuid, KEY_ENCHANTED_BRANCH)) {
            player.sendMessage(messages.get("messages.enchanted-branch.not-unlocked"));
            return;
        }

        ItemStack target;
        ItemStack branchStack;
        boolean branchOnCursor;

        if (cursorBranch && current != null && current.getType() != Material.AIR) {
            target = current;
            branchStack = cursor;
            branchOnCursor = true;
        } else if (slotBranch && cursor != null && cursor.getType() != Material.AIR) {
            target = cursor;
            branchStack = current;
            branchOnCursor = false;
        } else {
            return;
        }

        if (applyRandomEnchantFromPool(player, target)) {
            PlayerUsage usage = usageStore.getOrCreate(uuid);
            usage.incrementEnchantedBranchUses();

            int amount = branchStack.getAmount();
            if (amount <= 1) {
                if (branchOnCursor) {
                    event.setCursor(null);
                } else {
                    event.setCurrentItem(null);
                }
            } else {
                branchStack.setAmount(amount - 1);
                if (branchOnCursor) {
                    event.setCursor(branchStack);
                } else {
                    event.setCurrentItem(branchStack);
                }
            }
        } else {
            player.sendMessage(messages.get("messages.enchanted-branch.no-valid-enchant"));
        }
    }

    private boolean applyRandomEnchantFromPool(Player player, ItemStack target) {
        List<Enchantment> pool = EnchantPools.getPool(target.getType());
        if (pool.isEmpty()) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        PlayerProgress prog = SbpcAPI.getProgress(uuid);

        for (int attempts = 0; attempts < pool.size(); attempts++) {
            Enchantment ench = pool.get(random.nextInt(pool.size()));
            int currentLevel = target.getEnchantmentLevel(ench);

            int newLevel;
            if (currentLevel <= 0) {
                if (!prog.isEnchantUnlocked(ench, 1)) continue;
                newLevel = 1;
            } else if (currentLevel == 1) {
                if (!prog.isEnchantUnlocked(ench, 2)) continue;
                newLevel = 2;
            } else {
                continue;
            }

            target.addUnsafeEnchantment(ench, newLevel);

            String enchantName = ench.getKey().getKey() + " " + newLevel;
            String msg = messages.format(
                    "messages.enchanted-branch.apply-success",
                    Collections.singletonMap("enchant", enchantName)
            );
            player.sendMessage(msg);
            return true;
        }

        return false;
    }

    // ------------------------------------------------------------------------
    // Heart Meds
    // ------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onHeartMedsUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!isHeartMeds(item)) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!SbpcAPI.isCustomUnlocked(uuid, KEY_HEART_MEDS)) {
            player.sendMessage(messages.get("messages.heart-meds.not-unlocked"));
            event.setCancelled(true);
            return;
        }

        PlayerUsage usage = usageStore.getOrCreate(uuid);
        if (usage.getHeartMedsUses() >= 1) {
            player.sendMessage(messages.get("messages.heart-meds.already-used"));
            event.setCancelled(true);
            return;
        }

        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            event.setCancelled(true);
            return;
        }

        double base = attr.getBaseValue();
        if (base >= 20.0) {
            player.sendMessage(messages.get("messages.heart-meds.not-missing"));
            event.setCancelled(true);
            return;
        }

        double newVal = base + (heartMedsHeartsRestoredHearts * 2.0);
        if (newVal > 20.0) newVal = 20.0;
        attr.setBaseValue(newVal);

        usage.incrementHeartMedsUses();

        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }

        player.sendMessage(messages.get("messages.heart-meds.consumed"));
        event.setCancelled(true);
    }
}
