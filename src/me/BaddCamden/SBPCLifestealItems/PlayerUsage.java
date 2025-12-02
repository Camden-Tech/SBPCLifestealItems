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

    public PlayerUsage(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getTrackingCompassUses() {
        return trackingCompassUses;
    }

    public void setTrackingCompassUses(int trackingCompassUses) {
        this.trackingCompassUses = trackingCompassUses;
    }

    public int getArmorPaddingUses() {
        return armorPaddingUses;
    }

    public void setArmorPaddingUses(int armorPaddingUses) {
        this.armorPaddingUses = armorPaddingUses;
    }

    public int getEnchantedBranchUses() {
        return enchantedBranchUses;
    }

    public void setEnchantedBranchUses(int enchantedBranchUses) {
        this.enchantedBranchUses = enchantedBranchUses;
    }

    public int getHeartMedsUses() {
        return heartMedsUses;
    }

    public void setHeartMedsUses(int heartMedsUses) {
        this.heartMedsUses = heartMedsUses;
    }

    public void incrementTrackingCompassUses() {
        trackingCompassUses++;
    }

    public void incrementArmorPaddingUses() {
        armorPaddingUses++;
    }

    public void incrementEnchantedBranchUses() {
        enchantedBranchUses++;
    }

    public void incrementHeartMedsUses() {
        heartMedsUses++;
    }
}
