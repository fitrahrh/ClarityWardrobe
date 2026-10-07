package com.harukiyu.claritywardrobe.gui;

import com.harukiyu.claritywardrobe.ClarityWardrobe;
import com.harukiyu.claritywardrobe.api.WardrobeSlotType;
import com.harukiyu.claritywardrobe.config.MenuConfig;
import com.harukiyu.claritywardrobe.cosmetics.ValidationResult;
import com.harukiyu.claritywardrobe.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all player interactions in the wardrobe GUI with strict anti-duplication
 * measures and Custom Model Data validation.
 */
public class WardrobeMenuListener implements Listener {

    private final ClarityWardrobe plugin;

    public WardrobeMenuListener(ClarityWardrobe plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof WardrobeMenu) {
            int topSize = event.getView().getTopInventory().getSize();
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WardrobeMenu menu)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ClickType clickType = event.getClick();
        MenuConfig menuConfig = plugin.getConfigManager().getMenuConfig();

        // Disallow dangerous actions that can desync or duplicate items
        if (clickType == ClickType.NUMBER_KEY || clickType == ClickType.SWAP_OFFHAND || clickType == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        // ---------------------------------------------------------------------
        // Case 1: Shift-clicking from player's bottom inventory
        // ---------------------------------------------------------------------
        if (event.isShiftClick() && rawSlot >= topSize) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType().isAir()) {
                return;
            }

            // Check if helmet slot is empty and item is valid helmet
            if (!plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.HELMET)) {
                ValidationResult result = plugin.getCosmeticManager().getValidator().validate(WardrobeSlotType.HELMET, clickedItem);
                if (result.isValid()) {
                    handleShiftEquip(player, menu, WardrobeSlotType.HELMET, clickedItem);
                    return;
                }
            }

            // Check if backpack slot is empty and item is valid backpack
            if (!plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.BACKPACK)) {
                ValidationResult result = plugin.getCosmeticManager().getValidator().validate(WardrobeSlotType.BACKPACK, clickedItem);
                if (result.isValid()) {
                    handleShiftEquip(player, menu, WardrobeSlotType.BACKPACK, clickedItem);
                    return;
                }
            }
            return;
        }

        // ---------------------------------------------------------------------
        // Case 2: Clicking inside player's bottom inventory normally
        // ---------------------------------------------------------------------
        if (rawSlot >= topSize) {
            // Allow normal inventory interaction in player's own inventory
            return;
        }

        // ---------------------------------------------------------------------
        // Case 3: Clicking inside the wardrobe GUI
        // ---------------------------------------------------------------------
        event.setCancelled(true); // Always cancel top inventory clicks

        int slot = event.getSlot();

        // Close button
        if (menuConfig.isCloseButton(slot)) {
            ComponentUtil.playSound(player, plugin.getConfigManager().getSoundSection("menu-click"));
            player.closeInventory();
            return;
        }

        // Unequip all button
        if (menuConfig.isUnequipAllButton(slot)) {
            List<ItemStack> removed = plugin.getCosmeticManager().unequipAll(player);
            if (removed.isEmpty()) {
                plugin.getConfigManager().getLanguageManager().sendMessage(player, "no-cosmetics-equipped");
                ComponentUtil.playSound(player, plugin.getConfigManager().getSoundSection("error"));
            } else {
                for (ItemStack item : removed) {
                    giveOrDrop(player, item);
                }
                menu.updateCosmeticSlot(WardrobeSlotType.HELMET);
                menu.updateCosmeticSlot(WardrobeSlotType.BACKPACK);
                ComponentUtil.playSound(player, plugin.getConfigManager().getSoundSection("unequip"));
                plugin.getConfigManager().getLanguageManager().sendMessage(player, "all-unequipped");
            }
            return;
        }

        // Info button
        if (menuConfig.getInfoButton() != null && menuConfig.getInfoButton().getSlot() == slot) {
            ComponentUtil.playSound(player, plugin.getConfigManager().getSoundSection("menu-click"));
            return;
        }

        // Cosmetic slots
        if (menuConfig.isCosmeticSlot(slot)) {
            WardrobeSlotType slotType = menuConfig.getSlotType(slot);
            handleCosmeticSlotClick(player, menu, slotType, event);
        }
    }

    /**
     * Handles equipping, unequipping, or swapping items in a cosmetic slot.
     */
    private void handleCosmeticSlotClick(Player player, WardrobeMenu menu, WardrobeSlotType slotType, InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        boolean hasEquipped = plugin.getCosmeticManager().hasCosmetic(player, slotType);

        // Sub-case A: Cursor is empty -> Unequip cosmetic if equipped
        if (cursor.getType().isAir()) {
            if (hasEquipped) {
                ItemStack unequipped = plugin.getCosmeticManager().unequipCosmetic(player, slotType);
                if (unequipped != null) {
                    event.getView().setCursor(unequipped);
                    menu.updateCosmeticSlot(slotType);
                    ComponentUtil.playSound(player, plugin.getConfigManager().getSoundSection("unequip"));
                    String msgKey = (slotType == WardrobeSlotType.HELMET) ? "helmet-unequipped" : "backpack-unequipped";
                    plugin.getConfigManager().getLanguageManager().sendMessage(player, msgKey);
                }
            } else {
                ComponentUtil.playSound(player, plugin.getConfigManager().getSoundSection("menu-click"));
            }
            return;
        }

        // Sub-case B: Cursor has an item -> Validate and equip
        ValidationResult validation = plugin.getCosmeticManager().getValidator().validate(slotType, cursor);
        if (!validation.isValid()) {
            handleValidationError(player, validation, cursor);
            return;
        }

        // Valid cosmetic item!
        ItemStack toEquip = cursor.clone();
        toEquip.setAmount(1);

        ItemStack previous = hasEquipped ? plugin.getCosmeticManager().unequipCosmetic(player, slotType) : null;
        plugin.getCosmeticManager().equipCosmetic(player, slotType, toEquip);

        // Adjust cursor
        if (cursor.getAmount() > 1) {
            cursor.setAmount(cursor.getAmount() - 1);
            event.getView().setCursor(cursor);
            if (previous != null) {
                giveOrDrop(player, previous);
            }
        } else {
            event.getView().setCursor(previous);
        }

        menu.updateCosmeticSlot(slotType);
        ComponentUtil.playSound(player, plugin.getConfigManager().getSoundSection("equip"));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<item>", getItemDisplayName(toEquip));
        String msgKey = (slotType == WardrobeSlotType.HELMET) ? "helmet-equipped" : "backpack-equipped";
        plugin.getConfigManager().getLanguageManager().sendMessage(player, msgKey, placeholders);
    }

    /**
     * Handles shift-clicking an item into an empty cosmetic slot.
     */
    private void handleShiftEquip(Player player, WardrobeMenu menu, WardrobeSlotType slotType, ItemStack clickedItem) {
        ItemStack toEquip = clickedItem.clone();
        toEquip.setAmount(1);

        plugin.getCosmeticManager().equipCosmetic(player, slotType, toEquip);

        if (clickedItem.getAmount() > 1) {
            clickedItem.setAmount(clickedItem.getAmount() - 1);
        } else {
            clickedItem.setAmount(0);
        }

        menu.updateCosmeticSlot(slotType);
        ComponentUtil.playSound(player, plugin.getConfigManager().getSoundSection("equip"));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<item>", getItemDisplayName(toEquip));
        String msgKey = (slotType == WardrobeSlotType.HELMET) ? "helmet-equipped" : "backpack-equipped";
        plugin.getConfigManager().getLanguageManager().sendMessage(player, msgKey, placeholders);
    }

    /**
     * Handles validation failure messages and error sounds.
     */
    private void handleValidationError(Player player, ValidationResult result, ItemStack item) {
        ComponentUtil.playSound(player, plugin.getConfigManager().getSoundSection("error"));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<material>", item.getType().name());

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasCustomModelData()) {
            placeholders.put("<cmd>", String.valueOf(meta.getCustomModelData()));
        } else {
            placeholders.put("<cmd>", "None");
        }

        switch (result) {
            case MISSING_CUSTOM_MODEL_DATA -> plugin.getConfigManager().getLanguageManager().sendMessage(player, "error-no-cmd", placeholders);
            case UNAPPROVED_CUSTOM_MODEL_DATA -> plugin.getConfigManager().getLanguageManager().sendMessage(player, "error-unapproved-cmd", placeholders);
            case UNAPPROVED_MATERIAL -> plugin.getConfigManager().getLanguageManager().sendMessage(player, "error-unapproved-material", placeholders);
            default -> plugin.getConfigManager().getLanguageManager().sendMessage(player, "error-no-cmd", placeholders);
        }
    }

    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                Component comp = meta.displayName();
                if (comp != null) {
                    return ComponentUtil.serializeMiniMessage(comp);
                }
            }
        }
        return item.getType().name().replace('_', ' ').toLowerCase();
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            Location loc = player.getLocation();
            if (loc == null || loc.getWorld() == null) {
                return;
            }
            for (ItemStack drop : leftover.values()) {
                if (drop != null && !drop.getType().isAir()) {
                    loc.getWorld().dropItemNaturally(loc, drop);
                }
            }
            plugin.getConfigManager().getLanguageManager().sendMessage(player, "error-inventory-full");
        }
    }
}
