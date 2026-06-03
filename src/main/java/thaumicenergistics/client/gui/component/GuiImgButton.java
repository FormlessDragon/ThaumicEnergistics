package thaumicenergistics.client.gui.component;

import ae2.api.config.Setting;
import ae2.client.gui.widgets.ITooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import thaumicenergistics.util.ThEUtil;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

/**
 * Small local replacement for AE2's old config image button.
 */
public class GuiImgButton extends GuiButton implements ITooltip {
    private final Setting<?> setting;
    private Enum<?> currentValue;
    private boolean halfSize;

    public <T extends Enum<T>> GuiImgButton(int x, int y, Setting<T> setting, T currentValue) {
        super(0, x, y, 16, 16, "");
        this.setting = setting;
        this.currentValue = currentValue;
    }

    public Setting<?> getSetting() {
        return this.setting;
    }

    public Enum<?> getCurrentValue() {
        return this.currentValue;
    }

    public void set(Enum<?> value) {
        this.currentValue = value;
    }

    public Enum<?> getNextValue(boolean backwards) {
        return ThEUtil.rotateEnum(this.currentValue, this.setting.getValues(), backwards);
    }

    public void setHalfSize(boolean halfSize) {
        this.halfSize = halfSize;
        this.width = halfSize ? 8 : 16;
        this.height = halfSize ? 8 : 16;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        int color = this.enabled ? 0xFF555555 : 0xFF222222;
        if (this.hovered) {
            color = 0xFF777777;
        }
        drawRect(this.x, this.y, this.x + this.width, this.y + this.height, color);
        drawCenteredString(mc.fontRenderer, this.getDisplayText(), this.x + this.width / 2, this.y + (this.height - 8) / 2, 0xFFFFFF);
    }

    @Override
    public List<ITextComponent> getTooltipMessage() {
        if (this.currentValue == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new TextComponentString(this.setting.getName() + ": " + this.currentValue.name()));
    }

    @Override
    public Rectangle getTooltipArea() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return this.visible;
    }

    private String getDisplayText() {
        if (this.currentValue == null) {
            return "";
        }
        String name = this.currentValue.name();
        return name.isEmpty() ? "" : name.substring(0, 1);
    }
}
