# Supabase Minecraft Tunnel

This project provides a way to bypass port forwarding for your Minecraft server by using Supabase Realtime as a relay.

## How it works

1.  **Server Side**: The `SupabaseTunnel` plugin runs on your Spigot/Paper 1.16.5 server. It connects to Supabase and waits for incoming tunnel requests.
2.  **Relay**: Traffic is relayed through Supabase Realtime Broadcast.
3.  **Proxy Agent**: A proxy application (`SupabaseProxy`) connects players to the Supabase relay.

## Setup

### 1. Server Setup
1.  Install `bin/SupabaseTunnel.jar` in your server's `plugins` folder.
2.  Start the server. The config is pre-configured with your Supabase credentials.

### 2. Joining the Server

There are two ways for players to join:

#### Option A: Direct IP (Recommended)
You can host the Proxy Agent on a VPS (like a cheap $5/mo server) that *does* have ports open. Players can then join using that VPS's IP address without needing to install anything themselves.

1.  On the VPS, run the proxy:
    ```bash
    java -cp SupabaseTunnel.jar me.jules.supabasetunnel.proxy.SupabaseProxy https://rhjiubcxnxzibqsmfknk.supabase.co sb_publishable_5IOYQdhg2nfalxrtmkL53A_MnxUfxdG minecraft-tunnel 25565
    ```
2.  Players join using the **VPS IP**.

#### Option B: Local Proxy
If you don't have a VPS, players can run the proxy locally:
1.  Player runs the same command above on their own computer.
2.  Player joins using `localhost`.

## Features
- **Port Forwarding Bypass**: Host a server from home without touching router settings.
- **Database Logging**: Automatically logs joins/leaves to your Supabase `logs` table.
- **Auto-Reconnect**: The tunnel automatically recovers from network interruptions.

## Security Note
This uses Supabase Realtime Broadcast. Ensure your RLS policies allow the `anon` role to use Broadcast on the specified channel.
