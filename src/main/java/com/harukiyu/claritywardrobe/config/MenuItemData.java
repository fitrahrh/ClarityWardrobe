package com.harukiyu.claritywardrobe.config;

import com.harukiyu.claritywardrobe.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a configurable menu item with full support for Custom Model Data,
 * Adventure MiniMessage components, and item flags.
 */
public class MenuItemData {

    private final int slot;
    private final Material material;
    private final int customModelData;
    private final String name;
    private final List<String> lore;

    public MenuItemData(int slot, Material material, int customModelData, String name, List<String> lore) {
        this.slot = slot;
        this.material = material != null ? material : Material.STONE;
        this.customModelData = customModelData;
        this.name = name;
        this.lore = lore != null ? lore : Collections.emptyList();
    }

    public static MenuItemData fromSection(ConfigurationSection section, int defaultSlot) {
        if (section == null) {
            return new MenuItemData(defaultSlot, Material.BARRIER, 0, "<red>Missing Item</red>", Collections.emptyList());
        }

        int slot = section.getInt("slot", defaultSlot);
        String matName = section.getString("material");
        Material mat = null;
        if (matName != null && !matName.isEmpty()) {
            mat = Material.matchMaterial(matName.toUpperCase());
        }
        if (mat == null) {
            mat = Material.STONE;
        }

        int cmd = section.getInt("custom-model-data", 0);
        String name = section.getString("name", "");
        List<String> lore = section.getStringList("lore");

        return new MenuItemData(slot, mat, cmd, name, lore);
    }

    /**
     * Builds an ItemStack from this menu item configuration.
     * Item names and lores are formatted with non-italic MiniMessage.
     */
    public ItemStack build() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.displayName(ComponentUtil.parseItemText(name));
            }
            if (!lore.isEmpty()) {
                meta.lore(ComponentUtil.parseItemLore(lore));
            }
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            // Clean item appearance for GUI
            meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Builds an ItemStack appending extra lore lines.
     */
    public ItemStack buildWithAppendedLore(List<String> extraLore) {
        ItemStack item = build();
        if (extraLore == null || extraLore.isEmpty()) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> currentLore = (meta.hasLore() && meta.lore() != null)
                    ? new ArrayList<>(meta.lore())
                    : new ArrayList<>();
            currentLore.addAll(ComponentUtil.parseItemLore(extraLore));
            meta.lore(currentLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public int getSlot() {
        return slot;
    }

    public Material getMaterial() {
        return material;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }
}
