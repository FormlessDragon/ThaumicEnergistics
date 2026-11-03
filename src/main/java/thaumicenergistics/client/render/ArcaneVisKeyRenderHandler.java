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
import thaumicenergistics.common.me.key.ArcaneVisKey;
import thaumicenergistics.common.me.key.ArcaneVisKeys;

/**
 * Renders the diagnostic Arcane Vis key as a blue Vis mote in AE2 pattern displays.
 */
public final class ArcaneVisKeyRenderHandler implements AEKeyRenderHandler<ArcaneVisKey> {

    private static final int SEGMENTS = 12;

    public static void register() {
        AEKeyRendering.register(ArcaneVisKeys.INSTANCE, ArcaneVisKey.class, new ArcaneVisKeyRenderHandler());
    }

    @Override
    public void drawInGui(Minecraft minecraft, int x, int y, ArcaneVisKey what) {
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.enableBlend();
            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            drawVisMote(x + 8.0F, y + 8.0F, 6.0F);
        } finally {
            restoreCapabilities(blendEnabled, lightingEnabled, textureEnabled);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void drawOnBlockFace(ArcaneVisKey what, float scale, int combinedLight, World world) {
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.enableBlend();
            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            drawVisMote(0.0F, 0.0F, scale / 2.0F);
        } finally {
            restoreCapabilities(blendEnabled, lightingEnabled, textureEnabled);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public ITextComponent getDisplayName(ArcaneVisKey what) {
        return what.getDisplayName();
    }

    private static void drawVisMote(float centerX, float centerY, float radius) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(centerX, centerY, 0.0001D).color(186, 242, 255, 255).endVertex();
        for (int segment = 0; segment <= SEGMENTS; segment++) {
            double angle = (Math.PI * 2.0D * segment) / SEGMENTS;
            double x = centerX + Math.cos(angle) * radius;
            double y = centerY + Math.sin(angle) * radius;
            buffer.pos(x, y, 0.0001D).color(54, 154, 230, 255).endVertex();
        }
        tessellator.draw();
    }

    private static void restoreCapabilities(boolean blendEnabled, boolean lightingEnabled, boolean textureEnabled) {
        if (blendEnabled) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
        }
        if (lightingEnabled) {
            GlStateManager.enableLighting();
        } else {
            GlStateManager.disableLighting();
        }
        if (textureEnabled) {
            GlStateManager.enableTexture2D();
        } else {
            GlStateManager.disableTexture2D();
        }
    }

}
