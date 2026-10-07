package com.harukiyu.claritywardrobe.cosmetics;

import com.harukiyu.claritywardrobe.ClarityWardrobe;
import com.harukiyu.claritywardrobe.api.WardrobeSlotType;
import com.harukiyu.claritywardrobe.config.ValidationRule;
import org.bukkit.inventory.ItemStack;

/**
 * Validates cosmetic items based on rules loaded from wardrobe-menu.yml.
 */
public class CosmeticValidator {

    private final ClarityWardrobe plugin;

    public CosmeticValidator(ClarityWardrobe plugin) {
        this.plugin = plugin;
    }

    /**
     * Validates if the given ItemStack can be placed into the specified cosmetic slot.
     *
     * @param slotType The cosmetic slot type (HELMET or BACKPACK).
     * @param item     The item to validate.
     * @return ValidationResult status.
     */
    public ValidationResult validate(WardrobeSlotType slotType, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return ValidationResult.INVALID_ITEM;
        }

        ValidationRule rule = plugin.getConfigManager().getMenuConfig().getValidationRule(slotType);
        if (rule == null) {
            return ValidationResult.SUCCESS;
        }

        return rule.validate(item);
    }
}
