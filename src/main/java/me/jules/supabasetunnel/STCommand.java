package me.jules.supabasetunnel;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class STCommand implements CommandExecutor {
    private final SupabaseTunnel plugin;

    public STCommand(SupabaseTunnel plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§bSupabaseTunnel v1.0 §7by Jules");
            sender.sendMessage("§f/st status §7- Check tunnel status");
            sender.sendMessage("§f/st reload §7- Reload configuration");
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            boolean enabled = plugin.getConfig().getBoolean("tunnel.enabled", true);
            sender.sendMessage("§7Tunnel status: " + (enabled ? "§aEnabled" : "§cDisabled"));
            sender.sendMessage("§7Supabase URL: §f" + plugin.getConfig().getString("supabase.url"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("supabasetunnel.reload")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage("§aConfiguration reloaded. (Requires restart for tunnel changes)");
            return true;
        }

        return false;
    }
}
