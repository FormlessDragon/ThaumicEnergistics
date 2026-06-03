package thaumicenergistics.client.gui.crafting;

import ae2.client.gui.me.crafting.GuiCraftConfirm;
import ae2.client.gui.style.GuiStyleManager;
import net.minecraft.entity.player.InventoryPlayer;
import thaumicenergistics.container.crafting.ContainerCraftConfirmBridge;
import thaumicenergistics.part.PartSharedTerminal;

/**
 * @author BrockWS
 */
public class GuiCraftConfirmBridge extends GuiCraftConfirm {

    public GuiCraftConfirmBridge(ContainerCraftConfirmBridge container, InventoryPlayer inventoryPlayer, PartSharedTerminal part) {
        super(container, inventoryPlayer, null, GuiStyleManager.loadStyleDoc("craft_confirm"));
    }
}
