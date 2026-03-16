# Supabase Minecraft Tunnel

This project provides a way to bypass port forwarding for your Minecraft server by using Supabase Realtime as a relay.

## How it works

1.  **Server Side**: The `SupabaseTunnel` plugin runs on your Spigot/Paper 1.16.5 server. It connects to Supabase and waits for incoming tunnel requests.
2.  **Client Side**: Players run the `SupabaseProxy` standalone application on their own computer.
3.  **The Tunnel**: When a player connects to their local proxy, the proxy sends the data to Supabase. Supabase broadcasts it to your server plugin. The plugin then hands the data to the local Minecraft server. Responses go back through the same path.

## Setup

### 1. Server Setup
1.  Install the generated JAR in your server's `plugins` folder.
2.  Start the server to generate `plugins/SupabaseTunnel/config.yml`.
3.  The config is already pre-configured with your Supabase credentials.
4.  Ensure your server is running on port 25565 (or update `tunnel.local-port` in config).

### 2. Player Setup (The Proxy)
Players need to run the proxy to connect. Since the proxy is part of the same JAR, they can run it using:

```bash
java -cp SupabaseTunnel-1.0-SNAPSHOT.jar me.jules.supabasetunnel.proxy.SupabaseProxy https://rhjiubcxnxzibqsmfknk.supabase.co sb_publishable_5IOYQdhg2nfalxrtmkL53A_MnxUfxdG minecraft-tunnel 25565
```

Then, they can join the server in Minecraft by connecting to `localhost:25565`.

### 3. Database Logging
The plugin automatically logs join/leave events to a table named `logs` in your Supabase database. Ensure you have a table created with the following columns:
- `id`: int8 (Primary Key, Identity)
- `created_at`: timestamptz (Default: now())
- `event`: text
- `details`: text

## Security Note
This uses the "Broadcast" feature of Supabase Realtime. Ensure your RLS policies allow authenticated/anon users to broadcast if you haven't already disabled RLS for development.
