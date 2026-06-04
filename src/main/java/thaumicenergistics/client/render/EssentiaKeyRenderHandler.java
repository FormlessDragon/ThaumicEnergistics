package thaumicenergistics.client.render;

import ae2.api.client.AEKeyRenderHandler;
import ae2.api.client.AEKeyRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.me.key.AEEssentiaKeys;

import java.awt.*;

public class EssentiaKeyRenderHandler implements AEKeyRenderHandler<AEEssentiaKey> {

    public static void register() {
        AEKeyRendering.register(AEEssentiaKeys.INSTANCE, AEEssentiaKey.class, new EssentiaKeyRenderHandler());
    }

    @Override
    public void drawInGui(Minecraft mc, int x, int y, AEEssentiaKey what) {
        Aspect aspect = what.getAspect();
        GlStateManager.pushMatrix();

        try {
            mc.getTextureManager().bindTexture(aspect.getImage());
            GL11.glBlendFunc(770, 771);
            GlStateManager.disableLighting();
            // GlStateManager.scale(0.063f, 0.063f, 0);
            GlStateManager.rotate(180f, 1, 1, 0);
            GlStateManager.rotate(90f, 0, 0, 1);
            GlStateManager.translate(0f, -1f, 0);

            Color c = new Color(aspect.getColor());
            GlStateManager.color((float) c.getRed() / 255.0F, (float) c.getGreen() / 255.0F, (float) c.getBlue() / 255.0F, 1.0f);

            Tessellator tess = Tessellator.getInstance();
            tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            tess.getBuffer().pos(0.0D, 1.0D, 0).tex(0.0D, 1.0D).color((float) c.getRed() / 255.0F, (float) c.getGreen() / 255.0F, (float) c.getBlue() / 255.0F, 1.0f).endVertex();
            tess.getBuffer().pos(1.0D, 1.0D, 0).tex(1.0D, 1.0D).color((float) c.getRed() / 255.0F, (float) c.getGreen() / 255.0F, (float) c.getBlue() / 255.0F, 1.0f).endVertex();
            tess.getBuffer().pos(1.0D, 0.0D, 0).tex(1.0D, 0.0D).color((float) c.getRed() / 255.0F, (float) c.getGreen() / 255.0F, (float) c.getBlue() / 255.0F, 1.0f).endVertex();
            tess.getBuffer().pos(0.0D, 0.0D, 0).tex(0.0D, 0.0D).color((float) c.getRed() / 255.0F, (float) c.getGreen() / 255.0F, (float) c.getBlue() / 255.0F, 1.0f).endVertex();
            tess.draw();
        } finally {
            GlStateManager.enableBlend();
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
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

            Color c = new Color(aspect.getColor());
            GlStateManager.color((float) c.getRed() / 255.0F, (float) c.getGreen() / 255.0F, (float) c.getBlue() / 255.0F, 1.0f);

            Tessellator tess= Tessellator.getInstance();
            tess.getBuffer().begin(7, DefaultVertexFormats.POSITION_TEX);
            tess.getBuffer().pos(x0, y1, 0).tex(0.0D, 1.0D).color((float) c.getRed() / 255.0F, (float) c.getGreen() / 255.0F, (float) c.getBlue() / 255.0F, 1.0f).endVertex();
            tess.getBuffer().pos(x1, y1, 0).tex(1.0D, 1.0D).color((float) c.getRed() / 255.0F, (float) c.getGreen() / 255.0F, (float) c.getBlue() / 255.0F, 1.0f).endVertex();
            tess.getBuffer().pos(x1, y0, 0).tex(1.0D, 0.0D).color((float) c.getRed() / 255.0F, (float) c.getGreen() / 255.0F, (float) c.getBlue() / 255.0F, 1.0f).endVertex();
            tess.getBuffer().pos(x0, y1, 0).tex(0.0D, 0.0D).color((float) c.getRed() / 255.0F, (float) c.getGreen() / 255.0F, (float) c.getBlue() / 255.0F, 1.0f).endVertex();
            tess.draw();
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
}
