package thaumicenergistics.client.gui.part;

import ae2.api.config.ActionItems;
import ae2.client.gui.Icon;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.DynamicIconButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.core.definitions.GuiText;
import thaumicenergistics.items.ItemKnowledgeCore;
import thaumicenergistics.util.knowledgeCoreUtil.KnowledgeCoreUtil;

import java.util.Collections;
import java.util.List;

/**
 * @author Alex811
 */
public class GuiArcaneInscriber extends GuiArcaneTerm<ContainerArcaneInscriber> {

    public static final String STYLE_PATH = "/screens/terminals/thaumicenergistics_arcane_inscriber.json";

    private final DynamicIconButton coreAddButton;
    private final DynamicIconButton coreDelButton;
    private final DynamicIconButton coreViewButton;

    public GuiArcaneInscriber(ContainerArcaneInscriber container, InventoryPlayer playerInventory) {
        super(container, playerInventory, GuiText.arcane_inscriber.text(), GuiStyleManager.loadStyleDoc(GuiArcaneInscriber.STYLE_PATH));

        this.coreAddButton = new DynamicIconButton(() -> Icon.ARROW_DOWN, GuiText.add.text(), () -> {
            ItemStack knowledgeCore = this.getKnowledgeCore();
            if (knowledgeCore.isEmpty()) {
                return List.of(GuiText.insert_knowledge_core.text());
            }

            ItemStack result = this.container.getCraftingResultInventory().getStackInSlot(0);
            if (result.isEmpty()) {
                return List.of(GuiText.no_recipe.text());
            }

            if (!this.container.isRecipeArcane()) {
                return List.of(GuiText.recipe_not_arcane.text());
            }

            if (KnowledgeCoreUtil.hasRecipe(knowledgeCore, result.getItem())) {
                return List.of(GuiText.recipe_already_stored.text());
            }

            return Collections.emptyList();
        }, () -> {
            if (this.canAddKnowledgeCoreRecipe()) {
                this.container.requestKnowledgeCoreAdd();
            }
        });
        this.coreDelButton = new DynamicIconButton(() -> Icon.CLEAR, GuiText.del.text(), () -> {
            ItemStack knowledgeCore = this.getKnowledgeCore();
            if (knowledgeCore.isEmpty()) {
                return List.of(GuiText.insert_knowledge_core.text());
            }

            if (this.isKnowledgeCoreBlank(knowledgeCore)) {
                return List.of(GuiText.knowledge_core_is_blank.text());
            }

            return Collections.emptyList();
        }, () -> {
            if (this.canOpenStoredKnowledgeCore()) {
                this.container.requestKnowledgeCoreDel();
            }
        });
        this.coreViewButton = new DynamicIconButton(() -> Icon.VIEW_MODE_STORED, GuiText.view.text(), () -> {
            ItemStack knowledgeCore = this.getKnowledgeCore();
            if (knowledgeCore.isEmpty()) {
                return List.of(GuiText.insert_knowledge_core.text());
            }

            if (this.isKnowledgeCoreBlank(knowledgeCore)) {
                return List.of(GuiText.knowledge_core_is_blank.text());
            }

            return Collections.emptyList();
        }, () -> {
            if (this.canOpenStoredKnowledgeCore()) {
                this.container.requestKnowledgeCoreView();
            }
        });

        this.widgets.add("knowledgeCoreAdd", this.coreAddButton);
        this.widgets.add("knowledgeCoreDelete", this.coreDelButton);
        this.widgets.add("knowledgeCoreView", this.coreViewButton);
    }

    @Override
    protected ActionItems getClearGridActionItem() {
        return ActionItems.S_CLOSE;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (this.getKnowledgeCore().isEmpty()) {
            this.coreAddButton.enabled = false;
            this.coreDelButton.enabled = false;
            this.coreViewButton.enabled = false;
            return;
        }

        this.coreAddButton.enabled = this.canAddKnowledgeCoreRecipe();
        this.coreDelButton.enabled = this.canOpenStoredKnowledgeCore();
        this.coreViewButton.enabled = this.canOpenStoredKnowledgeCore();
    }

    @Override
    protected void drawVisInfo() {
        float visRequired = this.container.getVisState().getVisRequired();
        this.fontRenderer.drawString(
                GuiText.vis_required.getLocal(this.getVisIfSet(visRequired)),
                80,
                this.ySize - 178,
                4210752);
    }

    private boolean canAddKnowledgeCoreRecipe() {
        ItemStack knowledgeCore = this.getKnowledgeCore();
        ItemStack result = this.container.getCraftingResultInventory().getStackInSlot(0);
        return !knowledgeCore.isEmpty()
                && !result.isEmpty()
                && this.container.isRecipeArcane()
                && !KnowledgeCoreUtil.hasRecipe(knowledgeCore, result.getItem());
    }

    private boolean canOpenStoredKnowledgeCore() {
        ItemStack knowledgeCore = this.getKnowledgeCore();
        return !knowledgeCore.isEmpty() && !this.isKnowledgeCoreBlank(knowledgeCore);
    }

    private ItemStack getKnowledgeCore() {
        return this.container.getHost().getKnowledgeCoreInventory().getStackInSlot(0);
    }

    private boolean isKnowledgeCoreBlank(ItemStack knowledgeCore) {
        return !(knowledgeCore.getItem() instanceof ItemKnowledgeCore item) || item.isBlank();
    }

}
