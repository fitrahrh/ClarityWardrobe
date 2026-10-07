package com.harukiyu.claritywardrobe.config;

import com.harukiyu.claritywardrobe.ClarityWardrobe;
import com.harukiyu.claritywardrobe.api.WardrobeSlotType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Data-driven configuration for the dynamic wardrobe GUI loaded from wardrobe-menu.yml.
 */
public class MenuConfig {

    private final ClarityWardrobe plugin;
    private FileConfiguration config;

    private String title = "<gradient:#38ef7d:#11998e><bold>Clarity Wardrobe</bold></gradient>";
    private int rows = 6;

    // Cosmetic slots
    private int helmetSlot = 20;
    private ValidationRule helmetValidation;
    private MenuItemData helmetPlaceholder;
    private List<String> helmetEquippedLoreAppend = new ArrayList<>();

    private int backpackSlot = 24;
    private ValidationRule backpackValidation;
    private MenuItemData backpackPlaceholder;
    private List<String> backpackEquippedLoreAppend = new ArrayList<>();

    // Action buttons
    private MenuItemData unequipAllButton;
    private MenuItemData closeButton;
    private MenuItemData infoButton;

    // Decorations & background
    private final Map<String, MenuItemData> decorations = new LinkedHashMap<>();
    private boolean backgroundEnabled = true;
    private boolean backgroundFillEmpty = true;
    private MenuItemData backgroundItem;

    public MenuConfig(ClarityWardrobe plugin) {
        this.plugin = plugin;
        load();
    }

    public final void load() {
        File file = new File(plugin.getDataFolder(), "wardrobe-menu.yml");
        if (!file.exists()) {
            plugin.saveResource("wardrobe-menu.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = plugin.getResource("wardrobe-menu.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }

        title = config.getString("title", "<gradient:#38ef7d:#11998e><bold>Clarity Wardrobe</bold></gradient>");
        rows = Math.max(1, Math.min(6, config.getInt("rows", 6)));

        // Load Helmet Slot
        ConfigurationSection helmetSec = config.getConfigurationSection("slots.helmet");
        if (helmetSec != null) {
            helmetSlot = helmetSec.getInt("slot", 20);
            helmetValidation = new ValidationRule(helmetSec.getConfigurationSection("validation"));
            helmetPlaceholder = MenuItemData.fromSection(helmetSec.getConfigurationSection("placeholder"), helmetSlot);
            helmetEquippedLoreAppend = helmetSec.getStringList("equipped-lore-append");
        } else {
            helmetSlot = 20;
            helmetValidation = new ValidationRule(null);
            helmetPlaceholder = new MenuItemData(20, Material.LEATHER_HELMET, 0, "<yellow>Helmet Cosmetic Slot</yellow>", Collections.emptyList());
        }

        // Load Backpack Slot
        ConfigurationSection backpackSec = config.getConfigurationSection("slots.backpack");
        if (backpackSec != null) {
            backpackSlot = backpackSec.getInt("slot", 24);
            backpackValidation = new ValidationRule(backpackSec.getConfigurationSection("validation"));
            backpackPlaceholder = MenuItemData.fromSection(backpackSec.getConfigurationSection("placeholder"), backpackSlot);
            backpackEquippedLoreAppend = backpackSec.getStringList("equipped-lore-append");
        } else {
            backpackSlot = 24;
            backpackValidation = new ValidationRule(null);
            backpackPlaceholder = new MenuItemData(24, Material.ELYTRA, 0, "<aqua>Backpack / Wings Slot</aqua>", Collections.emptyList());
        }

        // Action buttons
        ConfigurationSection btnSec = config.getConfigurationSection("buttons");
        if (btnSec != null) {
            unequipAllButton = MenuItemData.fromSection(btnSec.getConfigurationSection("unequip-all"), 40);
            closeButton = MenuItemData.fromSection(btnSec.getConfigurationSection("close"), 49);
            infoButton = MenuItemData.fromSection(btnSec.getConfigurationSection("info"), 4);
        } else {
            unequipAllButton = new MenuItemData(40, Material.BARRIER, 0, "<red>Unequip All</red>", Collections.emptyList());
            closeButton = new MenuItemData(49, Material.IRON_DOOR, 0, "<red>Close Menu</red>", Collections.emptyList());
            infoButton = new MenuItemData(4, Material.BOOK, 0, "<gold>Info</gold>", Collections.emptyList());
        }

        // Decorations
        decorations.clear();
        ConfigurationSection decoSec = config.getConfigurationSection("decorations");
        if (decoSec != null) {
            for (String key : decoSec.getKeys(false)) {
                ConfigurationSection itemSec = decoSec.getConfigurationSection(key);
                if (itemSec != null) {
                    decorations.put(key, MenuItemData.fromSection(itemSec, itemSec.getInt("slot", 0)));
                }
            }
        }

        // Background
        ConfigurationSection bgSec = config.getConfigurationSection("background");
        if (bgSec != null) {
            backgroundEnabled = bgSec.getBoolean("enabled", true);
            backgroundFillEmpty = bgSec.getBoolean("fill-empty", true);
            backgroundItem = MenuItemData.fromSection(bgSec, 0);
        } else {
            backgroundEnabled = true;
            backgroundFillEmpty = true;
            backgroundItem = new MenuItemData(0, Material.GRAY_STAINED_GLASS_PANE, 0, "<dark_gray> ", Collections.emptyList());
        }
    }

    public boolean isHelmetSlot(int slot) {
        return slot == helmetSlot;
    }

    public boolean isBackpackSlot(int slot) {
        return slot == backpackSlot;
    }

    public boolean isCosmeticSlot(int slot) {
        return isHelmetSlot(slot) || isBackpackSlot(slot);
    }

    public WardrobeSlotType getSlotType(int slot) {
        if (isHelmetSlot(slot)) return WardrobeSlotType.HELMET;
        if (isBackpackSlot(slot)) return WardrobeSlotType.BACKPACK;
        return null;
    }

    public ValidationRule getValidationRule(WardrobeSlotType type) {
        return type == WardrobeSlotType.HELMET ? helmetValidation : backpackValidation;
    }

    public boolean isUnequipAllButton(int slot) {
        return unequipAllButton != null && unequipAllButton.getSlot() == slot;
    }

    public boolean isCloseButton(int slot) {
        return closeButton != null && closeButton.getSlot() == slot;
    }

    public String getTitle() {
        return title;
    }

    public int getRows() {
        return rows;
    }

    public int getInventorySize() {
        return rows * 9;
    }

    public int getHelmetSlot() {
        return helmetSlot;
    }

    public ValidationRule getHelmetValidation() {
        return helmetValidation;
    }

    public MenuItemData getHelmetPlaceholder() {
        return helmetPlaceholder;
    }

    public List<String> getHelmetEquippedLoreAppend() {
        return helmetEquippedLoreAppend;
    }

    public int getBackpackSlot() {
        return backpackSlot;
    }

    public ValidationRule getBackpackValidation() {
        return backpackValidation;
    }

    public MenuItemData getBackpackPlaceholder() {
        return backpackPlaceholder;
    }

    public List<String> getBackpackEquippedLoreAppend() {
        return backpackEquippedLoreAppend;
    }

    public MenuItemData getUnequipAllButton() {
        return unequipAllButton;
    }

    public MenuItemData getCloseButton() {
        return closeButton;
    }

    public MenuItemData getInfoButton() {
        return infoButton;
    }

    public Collection<MenuItemData> getDecorations() {
        return decorations.values();
    }

    public boolean isBackgroundEnabled() {
        return backgroundEnabled;
    }

    public boolean isBackgroundFillEmpty() {
        return backgroundFillEmpty;
    }

    public MenuItemData getBackgroundItem() {
        return backgroundItem;
    }
}
