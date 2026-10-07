package com.harukiyu.claritywardrobe.config;

import com.harukiyu.claritywardrobe.ClarityWardrobe;
import com.harukiyu.claritywardrobe.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages language strings and formatting using Adventure MiniMessage.
 */
public class LanguageManager {

    private final ClarityWardrobe plugin;
    private FileConfiguration langConfig;
    private String rawPrefix = "";
    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(ClarityWardrobe plugin) {
        this.plugin = plugin;
        load();
    }

    public final void load() {
        File file = new File(plugin.getDataFolder(), "language.yml");
        if (!file.exists()) {
            plugin.saveResource("language.yml", false);
        }

        langConfig = YamlConfiguration.loadConfiguration(file);

        // Load defaults from jar if missing
        InputStream defaultStream = plugin.getResource("language.yml");
        if (defaultStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            langConfig.setDefaults(defConfig);
        }

        rawPrefix = langConfig.getString("prefix", "<gradient:#4facfe:#00f2fe><b>ClarityWardrobe</b></gradient> <dark_gray>»</dark_gray> ");
        messageCache.clear();

        ConfigurationSection msgSection = langConfig.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                messageCache.put(key, msgSection.getString(key, ""));
            }
        }
    }

    public Component getMessage(String key, Map<String, String> placeholders) {
        String raw = messageCache.getOrDefault(key, "<red>Missing message for: " + key + "</red>");
        String combined = rawPrefix + raw;
        return ComponentUtil.parse(combined, placeholders);
    }

    public Component getMessage(String key) {
        return getMessage(key, Collections.emptyMap());
    }

    public Component getRawMessage(String key, Map<String, String> placeholders) {
        String raw = messageCache.getOrDefault(key, "<red>Missing message for: " + key + "</red>");
        return ComponentUtil.parse(raw, placeholders);
    }

    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        if (sender == null) return;
        sender.sendMessage(getMessage(key, placeholders));
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, Collections.emptyMap());
    }
}
