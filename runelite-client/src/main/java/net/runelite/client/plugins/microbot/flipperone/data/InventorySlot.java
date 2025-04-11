package net.runelite.client.plugins.microbot.flipperone.data;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class InventorySlot {
    @SerializedName("item_id")
    private int itemId;
    private int quantity;
}
