package thaumicenergistics.client.gui.part;

import ae2.api.config.ActionItems;
import ae2.client.gui.me.common.GuiMEStorage;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.ActionButton;
import ae2.core.AEConfig;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;
import thaumicenergistics.container.part.ArcaneTerminalVisState;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.core.definitions.GuiText;

public class GuiArcaneTerm extends GuiMEStorage<ContainerArcaneTerm> {
    public static final String STYLE_PATH = "/screens/terminals/thaumicenergistics_arcane_terminal.json";

    public GuiArcaneTerm(ContainerArcaneTerm container, InventoryPlayer playerInventory) {
        this(container, playerInventory, GuiText.arcane_terminal.text(), GuiStyleManager.loadStyleDoc(GuiArcaneTerm.STYLE_PATH));
    }

    public GuiArcaneTerm(ContainerArcaneTerm container, InventoryPlayer playerInventory, ITextComponent title, GuiStyle style) {
        super(container, playerInventory, title, style);

        ActionButton clearBtn = new ActionButton(ActionItems.S_STASH, container::clearCraftingGrid);
        clearBtn.setHalfSize(true);
        clearBtn.setDisableBackground(true);
        widgets.add("clearCraftingGrid", clearBtn);
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

        ArcaneTerminalVisState visState = this.container.getVisState();
        if (visState.getDiscount() > 0f) {
            this.fontRenderer.drawString(
                    GuiText.vis_discount.getLocal((int) (visState.getDiscount() * 100)),
                    90,
                    this.ySize - 94,
                    4210752);
        }
    }

    protected void drawVisInfo() {
        ArcaneTerminalVisState visState = this.container.getVisState();
        this.fontRenderer.drawString(
                GuiText.vis_required_out_of.getLocal(
                    getVisIfSet(visState.getVisRequired()),
                    (int) getVisIfSet(visState.getVisAvailable())
                ),
                80,
                this.ySize - 178,
                visState.getVisRequired() > visState.getVisAvailable() ? 0xFF0000 : 4210752);
    }

    protected float getVisIfSet(float vis) {
        return vis > -1 ? vis : 0;
    }
}
