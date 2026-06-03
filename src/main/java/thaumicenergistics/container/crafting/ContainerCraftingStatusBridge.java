package thaumicenergistics.container.crafting;

import ae2.container.implementations.ContainerCraftingStatus;
import net.minecraft.entity.player.InventoryPlayer;
import thaumicenergistics.part.PartSharedTerminal;

/**
 * @author BrockWS
 */
public class ContainerCraftingStatusBridge extends ContainerCraftingStatus {
    public ContainerCraftingStatusBridge(InventoryPlayer ip, PartSharedTerminal te) {
        super(ip, te);
    }
}
