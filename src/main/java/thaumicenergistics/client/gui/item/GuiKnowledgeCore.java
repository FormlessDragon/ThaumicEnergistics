package thaumicenergistics.client.gui.item;

import ae2.client.Point;
import ae2.client.gui.AEBaseGui;
import ae2.client.gui.Icon;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.UpgradesPanel;
import ae2.container.SlotSemantics;
import ae2.api.upgrades.Upgrades;
import ae2.core.localization.GuiText;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.ITextComponent;
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.item.ContainerKnowledgeCore;
import thaumicenergistics.core.definitions.ThEItems;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * @author Alex811
 */
public class GuiKnowledgeCore extends AEBaseGui<ContainerKnowledgeCore> {
    private static final String STYLE_PATH = "/screens/thaumicenergistics_knowledge_core.json";
    private static final String ACTION_WIDGET_ID = "knowledgeCoreAction";
    private static final int PREVIOUS_PAGE_BUTTON_ID = 1;
    private static final int NEXT_PAGE_BUTTON_ID = 2;
    private GuiButton previousPageButton;
    private GuiButton nextPageButton;

    public GuiKnowledgeCore(ContainerKnowledgeCore container) {
        super(container, container.getPlayerInventory(), GuiStyleManager.loadStyleDoc(STYLE_PATH));
        this.setTextContent(AEBaseGui.TEXT_ID_DIALOG_TITLE,
            ThEItems.KNOWLEDGE_CORE.stack(1).getTextComponent());
        this.widgets.add("upgrades", UpgradesPanel.create(
            this.widgets,
            container.getSlots(SlotSemantics.UPGRADE),
            this::getCompatibleUpgrades));
    }

    @Override
    public void initGui() {
        super.initGui();
        this.previousPageButton = new GuiButton(PREVIOUS_PAGE_BUTTON_ID, this.guiLeft + 118, this.guiTop + 2, 16, 12, "<");
        this.nextPageButton = new GuiButton(NEXT_PAGE_BUTTON_ID, this.guiLeft + 136, this.guiTop + 2, 16, 12, ">");
        this.buttonList.add(this.previousPageButton);
        this.buttonList.add(this.nextPageButton);
        this.updatePageButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == PREVIOUS_PAGE_BUTTON_ID) {
            this.container.requestPreviousPage();
            return;
        }
        if (button.id == NEXT_PAGE_BUTTON_ID) {
            this.container.requestNextPage();
            return;
        }
        super.actionPerformed(button);
    }

    @Override
    protected void handleMouseClick(@Nullable Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot == null || this.container.getSlotSemantic(slot) != ThESlotSemantics.KNOWLEDGE_CORE || slotId >= 9) {
            super.handleMouseClick(slot, slotId, mouseButton, type);
            return;
        }
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
            case KNOWLEDGE_CORE_MANAGE:
                break;
        }
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.updatePageButtons();
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
            case KNOWLEDGE_CORE_MANAGE:
                break;
        }
    }

    private void blitActionIcon(Icon icon) {
        Point position = Objects.requireNonNull(this.style, "style")
            .getWidget(ACTION_WIDGET_ID)
            .resolve(this.getBounds(false));
        icon.getBlitter().dest(position.x(), position.y()).blit();
    }

    private void updatePageButtons() {
        if (this.previousPageButton != null) {
            this.previousPageButton.enabled = this.container.hasPreviousPage();
        }
        if (this.nextPageButton != null) {
            this.nextPageButton.enabled = this.container.hasNextPage();
        }
    }

    private List<ITextComponent> getCompatibleUpgrades() {
        var upgradeLines = Upgrades.getTooltipLinesForInventory(this.container.getUpgradeInventory());
        var lines = new ObjectArrayList<ITextComponent>(upgradeLines.size() + 1);
        lines.add(GuiText.CompatibleUpgrades.text());
        lines.addAll(upgradeLines);
        return lines;
    }
}
