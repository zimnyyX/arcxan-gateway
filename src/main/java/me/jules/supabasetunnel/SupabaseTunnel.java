package me.jules.supabasetunnel;

import me.jules.supabasetunnel.realtime.PhoenixChannel;
import org.bukkit.plugin.java.JavaPlugin;

public class SupabaseTunnel extends JavaPlugin {
    private SupabaseManager supabaseManager;
    private PhoenixChannel channel;
    private TunnelManager tunnelManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.supabaseManager = new SupabaseManager(this);

        if (getConfig().getBoolean("tunnel.enabled", true)) {
            String topic = getConfig().getString("supabase.channel", "minecraft-tunnel");
            this.channel = new PhoenixChannel(supabaseManager.getUrl(), supabaseManager.getAnonKey(), topic);
            this.tunnelManager = new TunnelManager(this, channel);
            channel.connect();
            getLogger().info("Tunnel initialized on topic: " + topic);
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getCommand("st").setExecutor(new STCommand(this));

        getLogger().info("SupabaseTunnel enabled!");
    }

    @Override
    public void onDisable() {
        if (channel != null) {
            channel.disconnect();
        }
        if (supabaseManager != null) {
            supabaseManager.disconnect();
        }
        getLogger().info("SupabaseTunnel disabled!");
    }

    public SupabaseManager getSupabaseManager() {
        return supabaseManager;
    }
}
