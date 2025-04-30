package net.runelite.client.plugins.microbot.fooshapvp;

import com.google.gson.Gson;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.flipperone.FooshaPVPConfig;
import net.runelite.client.plugins.microbot.fooshapvp.data.CombatData;
import net.runelite.client.plugins.microbot.fooshapvp.data.GameActionsDeserializer;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.reflection.Rs2Reflection;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class FooshaGmaulScript extends Script {
    private final List<String> equipOptions = new ArrayList<>(Arrays.asList("Wear", "Wield", "Equip"));




    private int lastActionGameCycle = 0;



    private void init(FooshaPVPConfig config) {

    }

    private String findMatchingEquipOption(String[] actions) {
        for (String action : actions) {
            if (equipOptions.contains(action)) {
                return action; // Return the first matching value
            }
        }
        return null; // Return null if no match is found
    }



    public boolean run(FooshaPVPConfig config) {
        init(config);
//        mainScheduledFuture = scheduledExecutorService.schedule(() -> {
//
//        }, 0, TimeUnit.MILLISECONDS);




        return true;
    }
}