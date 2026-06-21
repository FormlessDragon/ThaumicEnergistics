package thaumicenergistics.client.gui.item;

import ae2.client.Point;
import ae2.client.gui.AEBaseGui;
import ae2.client.gui.Icon;
import ae2.client.gui.style.GuiStyleManager;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.util.text.TextComponentString;
import thaumicenergistics.container.item.ContainerKnowledgeCore;
import thaumicenergistics.core.ThEFeatures;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author Alex811
 */
public class GuiKnowledgeCore extends AEBaseGui<ContainerKnowledgeCore> {
    private static final String STYLE_PATH = "/screens/thaumicenergistics_knowledge_core.json";
    private static final String ACTION_WIDGET_ID = "knowledgeCoreAction";

    public GuiKnowledgeCore(ContainerKnowledgeCore container) {
        super(container, container.getPlayerInventory(), GuiStyleManager.loadStyleDoc(STYLE_PATH));
        this.setTextContent(AEBaseGui.TEXT_ID_DIALOG_TITLE,
                new TextComponentString(ThEFeatures.instance().lang().itemKnowledgeCore().getLocalizedKey()));
    }

    @Override
    protected void handleMouseClick(@Nullable Slot slot, int slotId, int mouseButton, ClickType type) {
        // Send to server for processing
        switch (container.getGUIAction()) {
            case KNOWLEDGE_CORE_ADD:
                if (slotId > -1) container.playWriteSound(mc.player);
                container.requestAddRecipe(slotId);
                break;
            case KNOWLEDGE_CORE_DEL:
                if (slotId > -1) container.playWriteSound(mc.player);
                container.requestDeleteRecipe(slotId);
                break;
            case KNOWLEDGE_CORE_VIEW:
                container.requestViewRecipe(slotId);
                break;
        }
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        switch (this.container.getGUIAction()) {
            case KNOWLEDGE_CORE_ADD:
                this.blitActionIcon(Icon.ARROW_DOWN);
                break;
            case KNOWLEDGE_CORE_DEL:
                this.blitActionIcon(Icon.CLEAR);
                break;
            case KNOWLEDGE_CORE_VIEW:
                this.blitActionIcon(Icon.VIEW_MODE_STORED);
                break;
        }
    }

    private void blitActionIcon(Icon icon) {
        Point position = Objects.requireNonNull(this.style, "style")
                .getWidget(ACTION_WIDGET_ID)
                .resolve(this.getBounds(false));
        icon.getBlitter().dest(position.x(), position.y()).blit();
    }
}
