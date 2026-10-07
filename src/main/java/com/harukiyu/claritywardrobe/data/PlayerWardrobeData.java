package com.harukiyu.claritywardrobe.data;

import com.harukiyu.claritywardrobe.api.WardrobeSlotType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

/**
 * Stores the active equipped cosmetics for a single player.
 */
public class PlayerWardrobeData {

    private final UUID uuid;
    private ItemStack helmetCosmetic;
    private ItemStack backpackCosmetic;
    private volatile boolean dirty = false;

    public PlayerWardrobeData(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "UUID cannot be null");
    }

    public UUID getUuid() {
        return uuid;
    }

    public ItemStack getCosmetic(WardrobeSlotType slotType) {
        if (slotType == WardrobeSlotType.HELMET) {
            return helmetCosmetic != null ? helmetCosmetic.clone() : null;
        } else if (slotType == WardrobeSlotType.BACKPACK) {
            return backpackCosmetic != null ? backpackCosmetic.clone() : null;
        }
        return null;
    }

    public boolean hasCosmetic(WardrobeSlotType slotType) {
        ItemStack item = (slotType == WardrobeSlotType.HELMET) ? helmetCosmetic : backpackCosmetic;
        return item != null && !item.getType().isAir();
    }

    public void setCosmetic(WardrobeSlotType slotType, ItemStack item) {
        ItemStack toSet = (item != null && !item.getType().isAir()) ? item.clone() : null;
        if (slotType == WardrobeSlotType.HELMET) {
            this.helmetCosmetic = toSet;
        } else if (slotType == WardrobeSlotType.BACKPACK) {
            this.backpackCosmetic = toSet;
        }
        this.dirty = true;
    }

    public ItemStack removeCosmetic(WardrobeSlotType slotType) {
        ItemStack removed = getCosmetic(slotType);
        setCosmetic(slotType, null);
        return removed;
    }

    public boolean isEmpty() {
        return (helmetCosmetic == null || helmetCosmetic.getType().isAir())
                && (backpackCosmetic == null || backpackCosmetic.getType().isAir());
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * Serializes this data into a Bukkit ConfigurationSection.
     */
    public void serialize(ConfigurationSection section) {
        if (helmetCosmetic != null && !helmetCosmetic.getType().isAir()) {
            section.set("helmet", helmetCosmetic);
        } else {
            section.set("helmet", null);
        }

        if (backpackCosmetic != null && !backpackCosmetic.getType().isAir()) {
            section.set("backpack", backpackCosmetic);
        } else {
            section.set("backpack", null);
        }
    }

    /**
     * Deserializes player wardrobe data from a ConfigurationSection.
     */
    public static PlayerWardrobeData deserialize(UUID uuid, ConfigurationSection section) {
        PlayerWardrobeData data = new PlayerWardrobeData(uuid);
        if (section != null) {
            ItemStack helmet = section.getItemStack("helmet");
            if (helmet != null && !helmet.getType().isAir()) {
                data.helmetCosmetic = helmet;
            }
            ItemStack backpack = section.getItemStack("backpack");
            if (backpack != null && !backpack.getType().isAir()) {
                data.backpackCosmetic = backpack;
            }
        }
        data.dirty = false;
        return data;
    }
}
