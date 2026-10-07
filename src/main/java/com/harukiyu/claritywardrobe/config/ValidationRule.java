package com.harukiyu.claritywardrobe.config;

import com.harukiyu.claritywardrobe.cosmetics.ValidationResult;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates Custom Model Data and item validation rules for a cosmetic slot.
 */
public class ValidationRule {

    private final boolean requireCustomModelData;
    private final boolean allowAnyCmd;
    private final Set<Integer> allowedCmds = new HashSet<>();
    private final boolean allowAnyMaterial;
    private final Set<Material> allowedMaterials = new HashSet<>();

    public ValidationRule(ConfigurationSection section) {
        if (section == null) {
            this.requireCustomModelData = true;
            this.allowAnyCmd = true;
            this.allowAnyMaterial = true;
            return;
        }

        this.requireCustomModelData = section.getBoolean("require-custom-model-data", true);

        List<String> rawCmds = section.getStringList("allowed-cmd");
        if (rawCmds.isEmpty() || rawCmds.contains("*")) {
            this.allowAnyCmd = true;
        } else {
            this.allowAnyCmd = false;
            for (String raw : rawCmds) {
                try {
                    this.allowedCmds.add(Integer.valueOf(raw.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        List<String> rawMats = section.getStringList("allowed-materials");
        if (rawMats.isEmpty() || rawMats.contains("*")) {
            this.allowAnyMaterial = true;
        } else {
            this.allowAnyMaterial = false;
            for (String raw : rawMats) {
                Material mat = Material.matchMaterial(raw.trim().toUpperCase());
                if (mat != null) {
                    this.allowedMaterials.add(mat);
                }
            }
        }
    }

    /**
     * Validates an item against these rules.
     *
     * @param item The item stack to check.
     * @return ValidationResult enum.
     */
    public ValidationResult validate(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return ValidationResult.INVALID_ITEM;
        }

        // Material whitelist check
        if (!allowAnyMaterial && !allowedMaterials.contains(item.getType())) {
            return ValidationResult.UNAPPROVED_MATERIAL;
        }

        // Custom Model Data check
        ItemMeta meta = item.getItemMeta();
        if (requireCustomModelData) {
            if (meta == null || !meta.hasCustomModelData()) {
                return ValidationResult.MISSING_CUSTOM_MODEL_DATA;
            }

            if (!allowAnyCmd) {
                int cmd = meta.getCustomModelData();
                if (!allowedCmds.contains(cmd)) {
                    return ValidationResult.UNAPPROVED_CUSTOM_MODEL_DATA;
                }
            }
        }

        return ValidationResult.SUCCESS;
    }

    public boolean isRequireCustomModelData() {
        return requireCustomModelData;
    }

    public boolean isAllowAnyCmd() {
        return allowAnyCmd;
    }

    public Set<Integer> getAllowedCmds() {
        return allowedCmds;
    }
}
