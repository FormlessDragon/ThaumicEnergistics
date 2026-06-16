package thaumicenergistics.client.gui.crafting;

import ae2.client.gui.me.crafting.GuiCraftConfirm;
import ae2.client.gui.style.GuiStyleManager;
import net.minecraft.entity.player.InventoryPlayer;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.container.crafting.ContainerCraftConfirmBridge;

/**
 * @author BrockWS
 */
public class GuiCraftConfirmBridge extends GuiCraftConfirm {

    public GuiCraftConfirmBridge(ContainerCraftConfirmBridge container, InventoryPlayer inventoryPlayer, IArcaneTerminalHost part) {
        super(container, inventoryPlayer, null, GuiStyleManager.loadStyleDoc("craft_confirm"));
    }
}
