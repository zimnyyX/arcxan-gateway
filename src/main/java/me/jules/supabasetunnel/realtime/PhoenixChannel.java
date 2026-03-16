package me.jules.supabasetunnel.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class PhoenixChannel extends WebSocketListener {
    private final String url;
    private final String topic;
    private final String clientId;
    private final OkHttpClient client;
    private WebSocket webSocket;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger refCounter = new AtomicInteger(0);
    private Consumer<JsonNode> messageHandler;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public PhoenixChannel(String baseUrl, String apiKey, String topic) {
        this.url = baseUrl.replace("http", "ws") + "/realtime/v1/websocket?apikey=" + apiKey + "&vsn=2.0.0";
        this.topic = "realtime:" + topic;
        this.clientId = UUID.randomUUID().toString();
        this.client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    public void connect() {
        Request request = new Request.Builder().url(url).build();
        webSocket = client.newWebSocket(request, this);

        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 30, 30, TimeUnit.SECONDS);
    }

    private void sendHeartbeat() {
        if (webSocket != null) {
            ObjectNode hb = mapper.createObjectNode();
            hb.put("topic", "phoenix");
            hb.put("event", "heartbeat");
            hb.set("payload", mapper.createObjectNode());
            hb.put("ref", String.valueOf(refCounter.incrementAndGet()));
            webSocket.send(hb.toString());
        }
    }

    public void setMessageHandler(Consumer<JsonNode> handler) {
        this.messageHandler = handler;
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        join();
    }

    private void join() {
        ObjectNode joinMsg = mapper.createObjectNode();
        joinMsg.put("topic", topic);
        joinMsg.put("event", "phx_join");
        joinMsg.set("payload", mapper.createObjectNode());
        joinMsg.put("ref", String.valueOf(refCounter.incrementAndGet()));
        webSocket.send(joinMsg.toString());
    }

    public void broadcast(String event, ObjectNode payload) {
        if (webSocket == null) return;
        ObjectNode msg = mapper.createObjectNode();
        msg.put("topic", topic);
        msg.put("event", "broadcast");
        ObjectNode wrappedPayload = mapper.createObjectNode();
        wrappedPayload.put("type", "broadcast");
        wrappedPayload.put("event", event);
        wrappedPayload.put("sender", clientId);
        wrappedPayload.set("payload", payload);
        msg.set("payload", wrappedPayload);
        msg.put("ref", String.valueOf(refCounter.incrementAndGet()));
        webSocket.send(msg.toString());
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        try {
            JsonNode node = mapper.readTree(text);

            // Check if it's a broadcast from self
            if (node.has("payload") && node.get("payload").has("sender")) {
                if (clientId.equals(node.get("payload").get("sender").asText())) {
                    return; // Ignore self
                }
            }

            if (messageHandler != null) {
                messageHandler.accept(node);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        scheduler.shutdown();
        if (webSocket != null) {
            webSocket.close(1000, "Goodbye");
        }
    }
}
