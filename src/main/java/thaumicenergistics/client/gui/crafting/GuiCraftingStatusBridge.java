package thaumicenergistics.client.gui.crafting;

import ae2.client.gui.me.crafting.GuiCraftingStatus;
import ae2.client.gui.style.GuiStyleManager;
import net.minecraft.entity.player.InventoryPlayer;
import thaumicenergistics.container.crafting.ContainerCraftingStatusBridge;
import thaumicenergistics.part.PartSharedTerminal;

/**
 * @author BrockWS
 */
public class GuiCraftingStatusBridge extends GuiCraftingStatus {

    public GuiCraftingStatusBridge(ContainerCraftingStatusBridge container, InventoryPlayer inventoryPlayer, PartSharedTerminal part) {
        super(container, inventoryPlayer, null, GuiStyleManager.loadStyleDoc("crafting_status"));
    }
}
