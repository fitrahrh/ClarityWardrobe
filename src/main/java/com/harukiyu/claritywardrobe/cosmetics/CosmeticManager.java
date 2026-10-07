package com.harukiyu.claritywardrobe.cosmetics;

import com.harukiyu.claritywardrobe.ClarityWardrobe;
import com.harukiyu.claritywardrobe.api.WardrobeSlotType;
import com.harukiyu.claritywardrobe.data.PlayerWardrobeData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Core manager for equipping, unequipping, and updating cosmetic visuals.
 */
public class CosmeticManager {

    private final ClarityWardrobe plugin;
    private final CosmeticValidator validator;
    private final BackpackDisplayManager displayManager;

    public CosmeticManager(ClarityWardrobe plugin) {
        this.plugin = plugin;
        this.validator = new CosmeticValidator(plugin);
        this.displayManager = new BackpackDisplayManager(plugin);
    }

    /**
     * Checks if a player has an active cosmetic equipped in the specified slot.
     */
    public boolean hasCosmetic(Player player, WardrobeSlotType slotType) {
        if (player == null) return false;
        PlayerWardrobeData data = plugin.getStorage().getPlayerData(player.getUniqueId());
        return data != null && data.hasCosmetic(slotType);
    }

    /**
     * Retrieves the cosmetic item equipped by the player in the specified slot.
     */
    public ItemStack getCosmetic(Player player, WardrobeSlotType slotType) {
        if (player == null) return null;
        PlayerWardrobeData data = plugin.getStorage().getPlayerData(player.getUniqueId());
        return data != null ? data.getCosmetic(slotType) : null;
    }

    /**
     * Equips a cosmetic item for the player.
     */
    public ValidationResult equipCosmetic(Player player, WardrobeSlotType slotType, ItemStack item) {
        ValidationResult result = validator.validate(slotType, item);
        if (!result.isValid()) {
            return result;
        }

        PlayerWardrobeData data = plugin.getStorage().getPlayerData(player.getUniqueId());
        data.setCosmetic(slotType, item);
        plugin.getStorage().savePlayerData(player.getUniqueId(), true);

        // Apply visual updates
        if (slotType == WardrobeSlotType.HELMET) {
            plugin.getProtocolLibHook().refreshEquipment(player);
        } else if (slotType == WardrobeSlotType.BACKPACK) {
            String mode = plugin.getConfigManager().getBackpackRenderer();
            if (mode.equalsIgnoreCase("ITEM_DISPLAY")) {
                displayManager.updateCosmetic(player, item);
            } else {
                plugin.getProtocolLibHook().refreshEquipment(player);
            }
        }

        return ValidationResult.SUCCESS;
    }

    /**
     * Unequips a cosmetic item from the player and returns the item.
     */
    public ItemStack unequipCosmetic(Player player, WardrobeSlotType slotType) {
        PlayerWardrobeData data = plugin.getStorage().getPlayerData(player.getUniqueId());
        ItemStack removed = data.removeCosmetic(slotType);
        plugin.getStorage().savePlayerData(player.getUniqueId(), true);

        // Restore visuals to vanilla
        if (slotType == WardrobeSlotType.HELMET) {
            plugin.getProtocolLibHook().refreshEquipment(player);
        } else if (slotType == WardrobeSlotType.BACKPACK) {
            String mode = plugin.getConfigManager().getBackpackRenderer();
            if (mode.equalsIgnoreCase("ITEM_DISPLAY")) {
                displayManager.removeDisplay(player);
            } else {
                plugin.getProtocolLibHook().refreshEquipment(player);
            }
        }

        return removed;
    }

    /**
     * Unequips all cosmetics for the player and returns a list of removed items.
     */
    public List<ItemStack> unequipAll(Player player) {
        List<ItemStack> removedList = new ArrayList<>();

        if (hasCosmetic(player, WardrobeSlotType.HELMET)) {
            ItemStack helmet = unequipCosmetic(player, WardrobeSlotType.HELMET);
            if (helmet != null && !helmet.getType().isAir()) {
                removedList.add(helmet);
            }
        }

        if (hasCosmetic(player, WardrobeSlotType.BACKPACK)) {
            ItemStack backpack = unequipCosmetic(player, WardrobeSlotType.BACKPACK);
            if (backpack != null && !backpack.getType().isAir()) {
                removedList.add(backpack);
            }
        }

        return removedList;
    }

    /**
     * Refreshes all cosmetic visuals for a player (called on join, respawn, world change, or armor change).
     */
    public void refreshVisuals(Player player) {
        if (player == null || !player.isOnline()) return;

        PlayerWardrobeData data = plugin.getStorage().getPlayerData(player.getUniqueId());
        if (data == null) return;

        // Refresh Helmet packet (overrides visual helmet directly on head)
        plugin.getProtocolLibHook().refreshEquipment(player);

        // Refresh Backpack
        if (data.hasCosmetic(WardrobeSlotType.BACKPACK)) {
            String mode = plugin.getConfigManager().getBackpackRenderer();
            if (mode.equalsIgnoreCase("ITEM_DISPLAY")) {
                displayManager.updateCosmetic(player, data.getCosmetic(WardrobeSlotType.BACKPACK));
            } else {
                plugin.getProtocolLibHook().refreshEquipment(player);
            }
        } else {
            displayManager.removeDisplay(player);
            plugin.getProtocolLibHook().refreshEquipment(player);
        }
    }

    /**
     * Removes active displays for the player.
     */
    public void removeDisplays(Player player) {
        if (player == null) return;
        displayManager.removeDisplay(player);
    }

    public CosmeticValidator getValidator() {
        return validator;
    }

    public BackpackDisplayManager getDisplayManager() {
        return displayManager;
    }

    public void shutdown() {
        displayManager.shutdown();
    }
}
