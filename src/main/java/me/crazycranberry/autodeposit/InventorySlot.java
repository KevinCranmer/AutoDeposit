package me.crazycranberry.autodeposit;

import org.bukkit.inventory.ItemStack;

public class InventorySlot {
    private final int index;
    private final ItemStack item;

    private InventorySlot(int index, ItemStack item) {
        this.index = index;
        this.item = item;
    }

    public static InventorySlot of(int index, ItemStack item) {
        return new InventorySlot(index, item);
    }

    public int index() {
        return index;
    }

    public ItemStack item() {
        return item;
    }
}
