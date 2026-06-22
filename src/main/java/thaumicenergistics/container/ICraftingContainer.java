package thaumicenergistics.container;

import ae2.api.inventories.InternalInventory;
import net.minecraft.item.ItemStack;

/**
 * @author BrockWS
 */
public interface ICraftingContainer {

    void onMatrixChanged();

    /**
     * Calculate the amount we can craft
     *
     * @param amount Amount requested (Normally either 1 or Integer.MAX_VALUE)
     * @return The max amount we can craft
     */
    int tryCraft(int amount);

    ItemStack onCraft(ItemStack crafted);

    /**
     * Returns the typed Supergiant internal inventory backing the arcane crafting matrix.
     */
    InternalInventory getCraftingInventory();

    /**
     * Returns the typed Supergiant internal inventory backing the visible crafting result.
     */
    InternalInventory getCraftingResultInventory();

}
