package thaumicenergistics.container.crafting;

import ae2.container.implementations.ContainerCraftConfirm;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.IThreadListener;
import thaumicenergistics.client.gui.GuiHandler;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.part.PartSharedTerminal;

/**
 * @author BrockWS
 * @author Alex811
 */
public class ContainerCraftConfirmBridge extends ContainerCraftConfirm {

    private PartSharedTerminal part;
    private final InventoryPlayer inventoryPlayer;

    public ContainerCraftConfirmBridge(InventoryPlayer ip, PartSharedTerminal te) {
        super(ip, te);
        this.part = te;
        this.inventoryPlayer = ip;
    }

    @Override
    public void startJob() {
        super.startJob();
        ((IThreadListener) part.getLocation().getWorld()).addScheduledTask(() ->
                GuiHandler.openGUI(ModGUIs.values()[this.part.getGui().ordinal()], this.inventoryPlayer.player, this.part.getLocation().getPos(), this.part.side));
    }
}
