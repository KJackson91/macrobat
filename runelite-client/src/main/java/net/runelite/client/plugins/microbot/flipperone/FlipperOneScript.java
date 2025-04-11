package net.runelite.client.plugins.microbot.flipperone;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.flipperone.data.*;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeSlots;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.util.ImageCapture;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.flipperone.FooshaGrandExchange.buyItemFromSlot;
import static net.runelite.client.plugins.microbot.flipperone.FooshaGrandExchange.sellItemInSlot;

public class FlipperOneScript extends Script {

    @Inject
    private ImageCapture imageCapture;
    private int loop_delay = 1000 * 8; //8 sec
    private HttpClient client;
    private Gson gson;

    public static String version = "7.3.420.73";

    public FlipperAccount currentAccount;
    AtomicInteger index;

    public List<FlipperAccount> accounts;
    private void init(FlipperOneConfig config) {
        gson = new Gson();
        client = HttpClient.newHttpClient();
        accounts = loadAccounts(config.accounts());
        index = new AtomicInteger();
    }



    public List<FlipperAccount> loadAccounts(String filePath) {
        try {
            return Files.lines(Paths.get(filePath))
                    .map(line -> {
                        String[] parts = line.split(":");
                        FlipperAccount account = new FlipperAccount();
                        account.setUsername(parts[0].trim());
                        account.setPassword(parts[1].trim());
                        account.setWorld(Integer.parseInt(parts[2].trim()));
                        return account;
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load accounts from " + filePath, e);
        }
    }

    public List<InventorySlot> getInventory(){
        List<InventorySlot> invItems = new ArrayList<>();
        for (var i: Rs2Inventory.items()
        ) {
            InventorySlot invSlot = new InventorySlot();
//            if(i.isNoted()){
//                invSlot.setItemId(i.getId() - 1);
//            } else {
                invSlot.setItemId(i.getId());
//            }

            invSlot.setQuantity(i.getQuantity());
            invItems.add(invSlot);
        }
        return invItems;
    }

    public List<GESlot> getGESlots() {
        List<GESlot> slots = new ArrayList<>();

        for (var x : GrandExchangeSlots.values()) {
            var slotWidget = Rs2GrandExchange.getSlot(x);
            var slotStatus = Objects.requireNonNull(Objects.requireNonNull(slotWidget).getChild(16)).getText();

            try {
                SlotStatus status = SlotStatus.valueOf(slotStatus.toUpperCase());
                GESlot slot = new GESlot();
                slot.setSlot_index(x.ordinal());
                switch (status) {
                    case BUY:
                        slot.set_buy_offer(true);
                        slot.setQuantity(1);
                        slot.setItem_id(Objects.requireNonNull(Objects.requireNonNull(slotWidget).getChild(18)).getItemId());
                    {
                        var offerPriceString = Objects.requireNonNull(Objects.requireNonNull(slotWidget).getChild(25)).getText();
                        var offerValue = offerPriceString.split(" ")[0];
                        var removeCommas = offerValue.replace(",", "");
                        var intValue = Integer.parseInt(removeCommas);
                        slot.setOffer_price(intValue);
                    }

                    break;
                    case SELL:
                        slot.set_buy_offer(false);
                        slot.setQuantity(1);
                        slot.setItem_id(Objects.requireNonNull(Objects.requireNonNull(slotWidget).getChild(18)).getItemId());

                    {
                        var offerPriceString = Objects.requireNonNull(Objects.requireNonNull(slotWidget).getChild(25)).getText();
                        var offerValue = offerPriceString.split(" ")[0];
                        var removeCommas = offerValue.replace(",", "");
                        var intValue = Integer.parseInt(removeCommas);
                        slot.setOffer_price(intValue);
                    }
                    break;
                    case EMPTY:
                        slot.set_buy_offer(false);
                        slot.setQuantity(0);
                        slot.setItem_id(-1);
                        slot.setOffer_price(0);
                        break;
                }


                slots.add(slot);
            } catch (IllegalArgumentException e) {
                System.out.println("Unexpected slotStatus value: " + slotStatus);
            }
        }

        return slots;
    }

    public boolean run(FlipperOneConfig config, FlipperOnePlugin plug) {
        Microbot.enableAutoRunOn = false;
        init(config);

        System.out.println("Loaded " + accounts.size() + " accounts");

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            currentAccount = accounts.get(index.get() % accounts.size());
            System.out.println("=== " + currentAccount.getUsername() + ":" + currentAccount.getPassword());
            index.getAndIncrement();
            if (Microbot.getClient().getGameState() == GameState.LOGIN_SCREEN) {
                System.out.println("Trying to log in");
                new FooshaLogin(currentAccount.getUsername(), currentAccount.getPassword(), currentAccount.getWorld());
                System.out.println("Past login init");
                sleepUntil(() -> Rs2Widget.getWidget(378, 72) != null, 20000);
                System.out.println("Past sleepuntill");
                var clicked = Rs2Widget.clickWidget(378, 72);
                System.out.println("Clicked the button?: " + clicked);
                sleep(3000);
            }


            // Always update offers at least once
            updateOffers();

            while (!Rs2Widget.getWidget(465, 6).getDynamicChildren()[1].isSelfHidden()) {
                System.out.println("An offer filled before we logged out");
                updateOffers();
            }

            System.out.println("No offers filled");
            System.out.println("Logging out then sleeping for  " + loop_delay/1000 + "s");
            plug.members_update_latch = false;

            sleepUntil(Rs2Tab::switchToLogout);
            var bounds = Rs2Widget.getWidget(182, 8).getBounds();
            Microbot.getMouse().click(bounds);
        }, 10, loop_delay ,TimeUnit.MILLISECONDS);
        return true;

    }

    private void updateOffers() {
        AccountState accountState = new AccountState();
        Rs2GrandExchange.openExchange();
        Rs2GrandExchange.collectToInventory();

        try {
            accountState.setInventory(getInventory());
            accountState.setSlots(getGESlots());

            var newOffers = pollNewOffers(accountState);

            if (newOffers.size() != 0) {
                var needsUpdated = findDifferentIndices(accountState.getSlots(), newOffers);
                if (needsUpdated.size() > 0){
                    System.out.println("Need to update " + needsUpdated.size() + " slots");
                    abortUpdatedOffers(needsUpdated, accountState);
                    placeNewOffers(needsUpdated, accountState, newOffers);
                }

            } else {
                System.out.println("Got 0 results back, probably 500");
            }


        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    private List<GESlot> pollNewOffers(AccountState accountState) {
        var accountStateJson = gson.toJson(accountState);
        System.out.println("Requesting new offers: " + accountStateJson);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://192.168.1.193:8000/accounts/" + Rs2Player.getLocalPlayer().getName() + "/state"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(accountStateJson))
                .build();

        try {
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 500) {
                return new ArrayList<>();
            } else {
                JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
                return gson.fromJson(jsonObject.get("recommended_slots"),
                        new TypeToken<List<GESlot>>(){}.getType());


            }

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void placeNewOffers(List<Integer> needsUpdated, AccountState accountState, List<GESlot> reccommended_slots) {
        for (var geSlotIndex : needsUpdated
        ) {
            //Now we make needed offers.
            var currentGESlot = GrandExchangeSlots.values()[geSlotIndex];
            var slotCurrentStatus = accountState.getSlots().get(geSlotIndex);
            var slotTargetStatus = reccommended_slots.get(geSlotIndex);

            if (slotTargetStatus.getItem_id() != -1) {
                System.out.println("Item " + slotTargetStatus.getItem_name());
                if (slotTargetStatus.is_buy_offer()) {
                    System.out.println("We are a buy offer");
                    buyItemFromSlot(currentGESlot, slotTargetStatus.getItem_name(), slotTargetStatus.getSearch_term(), slotTargetStatus.getOffer_price(), slotCurrentStatus.getQuantity());
                } else {
                    System.out.println("We are a sell offer");
                    sellItemInSlot(currentGESlot, slotTargetStatus.getItem_name(), slotTargetStatus.getQuantity(), slotTargetStatus.getOffer_price());
                }
            }
        }
    }

    private void abortUpdatedOffers(List<Integer> needsUpdated, AccountState accountState) {
        for (var geSlotIndex : needsUpdated
        ) {

            // First we do all needed abortions. My slots my choice.
            var currentGESlot = GrandExchangeSlots.values()[geSlotIndex];
            var slotCurrentStatus = accountState.getSlots().get(geSlotIndex);
            var slotWidget = Rs2GrandExchange.getSlot(currentGESlot);
            if (slotCurrentStatus.getItem_id() != -1) {
                // Slot is currently not empty, but there is a diff. We abort
                System.out.println("We need to abort");
                Microbot.doInvoke(new NewMenuEntry("Abort offer", 2, slotWidget.getId(), MenuAction.CC_OP.getId(), 2, -1, ""), new Rectangle(1, 1, Microbot.getClient().getCanvasWidth(), Microbot.getClient().getCanvasHeight()));
                sleep(1000);
                Rs2GrandExchange.collectToInventory();
            }
        }
    }


    public List<Integer> findDifferentIndices(List<GESlot> oldSlots, List<GESlot> newSlots) {
        List<Integer> differentIndices = new ArrayList<>();

        // Ensure we don't go out of bounds by using the shorter list length
        int minSize = Math.min(oldSlots.size(), newSlots.size());

        for (int i = 0; i < minSize; i++) {
            GESlot oldSlot = oldSlots.get(i);
            GESlot newSlot = newSlots.get(i);

            // Compare all relevant fields
            if (!slotsAreEqual(oldSlot, newSlot)) {
                differentIndices.add(i);
            }
        }

        // If lists are different sizes, add remaining indices
        for (int i = minSize; i < Math.max(oldSlots.size(), newSlots.size()); i++) {
            differentIndices.add(i);
        }

        return differentIndices;
    }

    private boolean slotsAreEqual(GESlot slot1, GESlot slot2) {
        return slot1.getSlot_index() == slot2.getSlot_index() &&
                slot1.getItem_id() == slot2.getItem_id() &&
                slot1.is_buy_offer() == slot2.is_buy_offer() &&
                slot1.getQuantity() == slot2.getQuantity() &&
                slot1.getOffer_price() == slot2.getOffer_price();
    }


    @Override
    public void shutdown() {
        super.shutdown();
    }

}