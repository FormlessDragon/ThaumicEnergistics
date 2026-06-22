package thaumicenergistics.client.gui.part;

import ae2.client.gui.Icon;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.IconButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.core.ThEFeatures;
import thaumicenergistics.items.ItemKnowledgeCore;
import thaumicenergistics.util.KnowledgeCoreUtil;

/**
 * @author Alex811
 */
public class GuiArcaneInscriber extends GuiArcaneTerm {

    private final KnowledgeCoreButton coreAddButton;
    private final KnowledgeCoreButton coreDelButton;
    private final KnowledgeCoreButton coreViewButton;
    private final ContainerArcaneInscriber inscriberContainer;

    public GuiArcaneInscriber(ContainerArcaneInscriber container, InventoryPlayer playerInventory) {
        super(container, playerInventory,
                new TextComponentTranslation("gui.thaumicenergistics.arcane_inscriber"),
                GuiStyleManager.loadStyleDoc(GuiArcaneTerm.STYLE_PATH));
        this.inscriberContainer = container;

        this.coreAddButton = new KnowledgeCoreButton(Icon.ARROW_DOWN, this::requestKnowledgeCoreAddIfAllowed);
        this.coreDelButton = new KnowledgeCoreButton(Icon.CLEAR, this::requestKnowledgeCoreDeleteIfAllowed);
        this.coreViewButton = new KnowledgeCoreButton(Icon.VIEW_MODE_STORED, this::requestKnowledgeCoreViewIfAllowed);

        this.widgets.add("knowledgeCoreAdd", this.coreAddButton);
        this.widgets.add("knowledgeCoreDelete", this.coreDelButton);
        this.widgets.add("knowledgeCoreView", this.coreViewButton);
    }

    public void requestMoveGhostItem(int slotNumber, ItemStack stack) {
        this.inscriberContainer.requestMoveGhostItem(slotNumber, stack);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.updateKnowledgeCoreButtons();
    }

    @Override
    protected void drawVisInfo() {
        float visRequired = this.inscriberContainer.getVisState().getVisRequired();
        this.fontRenderer.drawString(
                ThEFeatures.instance().lang().guiVisRequired().getLocalizedKey(this.getVisIfSet(visRequired)),
                60,
                this.ySize - 168,
                4210752);
    }

    private void updateKnowledgeCoreButtons() {
        ItemStack knowledgeCore = this.getKnowledgeCore();
        boolean hasArcaneRecipe = this.inscriberContainer.isRecipeArcane();
        ItemStack result = this.inscriberContainer.getCraftingResultInventory().getStackInSlot(0);
        boolean hasRecipe = !result.isEmpty();
        boolean recipeExists = hasRecipe && KnowledgeCoreUtil.hasRecipe(knowledgeCore, result.getItem());
        boolean currentIsBlank = this.isKnowledgeCoreBlank(knowledgeCore);

        if (knowledgeCore.isEmpty()) {
            ITextComponent insertKnowledgeCore = this.tooltip(ThEFeatures.instance().lang()
                    .guiInsertKnowledgeCore()
                    .getLocalizedKey());
            this.setKnowledgeCoreButtonState(this.coreAddButton, false, insertKnowledgeCore);
            this.setKnowledgeCoreButtonState(this.coreDelButton, false, insertKnowledgeCore);
            this.setKnowledgeCoreButtonState(this.coreViewButton, false, insertKnowledgeCore);
            return;
        }

        this.setKnowledgeCoreButtonState(
                this.coreAddButton,
                hasRecipe && hasArcaneRecipe && !recipeExists,
                this.getAddKnowledgeCoreTooltip(hasRecipe, hasArcaneRecipe, recipeExists));

        if (currentIsBlank) {
            ITextComponent blankKnowledgeCore = this.tooltip(ThEFeatures.instance().lang()
                    .guiKnowledgeCoreBlank()
                    .getLocalizedKey());
            this.setKnowledgeCoreButtonState(this.coreDelButton, false, blankKnowledgeCore);
            this.setKnowledgeCoreButtonState(this.coreViewButton, false, blankKnowledgeCore);
            return;
        }

        this.setKnowledgeCoreButtonState(this.coreDelButton, true, null);
        this.setKnowledgeCoreButtonState(this.coreViewButton, true, null);
    }

    private ITextComponent getAddKnowledgeCoreTooltip(boolean hasRecipe, boolean hasArcaneRecipe, boolean recipeExists) {
        if (!hasRecipe) {
            return this.tooltip(ThEFeatures.instance().lang().guiNoRecipe().getLocalizedKey());
        }

        if (!hasArcaneRecipe) {
            return this.tooltip(ThEFeatures.instance().lang().guiRecipeNotArcane().getLocalizedKey());
        }

        if (recipeExists) {
            return this.tooltip(ThEFeatures.instance().lang().guiRecipeAlreadyStored().getLocalizedKey());
        }

        return null;
    }

    private void requestKnowledgeCoreAddIfAllowed() {
        if (this.canAddKnowledgeCoreRecipe()) {
            this.inscriberContainer.requestKnowledgeCoreAdd();
        }
    }

    private void requestKnowledgeCoreDeleteIfAllowed() {
        if (this.canOpenStoredKnowledgeCore()) {
            this.inscriberContainer.requestKnowledgeCoreDel();
        }
    }

    private void requestKnowledgeCoreViewIfAllowed() {
        if (this.canOpenStoredKnowledgeCore()) {
            this.inscriberContainer.requestKnowledgeCoreView();
        }
    }

    private void setKnowledgeCoreButtonState(KnowledgeCoreButton button, boolean enabled, ITextComponent tooltip) {
        button.visible = true;
        button.enabled = enabled;
        button.setTooltip(tooltip);
    }

    private ITextComponent tooltip(String text) {
        return new TextComponentString(text);
    }

    private boolean canAddKnowledgeCoreRecipe() {
        ItemStack knowledgeCore = this.getKnowledgeCore();
        ItemStack result = this.inscriberContainer.getCraftingResultInventory().getStackInSlot(0);
        return !knowledgeCore.isEmpty()
                && !result.isEmpty()
                && this.inscriberContainer.isRecipeArcane()
                && !KnowledgeCoreUtil.hasRecipe(knowledgeCore, result.getItem());
    }

    private boolean canOpenStoredKnowledgeCore() {
        ItemStack knowledgeCore = this.getKnowledgeCore();
        return !knowledgeCore.isEmpty() && !this.isKnowledgeCoreBlank(knowledgeCore);
    }

    private ItemStack getKnowledgeCore() {
        return this.inscriberContainer.getArcaneHost().getArcaneUpgradeInventory().getStackInSlot(0);
    }

    private boolean isKnowledgeCoreBlank(ItemStack knowledgeCore) {
        return !(knowledgeCore.getItem() instanceof ItemKnowledgeCore)
                || ((ItemKnowledgeCore) knowledgeCore.getItem()).isBlank();
    }

    private static final class KnowledgeCoreButton extends IconButton {
        private final Icon icon;
        private boolean hasTooltip;

        private KnowledgeCoreButton(Icon icon, Runnable onPress) {
            super(onPress);
            this.icon = icon;
        }

        @Override
        protected Icon getIcon() {
            return this.icon;
        }

        @Override
        public boolean isTooltipAreaVisible() {
            return this.hasTooltip && super.isTooltipAreaVisible();
        }

        private void setTooltip(ITextComponent tooltip) {
            this.hasTooltip = tooltip != null;
            this.setMessage(tooltip);
        }
    }
}
