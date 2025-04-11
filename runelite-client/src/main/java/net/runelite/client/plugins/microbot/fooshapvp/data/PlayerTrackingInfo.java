package net.runelite.client.plugins.microbot.fooshapvp.data;

import lombok.Data;

@Data
public class PlayerTrackingInfo {
    private SpottedPlayer player;
    private long lastSeenTimestamp;
}