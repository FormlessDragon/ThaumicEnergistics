package thaumicenergistics.client.gui.part;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.client.gui.component.GuiImageButton;
import thaumicenergistics.client.gui.style.ThEGuiStyleManager;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.items.ItemKnowledgeCore;
import thaumicenergistics.util.KnowledgeCoreUtil;

import java.io.IOException;

/**
 * @author Alex811
 */
public class GuiArcaneInscriber extends GuiArcaneTerm {

    private GuiImageButton coreAddButton;
    private GuiImageButton coreDelButton;
    private GuiImageButton coreViewButton;
    private final ContainerArcaneInscriber inscriberContainer;
    private final ResourceLocation images = new ResourceLocation(ModGlobals.MOD_ID_AE2, "textures/guis/states.png");

    public GuiArcaneInscriber(ContainerArcaneInscriber container, InventoryPlayer playerInventory) {
        super(container, playerInventory,
                new TextComponentTranslation("gui.thaumicenergistics.arcane_inscriber"),
                ThEGuiStyleManager.loadStyleDoc(GuiArcaneTerm.STYLE_PATH));
        this.inscriberContainer = container;
    }

    @Override
    public void initGui() {
        super.initGui();
        int coreBtnRowY = this.guiTop + this.ySize - 100;

        this.coreAddButton = this.addButton(new GuiImageButton(this.getGuiLeft() + 87, coreBtnRowY, 0, 12, images));
        this.coreDelButton = this.addButton(new GuiImageButton(this.getGuiLeft() + 104, coreBtnRowY, 0, 7, images));
        this.coreViewButton = this.addButton(new GuiImageButton(this.getGuiLeft() + 121, coreBtnRowY, 1, 12, images));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == this.coreAddButton && this.canAddKnowledgeCoreRecipe()) {
            this.inscriberContainer.requestKnowledgeCoreAdd();
            return;
        }

        if (button == this.coreDelButton && this.canOpenStoredKnowledgeCore()) {
            this.inscriberContainer.requestKnowledgeCoreDel();
            return;
        }

        if (button == this.coreViewButton && this.canOpenStoredKnowledgeCore()) {
            this.inscriberContainer.requestKnowledgeCoreView();
            return;
        }

        super.actionPerformed(button);
    }

    public void setIsArcane(boolean isArcane) {
        this.inscriberContainer.recipeIsArcane = isArcane;
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        this.updateKnowledgeCoreButtons(mouseX, mouseY);
    }

    @Override
    protected void drawVisInfo() {
        this.fontRenderer.drawString(
                ThEApi.instance().lang().guiVisRequired().getLocalizedKey(this.getVisIfSet(this.visRequired)),
                60,
                this.ySize - 168,
                4210752);
    }

    private void updateKnowledgeCoreButtons(int mouseX, int mouseY) {
        ItemStack knowledgeCore = this.inscriberContainer.getInventory("upgrades").getStackInSlot(0);
        boolean hasArcaneRecipe = this.inscriberContainer.recipeIsArcane;
        ItemStack result = this.inscriberContainer.getInventory("result").getStackInSlot(0);
        boolean hasRecipe = !result.isEmpty();
        boolean recipeExists = hasRecipe && KnowledgeCoreUtil.hasRecipe(knowledgeCore, result.getItem());
        boolean currentIsBlank = this.isKnowledgeCoreBlank(knowledgeCore);
        if (!knowledgeCore.isEmpty()) {
            renderButton(coreAddButton, hasRecipe && hasArcaneRecipe && !recipeExists);
            if (currentIsBlank) {
                renderButton(coreViewButton, false);
                renderButton(coreDelButton, false);
                if ((coreViewButton.isHovered() || coreDelButton.isHovered()))
                    renderText(ThEApi.instance().lang().guiKnowledgeCoreBlank().getLocalizedKey(), mouseX, mouseY);
            } else {
                renderButton(coreViewButton, true);
                renderButton(coreDelButton, true);
            }
            if (coreAddButton.isHovered()) {
                if (hasRecipe) {
                    if (!hasArcaneRecipe)
                        renderText(ThEApi.instance().lang().guiRecipeNotArcane().getLocalizedKey(), mouseX, mouseY);
                    else if (recipeExists)
                        renderText(ThEApi.instance().lang().guiRecipeAlreadyStored().getLocalizedKey(), mouseX, mouseY);
                } else renderText(ThEApi.instance().lang().guiNoRecipe().getLocalizedKey(), mouseX, mouseY);
            }
        } else {
            renderButton(coreAddButton, false);
            renderButton(coreViewButton, false);
            renderButton(coreDelButton, false);
            if ((coreViewButton.isHovered() || coreDelButton.isHovered() || coreAddButton.isHovered()))
                renderText(ThEApi.instance().lang().guiInsertKnowledgeCore().getLocalizedKey(), mouseX, mouseY);
        }
    }

    private boolean canAddKnowledgeCoreRecipe() {
        ItemStack knowledgeCore = this.inscriberContainer.getInventory("upgrades").getStackInSlot(0);
        ItemStack result = this.inscriberContainer.getInventory("result").getStackInSlot(0);
        return !knowledgeCore.isEmpty()
                && !result.isEmpty()
                && this.inscriberContainer.recipeIsArcane
                && !KnowledgeCoreUtil.hasRecipe(knowledgeCore, result.getItem());
    }

    private boolean canOpenStoredKnowledgeCore() {
        ItemStack knowledgeCore = this.inscriberContainer.getInventory("upgrades").getStackInSlot(0);
        return !knowledgeCore.isEmpty() && !this.isKnowledgeCoreBlank(knowledgeCore);
    }

    private boolean isKnowledgeCoreBlank(ItemStack knowledgeCore) {
        return !(knowledgeCore.getItem() instanceof ItemKnowledgeCore)
                || ((ItemKnowledgeCore) knowledgeCore.getItem()).isBlank();
    }

    protected void renderButton(GuiImageButton button, boolean enabled) {
        button.enabled = enabled;
        if (enabled) {
            button.setButtonAlpha(1.0F, 1.0F);
            if (button == coreDelButton) coreDelButton.setImageAlpha(1.0F, 1.0F);
            else button.setAllImages(images, (button == coreViewButton) ? button.width : 0, button.height * 11);
        } else {
            coreDelButton.setImageAlpha(0.5F, 0.5F);
            if (button == coreDelButton) coreDelButton.setImageAlpha(0.5F, 0.5F);
            else button.setAllImages(images, (button == coreViewButton) ? button.width : 0, button.height * 12);
        }
    }

    protected void renderText(String text, int x, int y) {
        this.drawCenteredString(mc.fontRenderer, text, x - this.getGuiLeft(), y - this.getGuiTop() - 5, 0x00e6ac);
    }
}
