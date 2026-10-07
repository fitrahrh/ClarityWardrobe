package com.harukiyu.claritywardrobe.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance utility for Adventure MiniMessage parsing, legacy color code
 * translation, and non-italic GUI item rendering.
 */
public final class ComponentUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("(?i)[&§]#([0-9a-f]{6})");
    private static final Pattern SECTION_HEX_PATTERN = Pattern.compile("(?i)[&§]x([&§][0-9a-f]){6}");

    private ComponentUtil() {}

    /**
     * Converts legacy color codes (& and §) to MiniMessage tags so both formatting
     * styles can be used simultaneously without breaking gradients or hex colors.
     */
    public static String convertLegacyToMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String result = input;

        // 1. Convert &#RRGGBB format
        Matcher hexMatcher = HEX_COLOR_PATTERN.matcher(result);
        if (hexMatcher.find()) {
            result = hexMatcher.replaceAll("<#$1>");
        }

        // 2. Convert &x&r&r&g&g&b&b or §x§r§r§g§g§b§b format
        Matcher sectionMatcher = SECTION_HEX_PATTERN.matcher(result);
        if (sectionMatcher.find()) {
            StringBuilder sb = new StringBuilder();
            do {
                String matched = sectionMatcher.group();
                String hex = matched.replaceAll("[&§xX]", "");
                sectionMatcher.appendReplacement(sb, "<#" + hex + ">");
            } while (sectionMatcher.find());
            sectionMatcher.appendTail(sb);
            result = sb.toString();
        }

        // 3. Convert standard Minecraft 16 color & decoration codes
        return result
                .replace("&0", "<black>").replace("§0", "<black>")
                .replace("&1", "<dark_blue>").replace("§1", "<dark_blue>")
                .replace("&2", "<dark_green>").replace("§2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("§3", "<dark_aqua>")
                .replace("&4", "<dark_red>").replace("§4", "<dark_red>")
                .replace("&5", "<dark_purple>").replace("§5", "<dark_purple>")
                .replace("&6", "<gold>").replace("§6", "<gold>")
                .replace("&7", "<gray>").replace("§7", "<gray>")
                .replace("&8", "<dark_gray>").replace("§8", "<dark_gray>")
                .replace("&9", "<blue>").replace("§9", "<blue>")
                .replace("&a", "<green>").replace("§a", "<green>")
                .replace("&b", "<aqua>").replace("§b", "<aqua>")
                .replace("&c", "<red>").replace("§c", "<red>")
                .replace("&d", "<light_purple>").replace("§d", "<light_purple>")
                .replace("&e", "<yellow>").replace("§e", "<yellow>")
                .replace("&f", "<white>").replace("§f", "<white>")
                .replace("&k", "<obfuscated>").replace("§k", "<obfuscated>")
                .replace("&l", "<bold>").replace("§l", "<bold>")
                .replace("&m", "<strikethrough>").replace("§m", "<strikethrough>")
                .replace("&n", "<underlined>").replace("§n", "<underlined>")
                .replace("&o", "<italic>").replace("§o", "<italic>")
                .replace("&r", "<reset>").replace("§r", "<reset>");
    }

    /**
     * Parses a text string with MiniMessage, automatically converting legacy color codes.
     */
    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String converted = convertLegacyToMiniMessage(input);
        return MINI_MESSAGE.deserialize(converted);
    }

    /**
     * Parses a text string after applying placeholder replacements.
     */
    public static Component parse(String input, Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        String formatted = input;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                formatted = formatted.replace(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
            }
        }

        return parse(formatted);
    }

    /**
     * Parses text specifically for item display names and lore.
     * Prevents Minecraft's default forced client-side italic on items unless explicitly specified.
     */
    public static Component parseItemText(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }

        String converted = convertLegacyToMiniMessage(input);

        // Prepend <!italic> if user didn't explicitly request italic formatting
        if (!converted.contains("<italic>") && !converted.contains("<i>")) {
            converted = "<!italic>" + converted;
        }

        Component component = MINI_MESSAGE.deserialize(converted);
        return component.decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Parses a list of strings for item lore, removing Minecraft's default italic from all lines.
     */
    public static List<Component> parseItemLore(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<>();
        }

        List<Component> components = new ArrayList<>(lines.size());
        for (String line : lines) {
            components.add(parseItemText(line));
        }
        return components;
    }

    /**
     * Serializes an Adventure Component into a MiniMessage formatted string.
     */
    public static String serializeMiniMessage(Component component) {
        if (component == null) {
            return "";
        }
        return MINI_MESSAGE.serialize(component);
    }

    /**
     * Parses a list of strings into a list of Components (standard format).
     */
    public static List<Component> parseList(List<String> lines) {
        return parseItemLore(lines);
    }

    /**
     * Plays a sound to a player based on a configuration section.
     */
    public static void playSound(Player player, ConfigurationSection section) {
        if (player == null || section == null || !section.getBoolean("enabled", true)) {
            return;
        }

        String soundName = section.getString("sound");
        if (soundName == null || soundName.isEmpty()) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            float volume = (float) section.getDouble("volume", 1.0);
            float pitch = (float) section.getDouble("pitch", 1.0);
            Location loc = player.getLocation();
            if (loc != null) {
                player.playSound(loc, sound, volume, pitch);
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid sound name in config
        }
    }
}
