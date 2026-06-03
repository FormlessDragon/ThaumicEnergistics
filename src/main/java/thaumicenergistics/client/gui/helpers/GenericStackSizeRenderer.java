package thaumicenergistics.client.gui.helpers;

import ae2.core.AEConfig;
import ae2.util.ReadableNumberConverter;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Based on StackSizeRenderer.
 */
public class GenericStackSizeRenderer {

    public void renderStackSize(FontRenderer fontRenderer, long stackSize, boolean craftable, int xPos, int yPos) {
        final boolean useLargeFonts = AEConfig.instance().isUseLargeFonts();
        final float scaleFactor = useLargeFonts ? 0.85f : 0.5f;
        final float inverseScaleFactor = 1.0f / scaleFactor;
        final int offset = useLargeFonts ? 0 : -1;

        final boolean unicodeFlag = fontRenderer.getUnicodeFlag();
        fontRenderer.setUnicodeFlag(false);

        if (stackSize == 0 && craftable) {
            this.renderLabel(fontRenderer, "+", xPos, yPos, scaleFactor, inverseScaleFactor, offset);
        }

        if (stackSize > 0) {
            final String renderedStackSize = this.getToBeRenderedStackSize(stackSize, useLargeFonts);
            this.renderLabel(fontRenderer, renderedStackSize, xPos, yPos, scaleFactor, inverseScaleFactor, offset);
        }

        fontRenderer.setUnicodeFlag(unicodeFlag);
    }

    private void renderLabel(FontRenderer fontRenderer, String text, int xPos, int yPos, float scaleFactor, float inverseScaleFactor, int offset) {
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleFactor, scaleFactor, scaleFactor);
        final int x = (int) (((float) xPos + offset + 16.0f - fontRenderer.getStringWidth(text) * scaleFactor) * inverseScaleFactor);
        final int y = (int) (((float) yPos + offset + 16.0f - 7.0f * scaleFactor) * inverseScaleFactor);
        fontRenderer.drawStringWithShadow(text, x, y, 16777215);
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
    }

    private String getToBeRenderedStackSize(final long originalSize, boolean useLargeFonts) {
        return ReadableNumberConverter.format(originalSize, useLargeFonts ? 3 : 4);
    }
}
