package net.runelite.client.plugins.microbot.fooshapvp;

import com.google.gson.Gson;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.flipperone.FooshaPVPConfig;
import net.runelite.client.plugins.microbot.fooshapvp.data.CombatData;
import net.runelite.client.plugins.microbot.fooshapvp.data.GameActionsDeserializer;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

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

public class FooshaPVPScript extends Script {
    private final List<String> equipOptions = new ArrayList<>(Arrays.asList("Wear", "Wield", "Equip"));
    private static final URI endpoint = URI.create("http://192.168.1.193:5010/combat");
    private int loop_delay = 20;
    private Gson gson;
    private HttpClient client;

    private int lastActionGameCycle = 0;

    private ConcurrentLinkedQueue<GameActionsDeserializer.Action> actionQueue = new ConcurrentLinkedQueue<>();

    private void init(FooshaPVPConfig config) {
        gson = new Gson();
        client = HttpClient.newHttpClient();
    }

    private String findMatchingEquipOption(String[] actions) {
        for (String action : actions) {
            if (equipOptions.contains(action)) {
                return action; // Return the first matching value
            }
        }
        return null; // Return null if no match is found
    }

    private List<GameActionsDeserializer.Action> getActions(CombatData tickData) {
        var tickDataJson = gson.toJson(tickData);
        //System.out.println("Requesting next tick actions: " + tickDataJson);
        List<GameActionsDeserializer.Action> actions = null;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(tickDataJson))
                .build();

        try {
            var responseStart = System.currentTimeMillis();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            System.out.println("Response took: " + (System.currentTimeMillis() - responseStart));
            if (response.statusCode() == 500) {
                System.out.println("[Error] 500 Internal server error from API");
            } else {
                //JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                System.out.println("Got response: " + response.body());

                try {
                    actions = GameActionsDeserializer.parseActions(response.body());
                } catch (Exception e) {
                    System.out.println("Failed to add actions: " + e.getMessage());
                }

            }

        } catch (Exception e) {
            System.out.println("Got an exception when requesting tick actions \n " + e.getMessage());
        }


        return actions;
    }

    public void checkRecoil() {
        scheduledExecutorService.schedule(() -> {
            System.out.println("We should check the recoil");
            if (Rs2Equipment.isEquipped(2550, EquipmentInventorySlot.RING)) {
                System.out.println("We have a recoil equipped");
                Rs2Equipment.interact(2550, "Check");
            } else {
                System.out.println("We do NOT have a recoil equipped");
            }
        }, 0, TimeUnit.MILLISECONDS);
    }

    public void getAndDoStuff(CombatData td) {

        System.out.println("Action request start: " + System.currentTimeMillis());
        scheduledExecutorService.schedule(() -> {

            List<GameActionsDeserializer.Action> actions;
            try {
                actions = getActions(td);
            } catch (Exception e) {
                System.out.println("Failed to get actions: " + e.getMessage());
                return;
            }

            System.out.println("[GetActionsThread] Action has " + actionQueue.size() + " remaining actions");
            actionQueue.clear();
            actionQueue.addAll(actions);

            System.out.println("Action request end: " + System.currentTimeMillis());
        }, 0, TimeUnit.MILLISECONDS);
    }

    public boolean run(FooshaPVPConfig config) {
        init(config);
        mainScheduledFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (Microbot.getClient().getGameCycle() == lastActionGameCycle) return;
            lastActionGameCycle = Microbot.getClient().getGameCycle();
            var clientTickAction = actionQueue.poll();
            if (clientTickAction != null) {
                // Do the action
                System.out.println("Client tick: " + Microbot.getClient().getGameCycle() + "\n Action type: " + clientTickAction.getType() + "\n Client thread: " + Microbot.getClient().isClientThread());

                if (Objects.equals(clientTickAction.getType(), "move")) {

                    try {
                        var a = (GameActionsDeserializer.MoveAction) clientTickAction;
                        WorldPoint targetLocation = new WorldPoint(a.getPosition()[0], a.getPosition()[1], 0);
                        Rs2Walker.walkFastCanvas(targetLocation);

                    } catch (Exception e) {
                        System.out.println("Failed to mode to tile: " + clientTickAction + "\n " + e.getMessage());
                    }

                } else if (Objects.equals(clientTickAction.getType(), "inventory")) {
                    var a = (GameActionsDeserializer.InventoryAction) clientTickAction;


                    if (a.getActionType().equals("equip")) {

                        var itemModel = Rs2Inventory.getItemInSlot(a.getSlotIndex());
                        var equipAction = findMatchingEquipOption(itemModel.getInventoryActions());
                        if (equipAction != null) {

                            Rs2Inventory.interact(itemModel, equipAction);
                        }
                    } else {

                        Rs2Inventory.interact(Rs2Inventory.getItemInSlot(a.getSlotIndex()), a.getActionType());

                    }

                } else if (Objects.equals(clientTickAction.getType(), "attack")) {
                    var a = (GameActionsDeserializer.AttackAction) clientTickAction;

                    Rs2Player.attack(Rs2Player.getPlayer(a.getTarget()));

                } else if (Objects.equals(clientTickAction.getType(), "special")) {

                    //10485795

                    Microbot.getMouse().click(Rs2Widget.getWidget(10485795).getBounds());


                } else if (Objects.equals(clientTickAction.getType(), "prayer")) {
                    var a = (GameActionsDeserializer.PrayerAction) clientTickAction;
                    System.out.println("Attempting to activate the following prayers: " + a.getPrayer());


                    var prayer_to_activate = a.getPrayer();
                    Rs2Prayer.toggle(Rs2PrayerEnum.valueOf(prayer_to_activate));


                } else if (Objects.equals(clientTickAction.getType(), "CheckRecoil")) {

                    if (Rs2Equipment.isEquipped(2550, EquipmentInventorySlot.RING)) {

                        Rs2Equipment.interact(2550, "Check");
                        //Rs2Tab.switchToInventoryTab();
                    } else {
                        System.out.println("We do NOT have a recoil equipped");
                    }
                } else {
                    System.out.println("Non-implemented action: " + clientTickAction.getType());
                }
            }
        }, 0, 2, TimeUnit.MILLISECONDS);

        return true;
    }
}