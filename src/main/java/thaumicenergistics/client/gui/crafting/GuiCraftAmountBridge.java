package thaumicenergistics.client.gui.crafting;

import ae2.client.gui.me.crafting.GuiCraftAmount;
import ae2.client.gui.style.GuiStyleManager;
import net.minecraft.entity.player.InventoryPlayer;
import thaumicenergistics.container.crafting.ContainerCraftAmountBridge;
import thaumicenergistics.part.PartSharedTerminal;

/**
 * @author BrockWS
 */
public class GuiCraftAmountBridge extends GuiCraftAmount {

    public GuiCraftAmountBridge(ContainerCraftAmountBridge container, InventoryPlayer inventoryPlayer, PartSharedTerminal part) {
        super(container, inventoryPlayer, null, GuiStyleManager.loadStyleDoc("craft_amount"));
    }
}
