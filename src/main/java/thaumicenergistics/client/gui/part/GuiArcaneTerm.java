package thaumicenergistics.client.gui.part;

import ae2.api.config.ActionItems;
import ae2.client.gui.me.common.GuiMEStorage;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.ActionButton;
import ae2.core.AEConfig;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.container.part.ContainerArcaneTerm;

public class GuiArcaneTerm extends GuiMEStorage<ContainerArcaneTerm> {
    private float visAvailable = -1;
    protected float visRequired = -1;
    private float discount = 0f;

    public GuiArcaneTerm(ContainerArcaneTerm container, InventoryPlayer playerInventory) {
        this(container, playerInventory, getDefaultTerminalTitle(),
                GuiStyleManager.loadStyleDoc("/screens/terminals/crafting_terminal.json"));
    }

    public GuiArcaneTerm(ContainerArcaneTerm container, InventoryPlayer playerInventory, ITextComponent title, GuiStyle style) {
        super(container, playerInventory, title, style);

        ActionButton clearBtn = new ActionButton(ActionItems.S_STASH, container::clearCraftingGrid);
        clearBtn.setHalfSize(true);
        clearBtn.setDisableBackground(true);
        widgets.add("clearCraftingGrid", clearBtn);
    }

    public void setVisInfo(float chunkVis, float visRequired, float discount) {
        this.visAvailable = chunkVis;
        this.visRequired = visRequired;
        this.discount = discount;
    }

    protected ITextComponent getTerminalTitle() {
        return getDefaultTerminalTitle();
    }

    private static ITextComponent getDefaultTerminalTitle() {
        return new TextComponentTranslation("gui.thaumicenergistics.arcane_terminal");
    }

    @Override
    public void initGui() {
        super.initGui();
        this.container.setClearGridOnClose(AEConfig.instance().isClearGridOnClose());
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);

        this.drawVisInfo();

        if (this.discount > 0f) {
            this.fontRenderer.drawString(
                    ThEApi.instance().lang().guiVisDiscount().getLocalizedKey((int) (this.discount * 100)),
                    90,
                    this.ySize - 94,
                    4210752);
        }
    }

    protected void drawVisInfo() {
        this.fontRenderer.drawString(
                ThEApi.instance()
                        .lang()
                        .guiVisRequiredOutOf()
                        .getLocalizedKey(
                                getVisIfSet(this.visRequired),
                                (int) getVisIfSet(this.visAvailable)
                        ),
                35,
                this.ySize - 168,
                this.visRequired > this.visAvailable ? 0xFF0000 : 4210752);
    }

    protected float getVisIfSet(float vis) {
        return vis > -1 ? vis : 0;
    }
}
