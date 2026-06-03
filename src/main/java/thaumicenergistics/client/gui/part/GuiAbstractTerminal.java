package thaumicenergistics.client.gui.part;

import ae2.api.implementations.IPowerChannelState;
import thaumicenergistics.client.gui.GuiConfigurable;
import thaumicenergistics.client.gui.helpers.MERepo;
import thaumicenergistics.container.ContainerBaseTerminal;
import thaumicenergistics.container.slot.SlotME;

/**
 * @author BrockWS
 * @author Alex811
 */
public abstract class GuiAbstractTerminal extends GuiConfigurable implements IPowerChannelState {

    protected ContainerBaseTerminal container;
    protected MERepo repo;

    public GuiAbstractTerminal(ContainerBaseTerminal container) {
        super(container);
        this.container = container;
    }

    public MERepo getRepo() {
        return this.repo;
    }

    @Override
    public boolean isPowered() {
        return this.container.getPart().isPowered();
    }

    @Override
    public boolean isActive() {
        return this.container.getPart().isActive();
    }

    @Override
    protected void drawSlotOverlay(net.minecraft.inventory.Slot slot) {
        super.drawSlotOverlay(slot);
        if (slot instanceof SlotME && !this.isActive())
            drawRect(slot.xPos, slot.yPos, slot.xPos + 16, slot.yPos + 16, 0x66111111);
    }
}
