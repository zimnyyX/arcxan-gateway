package me.jules.supabasetunnel.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.jules.supabasetunnel.realtime.PhoenixChannel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseProxy {
    private final String url;
    private final String apiKey;
    private final String topic;
    private final int localPort;
    private final PhoenixChannel channel;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Socket> connections = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SupabaseProxy(String url, String apiKey, String topic, int localPort) {
        this.url = url;
        this.apiKey = apiKey;
        this.topic = topic;
        this.localPort = localPort;
        this.channel = new PhoenixChannel(url, apiKey, topic);
        this.channel.setMessageHandler(this::handleMessage);
    }

    public void start() throws IOException {
        channel.connect();
        ServerSocket serverSocket = new ServerSocket(localPort);
        System.out.println("Proxy started on port " + localPort + ". Players can connect here.");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            String connectionId = UUID.randomUUID().toString();
            connections.put(connectionId, clientSocket);
            System.out.println("New player connection: " + connectionId);

            // Notify plugin to connect to local server
            ObjectNode payload = mapper.createObjectNode();
            payload.put("connectionId", connectionId);
            channel.broadcast("connect", payload);

            executor.submit(() -> {
                try {
                    InputStream in = clientSocket.getInputStream();
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        byte[] data = new byte[read];
                        System.arraycopy(buffer, 0, data, 0, read);
                        sendData(connectionId, data);
                    }
                } catch (IOException e) {
                    System.err.println("Player connection error (" + connectionId + "): " + e.getMessage());
                } finally {
                    handleDisconnect(connectionId);
                }
            });
        }
    }

    private void handleMessage(JsonNode node) {
        String event = node.get("event").asText();
        if (!"broadcast".equals(event)) return;

        JsonNode payloadWrapper = node.get("payload");
        if (payloadWrapper == null) return;

        String broadcastEvent = payloadWrapper.get("event").asText();
        JsonNode payload = payloadWrapper.get("payload");
        String connectionId = payload.get("connectionId").asText();

        if ("data".equals(broadcastEvent)) {
            handleData(connectionId, payload.get("data").asText());
        } else if ("disconnect".equals(broadcastEvent)) {
            handleDisconnect(connectionId);
        }
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
                System.out.println("Disconnected: " + connectionId);

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

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Usage: java SupabaseProxy <url> <apiKey> <topic> <localPort>");
            return;
        }
        new SupabaseProxy(args[0], args[1], args[2], Integer.parseInt(args[3])).start();
    }
}
