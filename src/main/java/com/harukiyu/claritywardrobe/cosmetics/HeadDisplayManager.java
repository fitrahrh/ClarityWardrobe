package com.harukiyu.claritywardrobe.cosmetics;

import com.harukiyu.claritywardrobe.ClarityWardrobe;
import com.harukiyu.claritywardrobe.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages 3D ItemDisplay entities worn on the player's head.
 * Provides true F5 third-person self-view for cosmetic hats and helmets
 * without interfering with vanilla combat armor slots.
 */
public class HeadDisplayManager {

    private final ClarityWardrobe plugin;
    private final NamespacedKey displayKey;
    private final Map<UUID, ItemDisplay> activeDisplays = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public HeadDisplayManager(ClarityWardrobe plugin) {
        this.plugin = plugin;
        this.displayKey = new NamespacedKey(plugin, "helmet_display");

        cleanDanglingDisplays();
        startTickTask();
    }

    /**
     * Spawns or updates a player's head cosmetic ItemDisplay entity.
     */
    public void updateCosmetic(Player player, ItemStack cosmeticItem) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (cosmeticItem == null || cosmeticItem.getType().isAir()) {
            removeDisplay(player);
            return;
        }

        ConfigManager config = plugin.getConfigManager();
        String renderer = config.getHelmetRenderer();
        if (!renderer.equalsIgnoreCase("ITEM_DISPLAY") && !renderer.equalsIgnoreCase("BOTH")) {
            return;
        }

        ItemDisplay existing = activeDisplays.get(player.getUniqueId());
        if (existing != null && existing.isValid()) {
            existing.setItemStack(cosmeticItem);
            return;
        }

        Location spawnLoc = calculateHeadLocation(player);
        if (spawnLoc == null || spawnLoc.getWorld() == null) {
            return;
        }

        ItemDisplay display = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(cosmeticItem);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setInterpolationDuration(1);
            entity.setInterpolationDelay(0);
            entity.setTeleportDuration(1);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setViewRange(1.0f);

            Transformation transformation = new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 1f, 0f),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new AxisAngle4f(0f, 0f, 1f, 0f)
            );
            entity.setTransformation(transformation);

            entity.getPersistentDataContainer().set(displayKey, PersistentDataType.STRING, player.getUniqueId().toString());
        });

        activeDisplays.put(player.getUniqueId(), display);
    }

    /**
     * Removes and despawns the head display for the given player.
     */
    public void removeDisplay(Player player) {
        if (player == null) return;
        ItemDisplay display = activeDisplays.remove(player.getUniqueId());
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    /**
     * Removes and despawns the head display by UUID.
     */
    public void removeDisplay(UUID uuid) {
        if (uuid == null) return;
        ItemDisplay display = activeDisplays.remove(uuid);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    /**
     * Calculates the exact head coordinate for the player.
     */
    public Location calculateHeadLocation(Player player) {
        if (player == null) {
            return null;
        }

        Location eyeLoc = player.getEyeLocation();
        if (eyeLoc.getWorld() == null) {
            return null;
        }

        // eyeLoc already has exact head coordinates, head yaw, and head pitch
        return eyeLoc.clone();
    }

    /**
     * Ticks head display positions smoothly with 1-tick client interpolation.
     */
    private void startTickTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, ItemDisplay> entry : activeDisplays.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                ItemDisplay display = entry.getValue();

                if (player == null || !player.isOnline() || !display.isValid()) {
                    if (display.isValid()) {
                        display.remove();
                    }
                    activeDisplays.remove(entry.getKey());
                    continue;
                }

                // Check world match
                if (!display.getWorld().equals(player.getWorld())) {
                    display.remove();
                    ItemStack cosmetic = plugin.getCosmeticManager().getCosmetic(player, com.harukiyu.claritywardrobe.api.WardrobeSlotType.HELMET);
                    if (cosmetic != null) {
                        updateCosmetic(player, cosmetic);
                    }
                    continue;
                }

                // Check death or sleeping
                if (player.isDead() || player.isSleeping()) {
                    if (!display.getItemStack().getType().isAir()) {
                        display.setItemStack(null);
                    }
                    continue;
                } else {
                    ItemStack current = display.getItemStack();
                    if (current.getType().isAir()) {
                        ItemStack cosmetic = plugin.getCosmeticManager().getCosmetic(player, com.harukiyu.claritywardrobe.api.WardrobeSlotType.HELMET);
                        if (cosmetic != null) {
                            display.setItemStack(cosmetic);
                        }
                    }
                }

                Location targetLoc = calculateHeadLocation(player);
                if (targetLoc != null) {
                    display.teleport(targetLoc);
                }
            }
        }, 1L, 1L);
    }

    /**
     * Cleans up leftover displays from crashes.
     */
    public final void cleanDanglingDisplays() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(ItemDisplay.class)) {
                if (entity.getPersistentDataContainer().has(displayKey, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
    }

    /**
     * Shuts down and cleans up all head displays.
     */
    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        for (ItemDisplay display : activeDisplays.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }
        activeDisplays.clear();
    }
}
