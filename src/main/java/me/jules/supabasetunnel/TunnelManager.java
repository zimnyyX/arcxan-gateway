package me.jules.supabasetunnel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.jules.supabasetunnel.realtime.PhoenixChannel;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TunnelManager {
    private final SupabaseTunnel plugin;
    private final PhoenixChannel channel;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Socket> connections = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final int localPort;

    public TunnelManager(SupabaseTunnel plugin, PhoenixChannel channel) {
        this.plugin = plugin;
        this.channel = channel;
        this.localPort = plugin.getConfig().getInt("tunnel.local-port", 25565);

        channel.setMessageHandler(this::handleMessage);
    }

    private void handleMessage(JsonNode node) {
        String event = node.get("event").asText();
        if (!"broadcast".equals(event)) return;

        JsonNode payloadWrapper = node.get("payload");
        if (payloadWrapper == null) return;

        String broadcastEvent = payloadWrapper.get("event").asText();
        JsonNode payload = payloadWrapper.get("payload");

        String connectionId = payload.get("connectionId").asText();

        if ("connect".equals(broadcastEvent)) {
            handleConnect(connectionId);
        } else if ("data".equals(broadcastEvent)) {
            handleData(connectionId, payload.get("data").asText());
        } else if ("disconnect".equals(broadcastEvent)) {
            handleDisconnect(connectionId);
        }
    }

    private void handleConnect(String connectionId) {
        executor.submit(() -> {
            try {
                Socket socket = new Socket("localhost", localPort);
                connections.put(connectionId, socket);
                plugin.getLogger().info("Connected tunnel for " + connectionId);

                InputStream in = socket.getInputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    byte[] data = new byte[read];
                    System.arraycopy(buffer, 0, data, 0, read);
                    sendData(connectionId, data);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Tunnel connection failed for " + connectionId + ": " + e.getMessage());
            } finally {
                handleDisconnect(connectionId);
            }
        });
    }

    private void handleData(String connectionId, String base64Data) {
        Socket socket = connections.get(connectionId);
        if (socket != null) {
            try {
                byte[] data = Base64.getDecoder().decode(base64Data);
                OutputStream out = socket.getOutputStream();
                out.write(data);
                out.flush();
            } catch (IOException e) {
                handleDisconnect(connectionId);
            }
        }
    }

    private void handleDisconnect(String connectionId) {
        Socket socket = connections.remove(connectionId);
        if (socket != null) {
            try {
                socket.close();
                plugin.getLogger().info("Disconnected tunnel for " + connectionId);

                ObjectNode payload = mapper.createObjectNode();
                payload.put("connectionId", connectionId);
                channel.broadcast("disconnect", payload);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendData(String connectionId, byte[] data) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("connectionId", connectionId);
        payload.put("data", Base64.getEncoder().encodeToString(data));
        channel.broadcast("data", payload);
    }
}
