package net.runelite.client.plugins.microbot.flipperone.data;

import lombok.Data;

@Data
public class GESlot {
    private int slot_index;
    private int item_id;
    private String item_name;
    private String search_term;
    private boolean is_buy_offer;
    private int quantity;
    private int offer_price;
}