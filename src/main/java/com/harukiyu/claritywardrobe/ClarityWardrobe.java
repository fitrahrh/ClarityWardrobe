package com.harukiyu.claritywardrobe;

import com.harukiyu.claritywardrobe.commands.WardrobeCommand;
import com.harukiyu.claritywardrobe.config.ConfigManager;
import com.harukiyu.claritywardrobe.cosmetics.CosmeticManager;
import com.harukiyu.claritywardrobe.data.WardrobeStorage;
import com.harukiyu.claritywardrobe.gui.WardrobeMenuListener;
import com.harukiyu.claritywardrobe.listeners.PlayerArmorListener;
import com.harukiyu.claritywardrobe.listeners.PlayerConnectionListener;
import com.harukiyu.claritywardrobe.packets.ProtocolLibHook;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ClarityWardrobe - High-performance cosmetic wardrobe plugin for Paper 1.21+.
 * Allows players to equip visual cosmetics without altering real combat armor stats.
 */
public class ClarityWardrobe extends JavaPlugin {

    private static ClarityWardrobe instance;

    private ConfigManager configManager;
    private WardrobeStorage storage;
    private ProtocolLibHook protocolLibHook;
    private CosmeticManager cosmeticManager;

    public static ClarityWardrobe getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // 1. Initialize configuration & language
        this.configManager = new ConfigManager(this);

        // 2. Initialize YAML storage
        this.storage = new WardrobeStorage(this);

        // 3. Initialize ProtocolLib packet hook
        this.protocolLibHook = new ProtocolLibHook(this);

        // 4. Initialize Cosmetic Manager (handles ItemDisplay and equipment packets)
        this.cosmeticManager = new CosmeticManager(this);

        // 5. Register Event Listeners
        getServer().getPluginManager().registerEvents(new WardrobeMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerArmorListener(this), this);
        getServer().getPluginManager().registerEvents(this.cosmeticManager.getDisplayManager(), this);

        // 6. Register Command and Tab Completer
        WardrobeCommand wardrobeCmd = new WardrobeCommand(this);
        PluginCommand command = getCommand("wardrobe");
        if (command != null) {
            command.setExecutor(wardrobeCmd);
            command.setTabCompleter(wardrobeCmd);
        } else {
            getLogger().severe("Command 'wardrobe' is not defined in plugin.yml!");
        }

        // 7. Refresh visuals for online players in case of reload
        for (Player player : Bukkit.getOnlinePlayers()) {
            cosmeticManager.refreshVisuals(player);
        }

        String version = getPluginMeta().getVersion();
        String protocolStatus = protocolLibHook.isEnabled() ? "Hooked" : "Disabled";

        getLogger().info("=========================================");
        getLogger().info(String.format(" ClarityWardrobe v%s has been enabled!", version));
        getLogger().info(String.format(" Target: Paper 1.21+ | ProtocolLib: %s", protocolStatus));
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling ClarityWardrobe...");

        // 1. Remove all active cosmetic displays from the world
        if (cosmeticManager != null) {
            cosmeticManager.shutdown();
        }

        // 2. Unregister ProtocolLib hooks
        if (protocolLibHook != null) {
            protocolLibHook.shutdown();
        }

        // 3. Flush all cached player data synchronously to disk
        if (storage != null) {
            storage.shutdown();
        }

        getLogger().info("ClarityWardrobe disabled successfully.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WardrobeStorage getStorage() {
        return storage;
    }

    public ProtocolLibHook getProtocolLibHook() {
        return protocolLibHook;
    }

    public CosmeticManager getCosmeticManager() {
        return cosmeticManager;
    }
}
