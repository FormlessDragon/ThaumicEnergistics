package thaumicenergistics.container.crafting;

import ae2.container.implementations.ContainerCraftConfirm;
import net.minecraft.entity.player.InventoryPlayer;
import thaumicenergistics.api.storage.IArcaneTerminalHost;

/**
 * @author BrockWS
 * @author Alex811
 */
public class ContainerCraftConfirmBridge extends ContainerCraftConfirm {

    public ContainerCraftConfirmBridge(InventoryPlayer ip, IArcaneTerminalHost te) {
        super(ip, te);
    }
}
