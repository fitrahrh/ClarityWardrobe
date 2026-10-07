package com.harukiyu.claritywardrobe.listeners;

import com.harukiyu.claritywardrobe.ClarityWardrobe;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Handles player join, quit, respawn, and world change events to synchronize visuals.
 */
public class PlayerConnectionListener implements Listener {

    private final ClarityWardrobe plugin;

    public PlayerConnectionListener(ClarityWardrobe plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay 2 ticks to ensure client is fully initialized in world
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getCosmeticManager().refreshVisuals(player);

                // Also send all existing players' cosmetic packets to this new player
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (!other.equals(player)) {
                        plugin.getProtocolLibHook().refreshEquipment(other);
                    }
                }
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getCosmeticManager().removeDisplays(player);
        plugin.getStorage().unloadPlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.getCosmeticManager().removeDisplays(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getCosmeticManager().refreshVisuals(player);
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        plugin.getCosmeticManager().removeDisplays(player);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getCosmeticManager().refreshVisuals(player);
            }
        }, 2L);
    }
}
