package thaumicenergistics.container.crafting;

import ae2.container.implementations.ContainerCraftingStatus;
import net.minecraft.entity.player.InventoryPlayer;
import thaumicenergistics.api.storage.IArcaneTerminalHost;

/**
 * @author BrockWS
 */
public class ContainerCraftingStatusBridge extends ContainerCraftingStatus {
    public ContainerCraftingStatusBridge(InventoryPlayer ip, IArcaneTerminalHost te) {
        super(ip, te);
    }
}
