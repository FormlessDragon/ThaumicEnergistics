package thaumicenergistics.client.gui.part;

import ae2.client.gui.Icon;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.IconButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.core.definitions.GuiText;
import thaumicenergistics.items.ItemKnowledgeCore;
import thaumicenergistics.util.knowledgeCoreUtil.KnowledgeCoreUtil;

/**
 * @author Alex811
 */
public class GuiArcaneInscriber extends GuiArcaneTerm {
    public static final String STYLE_PATH = "/screens/terminals/thaumicenergistics_arcane_inscriber.json";

    private final KnowledgeCoreButton coreAddButton;
    private final KnowledgeCoreButton coreDelButton;
    private final KnowledgeCoreButton coreViewButton;
    private final ContainerArcaneInscriber inscriberContainer;

    public GuiArcaneInscriber(ContainerArcaneInscriber container, InventoryPlayer playerInventory) {
        super(container, playerInventory, GuiText.arcane_inscriber.text(), GuiStyleManager.loadStyleDoc(GuiArcaneInscriber.STYLE_PATH));
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
                GuiText.vis_required.getLocal(this.getVisIfSet(visRequired)),
                80,
                this.ySize - 178,
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
            ITextComponent insertKnowledgeCore = GuiText.insert_knowledge_core.text();
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
            ITextComponent blankKnowledgeCore = GuiText.knowledge_core_is_blank.text();
            this.setKnowledgeCoreButtonState(this.coreDelButton, false, blankKnowledgeCore);
            this.setKnowledgeCoreButtonState(this.coreViewButton, false, blankKnowledgeCore);
            return;
        }

        this.setKnowledgeCoreButtonState(this.coreDelButton, true, null);
        this.setKnowledgeCoreButtonState(this.coreViewButton, true, null);
    }

    private ITextComponent getAddKnowledgeCoreTooltip(boolean hasRecipe, boolean hasArcaneRecipe, boolean recipeExists) {
        if (!hasRecipe) {
            return GuiText.no_recipe.text();
        }

        if (!hasArcaneRecipe) {
            return GuiText.recipe_not_arcane.text();
        }

        if (recipeExists) {
            return GuiText.recipe_already_stored.text();
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
        return this.inscriberContainer.getHost().getKnowledgeCoreInventory().getStackInSlot(0);
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
