package thaumicenergistics.client.gui.block;

import ae2.api.inventories.InternalInventory;
import ae2.client.gui.AEBaseGui;
import ae2.client.gui.style.GuiStyleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.opengl.GL11;
import thaumcraft.codechicken.lib.math.MathHelper;
import thaumicenergistics.container.block.ContainerArcaneAssembler;
import thaumicenergistics.core.ThEFeatures;
import thaumicenergistics.thaumicenergistics.Reference;

import java.awt.Color;

/**
 * @author Alex811
 */
public class GuiArcaneAssembler extends AEBaseGui<ContainerArcaneAssembler> {
    private static final String STYLE_PATH = "/screens/thaumicenergistics_arcane_assembler.json";
    private static final String[] aspects = {"aer", "terra", "ignis", "aqua", "ordo", "perditio"};
    private static final int[][] aspectGUILoc = {{69, 2}, {21, 82}, {21, 25}, {117, 25}, {117, 82}, {69, 106}};
    private static final ResourceLocation BACKGROUND_ACTIVE = new ResourceLocation(Reference.MOD_ID, "textures/gui/arcane_assembler/active.png");
    private static final ResourceLocation ASPECTS = new ResourceLocation(Reference.MOD_ID, "textures/gui/arcane_assembler/aspects.png");
    private final InternalInventory coreInventory;
    private float enAlpha;

    public GuiArcaneAssembler(ContainerArcaneAssembler container, InventoryPlayer playerInventory) {
        super(container, playerInventory, GuiStyleManager.loadStyleDoc(STYLE_PATH));
        this.coreInventory = container.getCoreInventory();
        this.enAlpha = this.coreInventory.getStackInSlot(0).isEmpty() ? 0.0F : 1.0F;
        this.setTextContent(AEBaseGui.TEXT_ID_DIALOG_TITLE,
                new TextComponentString(ThEFeatures.instance().lang().tileArcaneAssembler().getLocalizedKey()));
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        if (!this.coreInventory.getStackInSlot(0).isEmpty()) {
            if (this.container.getGuiState().getAspectExists().containsValue(false))
                this.fontRenderer.drawString(ThEFeatures.instance().lang().guiOutOfAspect().getLocalizedKey(), 100, this.getYSize() - 92, Color.RED.getRGB());
            if (!this.container.getGuiState().hasEnoughVis())
                this.fontRenderer.drawString(ThEFeatures.instance().lang().guiOutOfVis().getLocalizedKey(), 115, 3, Color.RED.getRGB());
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY, partialTicks);

        if (this.coreInventory.getStackInSlot(0).isEmpty()) {
            if (this.enAlpha > 0.0F) this.enAlpha -= 0.05F * partialTicks;
        } else {
            if (this.enAlpha < 1.0F) this.enAlpha += 0.05F * partialTicks;
        }

        if (this.enAlpha > 0.0F) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, this.enAlpha);
            this.mc.getTextureManager().bindTexture(BACKGROUND_ACTIVE);
            drawModalRectWithCustomSizedTexture(offsetX, offsetY, 0, 0, this.xSize, this.ySize, this.xSize, this.ySize);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }

        this.mc.getTextureManager().bindTexture(ASPECTS);
        for (int i = 0; i < 6; i++) {
            Boolean haveAspect = this.container.getGuiState().getAspectExists().get(aspects[i]);
            int x = aspectGUILoc[i][0];
            int y = aspectGUILoc[i][1];
            if (haveAspect != null && !this.coreInventory.getStackInSlot(0).isEmpty()) {   // recipe needs this aspect & we have a KCore
                if (!haveAspect) {       // we don't have enough of this aspect
                    float alpha = (float) ((MathHelper.sin((Minecraft.getSystemTime() / 200.0) % (2 * MathHelper.pi)) + 1.0) / 2.0);
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha * this.enAlpha);
                } else {
                    GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.2F + this.enAlpha * 0.8F);
                }
            } else {
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.2F + this.enAlpha * 0.5F);
            }
            drawModalRectWithCustomSizedTexture(offsetX + x, offsetY + y, x, y, 40, 40, this.xSize, this.ySize);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
