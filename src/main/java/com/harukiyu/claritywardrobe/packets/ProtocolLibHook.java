package com.harukiyu.claritywardrobe.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.harukiyu.claritywardrobe.ClarityWardrobe;
import com.harukiyu.claritywardrobe.api.WardrobeSlotType;
import com.harukiyu.claritywardrobe.data.PlayerWardrobeData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Handles ProtocolLib integration and packet broadcasting for cosmetic armor overriding.
 */
public class ProtocolLibHook {

    private final ClarityWardrobe plugin;
    private ProtocolManager protocolManager;
    private EquipmentPacketListener packetListener;
    private boolean enabled = false;

    public ProtocolLibHook(ClarityWardrobe plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            plugin.getLogger().warning("ProtocolLib was not detected. Visual helmet packet overriding is disabled.");
            return;
        }

        try {
            protocolManager = ProtocolLibrary.getProtocolManager();
            packetListener = new EquipmentPacketListener(plugin);
            protocolManager.addPacketListener(packetListener);
            enabled = true;
            plugin.getLogger().info("Successfully hooked into ProtocolLib for equipment packet handling.");
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize ProtocolLib hook: {0}", e.getMessage());
            enabled = false;
        }
    }

    /**
     * Refreshes the visual equipment packets of a target player for all nearby viewers.
     *
     * @param target The player whose visual equipment changed.
     */
    public void refreshEquipment(Player target) {
        if (!enabled || protocolManager == null || target == null || !target.isOnline()) {
            return;
        }

        Location targetLoc = target.getLocation();
        if (targetLoc == null || targetLoc.getWorld() == null) {
            return;
        }

        World targetWorld = targetLoc.getWorld();

        try {
            PlayerWardrobeData data = plugin.getStorage().getPlayerData(target.getUniqueId());

            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
            packet.getIntegers().write(0, target.getEntityId());

            List<Pair<EnumWrappers.ItemSlot, ItemStack>> pairs = new ArrayList<>();

            // HEAD SLOT
            ItemStack visualHead = data != null ? data.getCosmetic(WardrobeSlotType.HELMET) : null;
            if (visualHead == null || visualHead.getType().isAir()) {
                visualHead = target.getInventory().getHelmet();
            }
            pairs.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, visualHead != null ? visualHead.clone() : new ItemStack(Material.AIR)));

            // CHEST SLOT (if CHEST_PACKET mode is active)
            boolean checkChest = plugin.getConfigManager().getBackpackRenderer().equalsIgnoreCase("CHEST_PACKET");
            if (checkChest) {
                ItemStack visualChest = data != null ? data.getCosmetic(WardrobeSlotType.BACKPACK) : null;
                if (visualChest == null || visualChest.getType().isAir()) {
                    visualChest = target.getInventory().getChestplate();
                }
                pairs.add(new Pair<>(EnumWrappers.ItemSlot.CHEST, visualChest != null ? visualChest.clone() : new ItemStack(Material.AIR)));
            }

            packet.getSlotStackPairLists().write(0, pairs);

            // Broadcast to all players in the same world within tracking range (64 blocks)
            for (Player viewer : targetWorld.getPlayers()) {
                if (viewer.equals(target)) {
                    // Also send to player themselves
                    protocolManager.sendServerPacket(target, packet);
                    continue;
                }
                Location viewerLoc = viewer.getLocation();
                if (viewerLoc != null && viewerLoc.getWorld() != null && viewerLoc.getWorld().equals(targetWorld)) {
                    if (viewerLoc.distanceSquared(targetLoc) <= 64 * 64) {
                        protocolManager.sendServerPacket(viewer, packet);
                    }
                }
            }
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to broadcast equipment refresh packet: {0}", e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void shutdown() {
        if (enabled && protocolManager != null && packetListener != null) {
            try {
                protocolManager.removePacketListener(packetListener);
            } catch (Throwable ignored) {}
        }
    }
}
