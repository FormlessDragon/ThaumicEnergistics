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
import thaumicenergistics.container.part.ArcaneTerminalVisState;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.core.ThEFeatures;

public class GuiArcaneTerm extends GuiMEStorage<ContainerArcaneTerm> {
    public static final String STYLE_PATH = "/screens/terminals/thaumicenergistics_arcane_terminal.json";

    public GuiArcaneTerm(ContainerArcaneTerm container, InventoryPlayer playerInventory) {
        this(container, playerInventory, getDefaultTerminalTitle(),
                GuiStyleManager.loadStyleDoc(GuiArcaneTerm.STYLE_PATH));
    }

    public GuiArcaneTerm(ContainerArcaneTerm container, InventoryPlayer playerInventory, ITextComponent title, GuiStyle style) {
        super(container, playerInventory, title, style);

        ActionButton clearBtn = new ActionButton(ActionItems.S_STASH, container::clearCraftingGrid);
        clearBtn.setHalfSize(true);
        clearBtn.setDisableBackground(true);
        widgets.add("clearCraftingGrid", clearBtn);
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

        ArcaneTerminalVisState visState = this.container.getVisState();
        if (visState.getDiscount() > 0f) {
            this.fontRenderer.drawString(
                    ThEFeatures.instance().lang().guiVisDiscount().getLocalizedKey((int) (visState.getDiscount() * 100)),
                    90,
                    this.ySize - 94,
                    4210752);
        }
    }

    protected void drawVisInfo() {
        ArcaneTerminalVisState visState = this.container.getVisState();
        this.fontRenderer.drawString(
                ThEFeatures.instance()
                        .lang()
                        .guiVisRequiredOutOf()
                        .getLocalizedKey(
                                getVisIfSet(visState.getVisRequired()),
                                (int) getVisIfSet(visState.getVisAvailable())
                        ),
                35,
                this.ySize - 168,
                visState.getVisRequired() > visState.getVisAvailable() ? 0xFF0000 : 4210752);
    }

    protected float getVisIfSet(float vis) {
        return vis > -1 ? vis : 0;
    }
}
