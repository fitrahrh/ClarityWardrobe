package com.harukiyu.claritywardrobe.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.harukiyu.claritywardrobe.ClarityWardrobe;
import com.harukiyu.claritywardrobe.api.WardrobeSlotType;
import com.harukiyu.claritywardrobe.data.PlayerWardrobeData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * ProtocolLib packet adapter intercepting ENTITY_EQUIPMENT packets.
 * Overrides the visual helmet (and optionally chestplate) without touching real combat armor.
 */
public class EquipmentPacketListener extends PacketAdapter {

    private final ClarityWardrobe wardrobePlugin;

    public EquipmentPacketListener(ClarityWardrobe wardrobePlugin) {
        super(wardrobePlugin, PacketType.Play.Server.ENTITY_EQUIPMENT);
        this.wardrobePlugin = wardrobePlugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.isCancelled()) return;

        PacketContainer packet = event.getPacket();
        int entityId = packet.getIntegers().read(0);

        Player viewer = event.getPlayer();
        if (viewer == null) return;

        // Find the target entity in the viewer's world
        Entity targetEntity = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getEntityId() == entityId) {
                targetEntity = p;
                break;
            }
        }

        if (!(targetEntity instanceof Player targetPlayer)) {
            return;
        }

        PlayerWardrobeData data = wardrobePlugin.getStorage().getPlayerData(targetPlayer.getUniqueId());
        if (data == null || data.isEmpty()) {
            return;
        }

        List<Pair<EnumWrappers.ItemSlot, ItemStack>> pairList = packet.getSlotStackPairLists().read(0);
        if (pairList == null || pairList.isEmpty()) {
            return;
        }

        boolean modified = false;
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> modifiedList = new ArrayList<>(pairList.size());

        boolean checkChest = wardrobePlugin.getConfigManager().getBackpackRenderer().equalsIgnoreCase("CHEST_PACKET");

        for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : pairList) {
            EnumWrappers.ItemSlot slot = pair.getFirst();

            if (slot == EnumWrappers.ItemSlot.HEAD) {
                ItemStack cosmeticHelmet = data.getCosmetic(WardrobeSlotType.HELMET);
                if (cosmeticHelmet != null && !cosmeticHelmet.getType().isAir()) {
                    modifiedList.add(new Pair<>(slot, cosmeticHelmet.clone()));
                    modified = true;
                    continue;
                }
            } else if (checkChest && slot == EnumWrappers.ItemSlot.CHEST) {
                ItemStack cosmeticBackpack = data.getCosmetic(WardrobeSlotType.BACKPACK);
                if (cosmeticBackpack != null && !cosmeticBackpack.getType().isAir()) {
                    modifiedList.add(new Pair<>(slot, cosmeticBackpack.clone()));
                    modified = true;
                    continue;
                }
            }

            modifiedList.add(pair);
        }

        if (modified) {
            packet.getSlotStackPairLists().write(0, modifiedList);
        }
    }
}
