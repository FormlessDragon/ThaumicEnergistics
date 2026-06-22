package thaumicenergistics.util.inventory;

import net.minecraftforge.items.IItemHandler;

/**
 * Inventory contract for ThE block tiles that expose typed internal inventories to production code.
 * <p>
 * The named {@link IItemHandler} lookup remains only as a legacy Forge item-handler bridge for callers that cannot
 * consume the typed Supergiant inventory API directly.
 *
 * @author Alex811
 */
public interface IThEInvTile {

    /**
     * Returns the tile's typed knowledge-core inventory for container slots and tile logic.
     */
    ThEInternalInventory getCoreInventory();

    /**
     * Returns the tile's typed upgrade inventory for container slots and tile logic.
     */
    ThEUpgradeInventory getUpgradeInventory();

    /**
     * Legacy named Forge item-handler bridge retained for external ABI callers.
     */
    IItemHandler getInventoryByName(String name);
}
