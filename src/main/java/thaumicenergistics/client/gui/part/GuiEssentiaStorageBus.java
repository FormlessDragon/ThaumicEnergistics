package thaumicenergistics.client.gui.part;

import ae2.api.config.ActionItems;
import ae2.api.config.Settings;
import ae2.core.localization.GuiText;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.client.gui.component.GuiImgButton;
import thaumicenergistics.container.part.ContainerEssentiaStorageBus;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketEssentiaFilterAction;
import thaumicenergistics.network.packets.PacketOpenGUI;

import static thaumicenergistics.config.ThESettings.actions;

/**
 * @author BrockWS
 * @author Alex811
 */
public class GuiEssentiaStorageBus extends GuiSharedEssentiaBus {

    private GuiImgButton clearButton;
    private GuiImgButton partitionButton;
    private GuiButton priorityButton;

    public GuiEssentiaStorageBus(ContainerEssentiaStorageBus container) {
        super(container);
        this.ySize = 251;
        this.mainBackgroundHeight = 251;
        this.upgradeBackgroundHeight += 18;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.priorityButton = new GuiButton(0, this.guiLeft + 154, this.guiTop, 24, 20, GuiText.Priority.getLocal());
        this.clearButton = new GuiImgButton(this.getGuiLeft() - 18, this.getGuiTop() + 8, actions(), ActionItems.CLOSE);
        this.partitionButton = new GuiImgButton(this.getGuiLeft() - 18, this.getGuiTop() + 28, actions(), ActionItems.COG);
        this.addButton(this.priorityButton);
        this.addButton(this.clearButton);
        this.addButton(this.partitionButton);
        this.addButton(new GuiImgButton(this.getGuiLeft() - 18, this.getGuiTop() + 48, Settings.ACCESS, this.container.getConfigManager().getSetting(Settings.ACCESS)));
        this.addButton(new GuiImgButton(this.getGuiLeft() - 18, this.getGuiTop() + 68, Settings.STORAGE_FILTER, this.container.getConfigManager().getSetting(Settings.STORAGE_FILTER)));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        this.fontRenderer.drawString(ThEApi.instance().lang().guiEssentiaStorageBus().getLocalizedKey(), 8, 6, 4210752);
    }

    @Override
    protected int getSlotBackgroundX() {
        return 7;
    }

    @Override
    protected int getSlotBackgroundY() {
        return 28;
    }

    @Override
    protected ResourceLocation getGuiBackground() {
        return new ResourceLocation(ModGlobals.MOD_ID_AE2, "textures/guis/storagebus.png");
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == this.priorityButton)
            PacketHandler.sendToServer(new PacketOpenGUI(ModGUIs.AE2_PRIORITY, this.container.getPart().getLocation().getPos(), this.container.getPart().side));
        else if (button == this.clearButton)
            PacketHandler.sendToServer(new PacketEssentiaFilterAction(this.container.getPart(), PacketEssentiaFilterAction.ACTION.CLEAR));
        else if (button == this.partitionButton)
            PacketHandler.sendToServer(new PacketEssentiaFilterAction(this.container.getPart(), PacketEssentiaFilterAction.ACTION.PARTITION));
        else super.actionPerformed(button);
    }
}
