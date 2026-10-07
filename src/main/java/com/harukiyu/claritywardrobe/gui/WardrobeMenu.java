package com.harukiyu.claritywardrobe.gui;

import com.harukiyu.claritywardrobe.ClarityWardrobe;
import com.harukiyu.claritywardrobe.api.WardrobeSlotType;
import com.harukiyu.claritywardrobe.config.MenuConfig;
import com.harukiyu.claritywardrobe.config.MenuItemData;
import com.harukiyu.claritywardrobe.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic wardrobe inventory GUI loaded from wardrobe-menu.yml.
 */
public class WardrobeMenu implements InventoryHolder {

    private final ClarityWardrobe plugin;
    private final Player player;
    private Inventory inventory;

    public WardrobeMenu(ClarityWardrobe plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    private void ensureInventory() {
        if (this.inventory == null) {
            MenuConfig config = plugin.getConfigManager().getMenuConfig();
            Component titleComponent = ComponentUtil.parse(config.getTitle());
            this.inventory = Bukkit.createInventory(this, config.getInventorySize(), titleComponent);
        }
    }

    /**
     * Builds and populates the wardrobe inventory according to wardrobe-menu.yml.
     */
    public final void build() {
        ensureInventory();
        MenuConfig config = plugin.getConfigManager().getMenuConfig();
        inventory.clear();

        // 1. Populate Decorations
        for (MenuItemData deco : config.getDecorations()) {
            if (isValidSlot(deco.getSlot())) {
                inventory.setItem(deco.getSlot(), deco.build());
            }
        }

        // 2. Populate Action Buttons
        if (config.getUnequipAllButton() != null && isValidSlot(config.getUnequipAllButton().getSlot())) {
            inventory.setItem(config.getUnequipAllButton().getSlot(), config.getUnequipAllButton().build());
        }
        if (config.getCloseButton() != null && isValidSlot(config.getCloseButton().getSlot())) {
            inventory.setItem(config.getCloseButton().getSlot(), config.getCloseButton().build());
        }
        if (config.getInfoButton() != null && isValidSlot(config.getInfoButton().getSlot())) {
            inventory.setItem(config.getInfoButton().getSlot(), config.getInfoButton().build());
        }

        // 3. Populate Cosmetic Slots
        updateCosmeticSlot(WardrobeSlotType.HELMET);
        updateCosmeticSlot(WardrobeSlotType.BACKPACK);

        // 4. Fill empty slots with background item
        if (config.isBackgroundEnabled() && config.isBackgroundFillEmpty() && config.getBackgroundItem() != null) {
            ItemStack bgItem = config.getBackgroundItem().build();
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack current = inventory.getItem(i);
                if (current == null || current.getType().isAir()) {
                    inventory.setItem(i, bgItem.clone());
                }
            }
        }
    }

    /**
     * Updates a single cosmetic slot without recreating the inventory.
     */
    public void updateCosmeticSlot(WardrobeSlotType slotType) {
        ensureInventory();
        MenuConfig config = plugin.getConfigManager().getMenuConfig();
        int slot = (slotType == WardrobeSlotType.HELMET) ? config.getHelmetSlot() : config.getBackpackSlot();
        if (!isValidSlot(slot)) return;

        ItemStack equipped = plugin.getCosmeticManager().getCosmetic(player, slotType);

        if (equipped != null && !equipped.getType().isAir()) {
            // Item is equipped: show it with status lore overlay
            ItemStack displayItem = equipped.clone();
            List<String> appendLore = (slotType == WardrobeSlotType.HELMET)
                    ? config.getHelmetEquippedLoreAppend()
                    : config.getBackpackEquippedLoreAppend();

            if (appendLore != null && !appendLore.isEmpty()) {
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    List<Component> currentLore = (meta.hasLore() && meta.lore() != null)
                            ? new ArrayList<>(meta.lore())
                            : new ArrayList<>();
                    currentLore.addAll(ComponentUtil.parseItemLore(appendLore));
                    meta.lore(currentLore);
                    displayItem.setItemMeta(meta);
                }
            }
            inventory.setItem(slot, displayItem);
        } else {
            // No item equipped: show placeholder
            MenuItemData placeholder = (slotType == WardrobeSlotType.HELMET)
                    ? config.getHelmetPlaceholder()
                    : config.getBackpackPlaceholder();
            inventory.setItem(slot, placeholder.build());
        }
    }

    private boolean isValidSlot(int slot) {
        ensureInventory();
        return slot >= 0 && slot < inventory.getSize();
    }

    public void open() {
        ensureInventory();
        build();
        player.openInventory(inventory);
        ComponentUtil.playSound(player, plugin.getConfigManager().getSoundSection("menu-open"));
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public Inventory getInventory() {
        ensureInventory();
        return inventory;
    }
}
