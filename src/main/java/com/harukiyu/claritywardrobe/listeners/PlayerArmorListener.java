package com.harukiyu.claritywardrobe.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.harukiyu.claritywardrobe.ClarityWardrobe;
import com.harukiyu.claritywardrobe.api.WardrobeSlotType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Real-time armor listener that detects whenever a player equips, unequips,
 * or swaps real combat armor (via inventory, right-click, shift-click, or breaking).
 * Ensures that cosmetic hats, helmets, and backpacks remain continuously updated
 * and never get visually overwritten by real armor.
 */
public class PlayerArmorListener implements Listener {

    private final ClarityWardrobe plugin;

    public PlayerArmorListener(ClarityWardrobe plugin) {
        this.plugin = plugin;
    }

    /**
     * Triggered by Paper whenever any armor slot changes (covers inventory click,
     * right-click in hand, shift-click, dispenser, breaking, etc.).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        if (event.getSlotType() == PlayerArmorChangeEvent.SlotType.HEAD) {
            Player player = event.getPlayer();
            if (plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.HELMET)) {
                scheduleRefresh(player);
            }
        } else if (event.getSlotType() == PlayerArmorChangeEvent.SlotType.CHEST) {
            Player player = event.getPlayer();
            if (plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.BACKPACK)) {
                scheduleRefresh(player);
            }
        }
    }

    /**
     * Catches right-clicking armor from main hand or off hand.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null) {
                Player player = event.getPlayer();
                if (isWearableHelmet(item.getType()) && plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.HELMET)) {
                    scheduleRefresh(player);
                } else if (isWearableChestplate(item.getType()) && plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.BACKPACK)) {
                    scheduleRefresh(player);
                }
            }
        }
    }

    /**
     * Catches manual clicks inside the player's survival inventory (pressing 'E').
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        boolean hasHelmet = plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.HELMET);
        boolean hasBackpack = plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.BACKPACK);

        if (!hasHelmet && !hasBackpack) {
            return;
        }

        // If clicking armor slot, shift-clicking into armor, or clicking raw slots 5/6
        if (event.getSlotType() == InventoryType.SlotType.ARMOR || event.isShiftClick() || event.getRawSlot() == 5 || event.getRawSlot() == 6) {
            scheduleRefresh(player);
        }
    }

    /**
     * Catches dragging items across armor slots in survival inventory.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        boolean hasHelmet = plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.HELMET);
        boolean hasBackpack = plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.BACKPACK);

        if (!hasHelmet && !hasBackpack) {
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            if ((rawSlot == 5 && hasHelmet) || (rawSlot == 6 && hasBackpack)) {
                scheduleRefresh(player);
                break;
            }
        }
    }

    /**
     * Catches when helmet or armor breaks due to durability damage.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent event) {
        ItemStack broken = event.getBrokenItem();
        Player player = event.getPlayer();
        if (isWearableHelmet(broken.getType()) && plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.HELMET)) {
            scheduleRefresh(player);
        } else if (isWearableChestplate(broken.getType()) && plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.BACKPACK)) {
            scheduleRefresh(player);
        }
    }

    /**
     * Catches when player closes their inventory after changing armor items.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.HELMET)
                    || plugin.getCosmeticManager().hasCosmetic(player, WardrobeSlotType.BACKPACK)) {
                scheduleRefresh(player);
            }
        }
    }

    /**
     * Schedules a visual refresh on the next tick so the server's vanilla
     * equipment change completes first, then our cosmetic visual overrides it immediately.
     */
    private void scheduleRefresh(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getCosmeticManager().refreshVisuals(player);
            }
        }, 1L);
    }

    /**
     * Checks if a material is a wearable head/helmet item in Minecraft.
     */
    private boolean isWearableHelmet(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET")
                || name.equals("CARVED_PUMPKIN")
                || name.endsWith("_HEAD")
                || name.endsWith("_SKULL")
                || name.equals("TURTLE_HELMET");
    }

    /**
     * Checks if a material is a wearable chestplate or elytra item in Minecraft.
     */
    private boolean isWearableChestplate(Material material) {
        String name = material.name();
        return name.endsWith("_CHESTPLATE") || name.equals("ELYTRA");
    }
}
