package com.harukiyu.claritywardrobe.commands;

import com.harukiyu.claritywardrobe.ClarityWardrobe;
import com.harukiyu.claritywardrobe.gui.WardrobeMenu;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command handler for /wardrobe, /cw, and /cosmetics.
 */
public class WardrobeCommand implements CommandExecutor, TabCompleter {

    private final ClarityWardrobe plugin;

    public WardrobeCommand(ClarityWardrobe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Subcommand: /wardrobe reload
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("claritywardrobe.admin")) {
                plugin.getConfigManager().getLanguageManager().sendMessage(sender, "error-no-permission");
                return true;
            }

            plugin.getConfigManager().reload();

            // Refresh visuals for all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getCosmeticManager().refreshVisuals(player);
            }

            plugin.getConfigManager().getLanguageManager().sendMessage(sender, "reload-success");
            return true;
        }

        // Main command: /wardrobe (Opens GUI)
        if (!(sender instanceof Player player)) {
            plugin.getConfigManager().getLanguageManager().sendMessage(sender, "player-only");
            return true;
        }

        if (!player.hasPermission("claritywardrobe.use")) {
            plugin.getConfigManager().getLanguageManager().sendMessage(player, "error-no-permission");
            return true;
        }

        if (args.length > 0) {
            plugin.getConfigManager().getLanguageManager().sendMessage(player, "unknown-subcommand");
            return true;
        }

        new WardrobeMenu(plugin, player).open();
        plugin.getConfigManager().getLanguageManager().sendMessage(player, "gui-opened");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("claritywardrobe.admin")) {
                completions.add("reload");
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
