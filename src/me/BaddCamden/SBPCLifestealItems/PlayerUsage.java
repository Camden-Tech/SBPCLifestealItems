package me.BaddCamden.SBPCLifestealItems;

import java.util.UUID;

/**
 * In-memory record of custom item usage for a single player.
 */
public class PlayerUsage {

    private final UUID uuid;
    private int trackingCompassUses;
    private int armorPaddingUses;
    private int enchantedBranchUses;
    private int heartMedsUses;

    /**
     * Constructs a usage tracker for a specific player UUID.
     *
     * @param uuid player identifier to associate with this record
     */
    public PlayerUsage(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * @return unique identifier of the player these stats belong to
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * @return number of times the player has used a tracking compass
     */
    public int getTrackingCompassUses() {
        return trackingCompassUses;
    }

    /**
     * Sets the stored tracking compass usage count, typically from disk.
     */
    public void setTrackingCompassUses(int trackingCompassUses) {
        this.trackingCompassUses = trackingCompassUses;
    }

    /**
     * @return number of armor padding items the player has consumed
     */
    public int getArmorPaddingUses() {
        return armorPaddingUses;
    }

    /**
     * Sets the stored armor padding usage count, typically from disk.
     */
    public void setArmorPaddingUses(int armorPaddingUses) {
        this.armorPaddingUses = armorPaddingUses;
    }

    /**
     * @return number of enchanted branches the player has expended
     */
    public int getEnchantedBranchUses() {
        return enchantedBranchUses;
    }

    /**
     * Sets the stored enchanted branch usage count, typically from disk.
     */
    public void setEnchantedBranchUses(int enchantedBranchUses) {
        this.enchantedBranchUses = enchantedBranchUses;
    }

    /**
     * @return number of heart meds used by the player
     */
    public int getHeartMedsUses() {
        return heartMedsUses;
    }

    /**
     * Sets the stored heart meds usage count, typically from disk.
     */
    public void setHeartMedsUses(int heartMedsUses) {
        this.heartMedsUses = heartMedsUses;
    }

    /**
     * Increments tracking compass usage by one.
     */
    public void incrementTrackingCompassUses() {
        trackingCompassUses++;
    }

    /**
     * Increments armor padding usage by one.
     */
    public void incrementArmorPaddingUses() {
        armorPaddingUses++;
    }

    /**
     * Increments enchanted branch usage by one.
     */
    public void incrementEnchantedBranchUses() {
        enchantedBranchUses++;
    }

    /**
     * Increments heart meds usage by one.
     */
    public void incrementHeartMedsUses() {
        heartMedsUses++;
    }
}
