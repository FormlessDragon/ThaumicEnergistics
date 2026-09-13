package thaumicenergistics.client.gui.item;

import ae2.client.gui.pattern.GuiPattern;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.container.ContainerKnowledgeCorePattern;
import thaumicenergistics.core.definitions.GuiText;

import static thaumicenergistics.ThaumicEnergistics.id;

/**
 * Pattern view of a Knowledge Core recipe projection, opened with the AE2 "open pattern" key.
 */
public class GuiKnowledgeCorePattern extends GuiPattern<ContainerKnowledgeCorePattern> {

    private static final ResourceLocation BACKGROUND = id("textures/gui/knowledge_core_pattern.png");

    public GuiKnowledgeCorePattern(ContainerKnowledgeCorePattern container, InventoryPlayer playerInventory) {
        super(container, playerInventory);
        this.ySize = 109;
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRenderer.drawString(
            GuiText.vis_required.getLocal(this.getVisIfSet(this.container.getVisCost())),
            8, 6, 0x303030);
    }

    private float getVisIfSet(float vis) {
        return vis > -1 ? vis : 0;
    }
}
