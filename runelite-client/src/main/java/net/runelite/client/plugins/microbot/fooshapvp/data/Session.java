package net.runelite.client.plugins.microbot.fooshapvp.data;

import lombok.Data;

@Data
public class Session {
    private String hero;
    private String opponent;
    private int hero_recoil_charges;
}
