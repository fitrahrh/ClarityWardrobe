package com.harukiyu.claritywardrobe.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public final class Utils {

    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    private Utils() {}

    public static String colorize(String msg) {
        if (msg == null || msg.isEmpty()) {
            return "";
        }

        Matcher match = HEX_PATTERN.matcher(msg);
        StringBuilder sb = new StringBuilder();
        while (match.find()) {
            String color = match.group();
            match.appendReplacement(sb, ChatColor.of(color).toString());
        }
        match.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}
