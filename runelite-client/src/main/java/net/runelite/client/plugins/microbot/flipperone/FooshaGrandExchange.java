package net.runelite.client.plugins.microbot.flipperone;

import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.grandexchange.GrandExchangeSlots;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.apache.commons.lang3.tuple.Pair;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;
import static net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange.*;

public class FooshaGrandExchange {
    public static boolean buyItemFromSlot(GrandExchangeSlots slot, String itemName, String searchTerm, int price, int quantity) {
        try {
            if (useGrandExchange()) return false;



            Widget buyOffer = getOfferBuyButton(slot);
            if (buyOffer == null) return false;

            Rs2Widget.clickWidgetFast(buyOffer);
            sleepUntil(Rs2GrandExchange::isOfferTextVisible, 5000);
            sleepUntil(() -> Rs2Widget.hasWidget("What would you like to buy?"));
            Rs2Keyboard.typeString(searchTerm);
            sleepUntil(() -> !Rs2Widget.hasWidget("Start typing the name"), 5000); //GE Search Results
            sleep(1200);
            Pair<Widget, Integer> itemResult = getSearchResultWidget(itemName);
            if (itemResult != null) {
                Rs2Widget.clickWidgetFast(itemResult.getLeft(), itemResult.getRight(), 1);
                sleepUntil(() -> getPricePerItemButton_X() != null);
            }
            Widget pricePerItemButtonX = getPricePerItemButton_X();
            if (pricePerItemButtonX != null) {
                System.out.println("tried to click widget");
                sleep(2000);
                Microbot.getMouse().click(pricePerItemButtonX.getBounds());
                Microbot.getMouse().click(pricePerItemButtonX.getBounds());
                sleepUntil(() -> Rs2Widget.getWidget(162, 41) != null, 5000); //GE Enter Price
                sleep(1000);
                Rs2Keyboard.typeString(Integer.toString(price));
                Rs2Keyboard.enter();
                sleep(2000);
                setQuantity(quantity);
                confirm();
                return true;
            } else {
                System.out.println("unable to find widget setprice.");
            }

            return false;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return false;
    }

    public static boolean sellItemInSlot(GrandExchangeSlots slot, String itemName, int quantity, int price) {
        try {
            Widget sellOffer = getOfferSellButton(slot);

            if (sellOffer == null) return false;

            Microbot.getMouse().click(sellOffer.getBounds());
            sleepUntil(Rs2GrandExchange::isOfferTextVisible, 5000);
            Rs2Inventory.interact(itemName, "Offer");
            sleepUntil(() -> Rs2Widget.hasWidget("actively traded price"));
            sleep(300, 600);
            Widget pricePerItemButtonX = getPricePerItemButton_X();
            if (pricePerItemButtonX != null) {
                Microbot.getMouse().click(pricePerItemButtonX.getBounds());
                sleepUntil(() -> Rs2Widget.getWidget(162, 41) != null, 5000); //GE Enter Price
                sleep(1000);
                Rs2Keyboard.typeString(Integer.toString(price));
                Rs2Keyboard.enter();
                sleep(300, 500);
                setQuantity(quantity);
                Microbot.getMouse().click(getConfirm().getBounds());
                sleepUntil(() -> !isOfferTextVisible());
                return true;
            } else {
                System.out.println("unable to find widget setprice.");
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return false;
    }
}
