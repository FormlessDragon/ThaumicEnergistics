package thaumicenergistics.client.gui;

import ae2.api.config.Setting;
import ae2.api.config.Settings;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.client.gui.widgets.ITooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.client.gui.component.GuiImgButton;
import thaumicenergistics.client.gui.helpers.GenericStackSizeRenderer;
import thaumicenergistics.client.gui.helpers.GuiScrollBar;
import thaumicenergistics.client.gui.helpers.TerminalDisplayStack;
import thaumicenergistics.container.ContainerBase;
import thaumicenergistics.container.slot.ISlotOptional;
import thaumicenergistics.container.slot.SlotGhostEssentia;
import thaumicenergistics.container.slot.SlotME;
import thaumicenergistics.container.slot.ThESlot;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

import static thaumicenergistics.config.ThESettings.actions;
import static thaumicenergistics.config.ThESettings.searchMode;

/**
 * @author BrockWS
 * @author Alex811
 */
public abstract class GuiBase extends GuiContainer {
    private static final GenericStackSizeRenderer stackSizeRenderer = new GenericStackSizeRenderer();
    private int currMouseX = 0;
    private int currMouseY = 0;

    public GuiBase(ContainerBase container) {
        super(container);
    }

    public void reload() {

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0f, 1.0F);
        this.drawSlotBackgrounds();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.drawSlotOverlays();
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    protected void drawSlotBackgrounds() {
        mc.getTextureManager().bindTexture(this.getGuiBackground());
        for (Slot slot : this.inventorySlots.inventorySlots) {
            this.drawSlotBackground(slot);
        }
    }

    protected void drawSlotBackground(Slot slot) {
        if (slot instanceof ISlotOptional) {
            if (slot.isEnabled()) {
                // TODO: Draw slot background on enabled slots
            }
        } else if (slot instanceof ThESlot) {
            if (((ThESlot) slot).hasBackgroundIcon()) {
                int index = ((ThESlot) slot).getBackgroundIconIndex();
                int uv_y = (int) Math.floor((double) index / 16);
                int uv_x = index - uv_y * 16;

                Minecraft.getMinecraft().getTextureManager().bindTexture(((ThESlot) slot).getBackgroundIcon());

                GlStateManager.enableBlend();
                GlStateManager.disableLighting();
                GlStateManager.enableTexture2D();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

                this.drawTexturedModelRectColor(slot.xPos, slot.yPos, uv_x * 16, uv_y * 16, 16, 16, new Color(1f, 1f, 1f, 0.4f));
            }
        }
    }

    protected void drawSlotOverlays() {
        for (Slot slot : this.inventorySlots.inventorySlots) {
            this.drawSlotOverlay(slot);
        }
    }

    protected void drawSlotOverlay(Slot slot) {
        if (slot instanceof SlotME) {
            TerminalDisplayStack stack = ((SlotME) slot).getDisplayStack();
            if (stack != null) {
                stackSizeRenderer.renderStackSize(this.fontRenderer, stack.stackSize(), stack.craftable(), slot.xPos, slot.yPos);
            }
        }
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        Slot hovered = this.findSlotAtMouse(mouseX, mouseY);
        if (hovered != null) {
            if (hovered instanceof SlotGhostEssentia && ((SlotGhostEssentia) hovered).getAspect() != null) {
                this.drawHoveringText(((SlotGhostEssentia) hovered).getAspect().getName(), mouseX, mouseY);
                return;
            }
            if (hovered instanceof SlotME && hovered.getHasStack()) {
                TerminalDisplayStack stack = ((SlotME) hovered).getDisplayStack();
                if (stack != null && stack.key() instanceof AEEssentiaKey) {
                    AEEssentiaKey key = (AEEssentiaKey) stack.key();
                    this.drawHoveringText(key.getAspect().getName(), mouseX, mouseY);
                    return;
                }
            }
        }

        // TODO: Don't use AE2 Core classes
        for (GuiButton c : this.buttonList) {
            if (!(c instanceof ITooltip) || !((ITooltip) c).isTooltipAreaVisible())
                continue;
            ITooltip t = (ITooltip) c;
            Rectangle area = t.getTooltipArea();
            int x = area.x;
            int y = area.y;
            if (area.contains(mouseX, mouseY)) {
                List<String> lines = t.getTooltipMessage().stream().map(component -> component.getFormattedText()).collect(Collectors.toList());
                if (lines.isEmpty())
                    continue;

                if (y < 15)
                    y = 15;

                // AE2 Has the first line as WHITE and the rest as GRAY
                lines.set(0, TextFormatting.WHITE + lines.get(0));
                for (int i = 1; i < lines.size(); i++)
                    lines.set(i, TextFormatting.GRAY + lines.get(i));

                this.drawHoveringText(lines, x + 11, y + 4, this.fontRenderer);
            }
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    protected Slot findSlotAtMouse(int mouseX, int mouseY) {
        for (Slot slot : this.inventorySlots.inventorySlots) {
            if (this.isPointOverSlot(slot, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private boolean isPointOverSlot(Slot slot, int mouseX, int mouseY) {
        int x = this.guiLeft + slot.xPos;
        int y = this.guiTop + slot.yPos;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    /**
     * Called when a PacketSettingChange is received (Client and Server)
     *
     * @param setting Setting changed
     * @param value   New Value
     */
    public void updateSetting(Setting<?> setting, Enum<?> value) {
        if (this.inventorySlots instanceof IConfigurableObject) {
            IConfigManager configManager = ((IConfigurableObject) this.inventorySlots).getConfigManager();
            putSetting(configManager, setting, value);
            this.buttonList.forEach(btn -> {
                if (!(btn instanceof GuiImgButton))
                    return;
                GuiImgButton b = (GuiImgButton) btn;
                if (actions() == b.getSetting() || Settings.TERMINAL_STYLE == b.getSetting() || searchMode() == b.getSetting())
                    return;
                b.set(getSetting(configManager, b.getSetting()));
            });
        }
    }

    public boolean hasConfigSetting(Setting<?> setting) {
        return !(this.inventorySlots instanceof IConfigurableObject)
                || ((IConfigurableObject) this.inventorySlots).getConfigManager().getSettings().contains(setting);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void putSetting(IConfigManager configManager, Setting<?> setting, Enum<?> value) {
        putSettingUnchecked(configManager, (Setting) setting, value);
    }

    private static <T extends Enum<T>> void putSettingUnchecked(IConfigManager configManager, Setting<T> setting, Enum<?> value) {
        configManager.putSetting(setting, setting.getEnumClass().cast(value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Enum<?> getSetting(IConfigManager configManager, Setting<?> setting) {
        return configManager.getSetting((Setting) setting);
    }

    protected void addMESlot(SlotME slot) {
        slot.slotNumber = this.inventorySlots.inventorySlots.size();
        this.inventorySlots.inventorySlots.add(slot);
        this.inventorySlots.inventoryItemStacks.add(ItemStack.EMPTY);
    }

    protected abstract ResourceLocation getGuiBackground();

    protected void drawTexturedModelRectColor(int x, int y, int textureX, int textureY, int width, int height, Color color) {
        float offsetX = 0.00390625F;
        float offsetY = 0.00390625F;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        buf.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        buf.pos(x, y + height, this.zLevel).tex(textureX * offsetX, (textureY + height) * offsetY).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buf.pos(x + width, y + height, this.zLevel).tex((textureX + width) * offsetX, (textureY + height) * offsetY).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buf.pos(x + width, y, this.zLevel).tex((textureX + width) * offsetX, textureY * offsetY).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        buf.pos(x, y, this.zLevel).tex(textureX * offsetX, textureY * offsetY).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        tess.draw();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.setCurrMousePos(mouseX, mouseY);
    }

    protected void setCurrMousePos(int mouseX, int mouseY) {
        this.currMouseX = mouseX;
        this.currMouseY = mouseY;
    }

    /**
     * Checks if the mouse is currently withing a region.
     *
     * @param x        horizontal start of the region
     * @param y        vertical start of the region
     * @param w        width of the region
     * @param h        height of the region
     * @param relative true: use mouse location relative to the GUI, false: use absolute mouse location
     * @return true if the mouse is within the region
     */
    protected boolean mouseWithin(int x, int y, int w, int h, boolean relative) {
        int mouseX = currMouseX;
        int mouseY = currMouseY;
        if (relative) {
            mouseX -= this.getGuiLeft();
            mouseY -= this.getGuiTop();
        }
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    /**
     * Checks if the mouse is within this GUI.
     *
     * @return true if inside the GUI area
     */
    protected boolean mouseWithin() {
        return mouseWithin(0, 0, xSize, ySize, true);
    }

    protected boolean mouseWithin(GuiScrollBar scrollBar) {
        return mouseWithin(scrollBar.getX(), scrollBar.getY(), 15, scrollBar.getHeight(), true);
    }

    protected boolean mouseWithin(GuiTextField textField) {
        return mouseWithin(textField.x, textField.y, textField.width, textField.height, false);
    }

    protected boolean mouseWithin(Slot slot) {
        return mouseWithin(slot.xPos, slot.yPos, 16, 16, true);
    }
}
