package me.jules.supabasetunnel;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final SupabaseTunnel plugin;

    public PlayerListener(SupabaseTunnel plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getSupabaseManager().storeLog("JOIN", event.getPlayer().getName() + " joined the server");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getSupabaseManager().storeLog("QUIT", event.getPlayer().getName() + " left the server");
    }
}
