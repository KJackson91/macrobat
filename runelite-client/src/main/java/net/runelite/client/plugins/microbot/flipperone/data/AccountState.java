package net.runelite.client.plugins.microbot.flipperone.data;

import lombok.Data;
import java.util.List;

@Data
public class AccountState {
    private List<GESlot> slots;
    private List<InventorySlot> inventory;
}