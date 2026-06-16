package thaumicenergistics.client.gui.crafting;

import ae2.client.gui.me.crafting.GuiCraftingStatus;
import ae2.client.gui.style.GuiStyleManager;
import net.minecraft.entity.player.InventoryPlayer;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.container.crafting.ContainerCraftingStatusBridge;

/**
 * @author BrockWS
 */
public class GuiCraftingStatusBridge extends GuiCraftingStatus {

    public GuiCraftingStatusBridge(ContainerCraftingStatusBridge container, InventoryPlayer inventoryPlayer, IArcaneTerminalHost part) {
        super(container, inventoryPlayer, null, GuiStyleManager.loadStyleDoc("crafting_status"));
    }
}
