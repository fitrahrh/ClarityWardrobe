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
 * High-performance manager for rendering backpacks and wings using Minecraft 1.21+
 * ItemDisplay entities with smooth client interpolation.
 */
public class BackpackDisplayManager {

    private final ClarityWardrobe plugin;
    private final NamespacedKey displayKey;
    private final Map<UUID, ItemDisplay> activeDisplays = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public BackpackDisplayManager(ClarityWardrobe plugin) {
        this.plugin = plugin;
        this.displayKey = new NamespacedKey(plugin, "backpack_display");

        cleanDanglingDisplays();
        startTickTask();
    }

    /**
     * Spawns or updates a player's backpack ItemDisplay entity.
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
        if (!config.getBackpackRenderer().equalsIgnoreCase("ITEM_DISPLAY")) {
            return;
        }

        ItemDisplay existing = activeDisplays.get(player.getUniqueId());
        if (existing != null && existing.isValid()) {
            existing.setItemStack(cosmeticItem);
            return;
        }

        // Spawn new ItemDisplay
        Location spawnLoc = calculateBackLocation(player);
        if (spawnLoc == null || spawnLoc.getWorld() == null) {
            return;
        }

        ItemDisplay display = spawnLoc.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(cosmeticItem);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setInterpolationDuration(1);
            entity.setInterpolationDelay(0);
            entity.setTeleportDuration(1);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setViewRange(1.0f);

            // Setup scale transformation with valid non-zero axis
            Transformation transformation = new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 1f, 0f),
                    new Vector3f(config.getScaleX(), config.getScaleY(), config.getScaleZ()),
                    new AxisAngle4f(0f, 0f, 1f, 0f)
            );
            entity.setTransformation(transformation);

            entity.getPersistentDataContainer().set(displayKey, PersistentDataType.STRING, player.getUniqueId().toString());
        });

        activeDisplays.put(player.getUniqueId(), display);
    }

    /**
     * Removes and despawns the backpack display for the given player.
     */
    public void removeDisplay(Player player) {
        if (player == null) return;
        ItemDisplay display = activeDisplays.remove(player.getUniqueId());
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    /**
     * Removes and despawns the backpack display by UUID.
     */
    public void removeDisplay(UUID uuid) {
        if (uuid == null) return;
        ItemDisplay display = activeDisplays.remove(uuid);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    /**
     * Calculates the exact coordinate behind the player's upper spine.
     */
    public Location calculateBackLocation(Player player) {
        if (player == null) {
            return null;
        }

        Location pLoc = player.getLocation();
        if (pLoc == null || pLoc.getWorld() == null) {
            return null;
        }

        ConfigManager cfg = plugin.getConfigManager();
        double yaw = pLoc.getYaw();
        double yawRad = Math.toRadians(yaw);

        double offX = cfg.getOffsetX();
        double offY = cfg.getOffsetY();
        double offZ = cfg.getOffsetZ();
        float pitch = cfg.getRotationPitch();

        // Adjust for crouching / sneaking
        if (player.isSneaking()) {
            offY += cfg.getCrouchOffsetY();
            offZ += cfg.getCrouchOffsetZ();
            pitch += cfg.getCrouchPitchTilt();
        }

        // Calculate back coordinates based on player yaw
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double worldX = pLoc.getX() + (cos * offX) - (sin * offZ);
        double worldY = pLoc.getY() + offY;
        double worldZ = pLoc.getZ() + (sin * offX) + (cos * offZ);

        Location loc = new Location(pLoc.getWorld(), worldX, worldY, worldZ);
        loc.setYaw((float) (yaw + cfg.getRotationYaw()));
        loc.setPitch(pitch);
        return loc;
    }

    /**
     * Ticks movement and visibility for all active backpack displays smoothly.
     */
    private void startTickTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ConfigManager cfg = plugin.getConfigManager();

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
                    ItemStack cosmetic = plugin.getCosmeticManager().getCosmetic(player, com.harukiyu.claritywardrobe.api.WardrobeSlotType.BACKPACK);
                    if (cosmetic != null) {
                        updateCosmetic(player, cosmetic);
                    }
                    continue;
                }

                // Check hiding conditions
                boolean shouldHide = (cfg.isHideWhenGliding() && player.isGliding())
                        || (cfg.isHideWhenSwimming() && player.isSwimming())
                        || (cfg.isHideWhenSleeping() && player.isSleeping())
                        || player.isDead();

                if (shouldHide) {
                    if (!display.getItemStack().getType().isAir()) {
                        display.setItemStack(null);
                    }
                    continue;
                } else {
                    ItemStack current = display.getItemStack();
                    if (current.getType().isAir()) {
                        ItemStack cosmetic = plugin.getCosmeticManager().getCosmetic(player, com.harukiyu.claritywardrobe.api.WardrobeSlotType.BACKPACK);
                        if (cosmetic != null) {
                            display.setItemStack(cosmetic);
                        }
                    }
                }

                // Smoothly update location
                Location targetLoc = calculateBackLocation(player);
                if (targetLoc != null) {
                    display.teleport(targetLoc);
                }
            }
        }, 1L, 1L);
    }

    /**
     * Cleans up any leftover dangling displays from unexpected restarts or crashes.
     */
    public final void cleanDanglingDisplays() {
        NamespacedKey helmetKey = new NamespacedKey(plugin, "helmet_display");
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(ItemDisplay.class)) {
                if (entity.getPersistentDataContainer().has(displayKey, PersistentDataType.STRING)
                        || entity.getPersistentDataContainer().has(helmetKey, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
    }

    /**
     * Despawns all active displays during shutdown.
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
