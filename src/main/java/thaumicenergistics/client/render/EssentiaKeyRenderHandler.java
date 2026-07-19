package thaumicenergistics.client.render;

import ae2.api.client.AEKeyRenderHandler;
import ae2.api.client.AEKeyRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.common.me.key.AEEssentiaKey;
import thaumicenergistics.common.me.key.AEEssentiaKeys;

import java.awt.Color;

public class EssentiaKeyRenderHandler implements AEKeyRenderHandler<AEEssentiaKey> {

    public static void register() {
        AEKeyRendering.register(AEEssentiaKeys.INSTANCE, AEEssentiaKey.class, new EssentiaKeyRenderHandler());
    }

    @Override
    public void drawInGui(Minecraft mc, int x, int y, AEEssentiaKey what) {
        renderAspect(mc, x, y, what.getAspect());
    }

    @Override
    public void drawOnBlockFace(AEEssentiaKey what, float scale, int combinedLight, World world) {
        float x0 = -scale / 2.0F;
        float y0 = -scale / 2.0F;
        float x1 = scale / 2.0F;
        float y1 = scale / 2.0F;

        Aspect aspect = what.getAspect();
        GlStateManager.pushMatrix();
        try {
            GlStateManager.enableBlend();
            GlStateManager.disableLighting();
            GlStateManager.disableCull();
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.getTextureManager().bindTexture(aspect.getImage());

            drawAspectQuad(x0, y0, x1, y1, new Color(aspect.getColor()));
        } finally {
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public ITextComponent getDisplayName(AEEssentiaKey what) {
        return what.getDisplayName();
    }

    public static void drawAspectQuad(double x0, double y0, double x1, double y1, Color color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(x0, y1, 0.0001D).tex(0.0D, 1.0D).color(color.getRed(), color.getGreen(), color.getBlue(), 255).endVertex();
        buffer.pos(x1, y1, 0.0001D).tex(1.0D, 1.0D).color(color.getRed(), color.getGreen(), color.getBlue(), 255).endVertex();
        buffer.pos(x1, y0, 0.0001D).tex(1.0D, 0.0D).color(color.getRed(), color.getGreen(), color.getBlue(), 255).endVertex();
        buffer.pos(x0, y0, 0.0001D).tex(0.0D, 0.0D).color(color.getRed(), color.getGreen(), color.getBlue(), 255).endVertex();
        tessellator.draw();
    }

    public static void renderAspect(Minecraft mc, int x, int y, Aspect aspect) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ZERO);
            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(aspect.getImage());

            drawAspectQuad(x, y, x + 16.0D, y + 16.0D, new Color(aspect.getColor()));
        } finally {
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ZERO);
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

}
