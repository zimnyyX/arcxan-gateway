package me.jules.supabasetunnel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import java.io.IOException;

public class SupabaseManager {
    private final SupabaseTunnel plugin;
    private final OkHttpClient httpClient;
    private final String url;
    private final String anonKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public SupabaseManager(SupabaseTunnel plugin) {
        this.plugin = plugin;
        this.httpClient = new OkHttpClient();
        this.url = plugin.getConfig().getString("supabase.url");
        this.anonKey = plugin.getConfig().getString("supabase.anon-key");
    }

    public void storeLog(String event, String details) {
        ObjectNode bodyNode = mapper.createObjectNode();
        bodyNode.put("event", event);
        bodyNode.put("details", details);

        RequestBody body = RequestBody.create(
            bodyNode.toString(),
            MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
            .url(url + "/rest/v1/logs")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer " + anonKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                plugin.getLogger().warning("Failed to store log in Supabase: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
            }
        });
    }

    public void disconnect() {
        // Handled by PhoenixChannel
    }

    public String getUrl() { return url; }
    public String getAnonKey() { return anonKey; }
}
