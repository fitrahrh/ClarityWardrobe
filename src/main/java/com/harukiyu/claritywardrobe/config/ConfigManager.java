package com.harukiyu.claritywardrobe.config;

import com.harukiyu.claritywardrobe.ClarityWardrobe;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Central configuration manager coordinating config.yml, language.yml, and wardrobe-menu.yml.
 */
public class ConfigManager {

    private final ClarityWardrobe plugin;
    private LanguageManager languageManager;
    private MenuConfig menuConfig;

    // Rendering settings
    private String helmetRenderer = "PROTOCOLLIB";
    private String backpackRenderer = "ITEM_DISPLAY";

    // ItemDisplay offsets
    private double offsetX = 0.0;
    private double offsetY = 1.15;
    private double offsetZ = -0.22;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float scaleZ = 1.0f;
    private float rotationYaw = 180.0f;
    private float rotationPitch = 0.0f;
    private double crouchOffsetY = -0.25;
    private double crouchOffsetZ = -0.05;
    private float crouchPitchTilt = 28.0f;
    private boolean hideWhenGliding = true;
    private boolean hideWhenSwimming = true;
    private boolean hideWhenSleeping = true;

    // Storage
    private String storageFile = "wardrobe-data.yml";
    private int autoSaveMinutes = 5;

    public ConfigManager(ClarityWardrobe plugin) {
        this.plugin = plugin;
        load();
    }

    public final void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        String rawHelmet = config.getString("rendering.helmet-renderer", "PROTOCOLLIB");
        helmetRenderer = (rawHelmet != null ? rawHelmet : "PROTOCOLLIB").toUpperCase();

        String rawBackpack = config.getString("rendering.backpack-renderer", "ITEM_DISPLAY");
        backpackRenderer = (rawBackpack != null ? rawBackpack : "ITEM_DISPLAY").toUpperCase();

        ConfigurationSection displaySec = config.getConfigurationSection("rendering.item-display");
        if (displaySec != null) {
            offsetX = displaySec.getDouble("offset-x", 0.0);
            offsetY = displaySec.getDouble("offset-y", 1.15);
            offsetZ = displaySec.getDouble("offset-z", -0.22);
            scaleX = (float) displaySec.getDouble("scale-x", 1.0);
            scaleY = (float) displaySec.getDouble("scale-y", 1.0);
            scaleZ = (float) displaySec.getDouble("scale-z", 1.0);
            rotationYaw = (float) displaySec.getDouble("rotation-yaw", 180.0);
            rotationPitch = (float) displaySec.getDouble("rotation-pitch", 0.0);
            crouchOffsetY = displaySec.getDouble("crouch-offset-y", -0.25);
            crouchOffsetZ = displaySec.getDouble("crouch-offset-z", -0.05);
            crouchPitchTilt = (float) displaySec.getDouble("crouch-pitch-tilt", 28.0);
            hideWhenGliding = displaySec.getBoolean("hide-when-gliding", true);
            hideWhenSwimming = displaySec.getBoolean("hide-when-swimming", true);
            hideWhenSleeping = displaySec.getBoolean("hide-when-sleeping", true);
        }

        storageFile = config.getString("storage.file", "wardrobe-data.yml");
        autoSaveMinutes = config.getInt("storage.auto-save-minutes", 5);

        if (languageManager == null) {
            languageManager = new LanguageManager(plugin);
        } else {
            languageManager.load();
        }

        if (menuConfig == null) {
            menuConfig = new MenuConfig(plugin);
        } else {
            menuConfig.load();
        }
    }

    public void reload() {
        load();
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public MenuConfig getMenuConfig() {
        return menuConfig;
    }

    public String getHelmetRenderer() {
        return helmetRenderer;
    }

    public String getBackpackRenderer() {
        return backpackRenderer;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public float getScaleZ() {
        return scaleZ;
    }

    public float getRotationYaw() {
        return rotationYaw;
    }

    public float getRotationPitch() {
        return rotationPitch;
    }

    public double getCrouchOffsetY() {
        return crouchOffsetY;
    }

    public double getCrouchOffsetZ() {
        return crouchOffsetZ;
    }

    public float getCrouchPitchTilt() {
        return crouchPitchTilt;
    }

    public boolean isHideWhenGliding() {
        return hideWhenGliding;
    }

    public boolean isHideWhenSwimming() {
        return hideWhenSwimming;
    }

    public boolean isHideWhenSleeping() {
        return hideWhenSleeping;
    }

    public String getStorageFile() {
        return storageFile;
    }

    public int getAutoSaveMinutes() {
        return autoSaveMinutes;
    }

    public ConfigurationSection getSoundSection(String key) {
        return plugin.getConfig().getConfigurationSection("sounds." + key);
    }
}
