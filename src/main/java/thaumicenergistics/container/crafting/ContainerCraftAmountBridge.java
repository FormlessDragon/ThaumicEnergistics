package thaumicenergistics.container.crafting;

import ae2.container.implementations.ContainerCraftAmount;
import net.minecraft.entity.player.InventoryPlayer;
import thaumicenergistics.api.storage.IArcaneTerminalHost;

/**
 * @author BrockWS
 */
public class ContainerCraftAmountBridge extends ContainerCraftAmount {

    public ContainerCraftAmountBridge(InventoryPlayer ip, IArcaneTerminalHost te) {
        super(ip, te);
    }
}
