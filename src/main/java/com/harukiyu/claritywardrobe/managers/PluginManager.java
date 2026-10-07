package com.harukiyu.claritywardrobe.managers;

import com.harukiyu.claritywardrobe.ClarityWardrobe;
import com.harukiyu.claritywardrobe.cosmetics.CosmeticManager;

/**
 * Convenience facade accessing plugin managers.
 */
public class PluginManager {

    private static PluginManager instance;

    private PluginManager() {}

    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public CosmeticManager getCosmeticManager() {
        return ClarityWardrobe.getInstance().getCosmeticManager();
    }
}
