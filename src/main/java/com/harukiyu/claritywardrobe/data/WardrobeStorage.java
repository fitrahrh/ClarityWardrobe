package com.harukiyu.claritywardrobe.data;

import com.harukiyu.claritywardrobe.ClarityWardrobe;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * High-performance asynchronous YAML storage for player wardrobe data.
 */
public class WardrobeStorage {

    private final ClarityWardrobe plugin;
    private final File dataFile;
    private final Object fileLock = new Object();
    private final Map<UUID, PlayerWardrobeData> cache = new ConcurrentHashMap<>();
    private BukkitTask autoSaveTask;

    public WardrobeStorage(ClarityWardrobe plugin) {
        this.plugin = plugin;
        String fileName = plugin.getConfigManager().getStorageFile();
        this.dataFile = new File(plugin.getDataFolder(), fileName);

        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create " + fileName, e);
            }
        }

        startAutoSaveTask();
    }

    /**
     * Gets the cached PlayerWardrobeData, loading it from disk if not yet present in cache.
     */
    public PlayerWardrobeData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadFromDisk);
    }

    /**
     * Loads a player's wardrobe data from disk.
     */
    private PlayerWardrobeData loadFromDisk(UUID uuid) {
        synchronized (fileLock) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection sec = yaml.getConfigurationSection("players." + uuid.toString());
            return PlayerWardrobeData.deserialize(uuid, sec);
        }
    }

    /**
     * Saves a player's data asynchronously or synchronously.
     */
    public void savePlayerData(UUID uuid, boolean async) {
        PlayerWardrobeData data = cache.get(uuid);
        if (data == null || !data.isDirty()) {
            return;
        }

        Runnable saveAction = () -> {
            synchronized (fileLock) {
                try {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
                    ConfigurationSection sec = yaml.createSection("players." + uuid.toString());
                    data.serialize(sec);
                    yaml.save(dataFile);
                    data.setDirty(false);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save wardrobe data for player " + uuid, e);
                }
            }
        };

        if (async && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, saveAction);
        } else {
            saveAction.run();
        }
    }

    /**
     * Unloads and saves player data from cache (e.g. on player quit).
     */
    public void unloadPlayer(UUID uuid) {
        PlayerWardrobeData data = cache.remove(uuid);
        if (data != null && data.isDirty()) {
            savePlayerData(uuid, true);
        }
    }

    /**
     * Saves all cached player data to disk.
     */
    public void saveAll(boolean async) {
        Runnable saveAllAction = () -> {
            synchronized (fileLock) {
                try {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
                    for (Map.Entry<UUID, PlayerWardrobeData> entry : cache.entrySet()) {
                        PlayerWardrobeData data = entry.getValue();
                        if (data.isDirty()) {
                            ConfigurationSection sec = yaml.createSection("players." + entry.getKey().toString());
                            data.serialize(sec);
                            data.setDirty(false);
                        }
                    }
                    yaml.save(dataFile);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save all wardrobe data to disk", e);
                }
            }
        };

        if (async && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, saveAllAction);
        } else {
            saveAllAction.run();
        }
    }

    private void startAutoSaveTask() {
        int intervalMinutes = plugin.getConfigManager().getAutoSaveMinutes();
        if (intervalMinutes <= 0) {
            return;
        }

        long intervalTicks = intervalMinutes * 60L * 20L;
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> saveAll(false),
                intervalTicks,
                intervalTicks
        );
    }

    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        // Save all synchronously before disabling to prevent data loss
        saveAll(false);
        cache.clear();
    }
}
