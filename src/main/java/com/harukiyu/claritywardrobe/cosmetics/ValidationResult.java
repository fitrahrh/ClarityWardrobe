package com.harukiyu.claritywardrobe.cosmetics;

/**
 * Result of validating an item for cosmetic placement.
 */
public enum ValidationResult {
    /**
     * Item is completely valid.
     */
    SUCCESS,

    /**
     * Item is empty or air.
     */
    INVALID_ITEM,

    /**
     * Item does not have any Custom Model Data component.
     */
    MISSING_CUSTOM_MODEL_DATA,

    /**
     * Item has Custom Model Data, but it is not in the allowed list for this slot.
     */
    UNAPPROVED_CUSTOM_MODEL_DATA,

    /**
     * Item's base material is not allowed for this slot.
     */
    UNAPPROVED_MATERIAL;

    public boolean isValid() {
        return this == SUCCESS;
    }
}
