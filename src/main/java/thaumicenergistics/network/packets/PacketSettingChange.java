package thaumicenergistics.network.packets;

import ae2.api.config.Setting;
import ae2.api.config.Settings;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumicenergistics.client.gui.GuiBase;
import thaumicenergistics.config.ThESettings;

/**
 * @author BrockWS
 * @author Alex811
 */
public class PacketSettingChange implements IMessage {

    private String setting;
    private String value;

    public PacketSettingChange() {
    }

    public PacketSettingChange(Setting<?> setting, Enum<?> value) {
        this(setting.getName(), value.name());
    }

    public PacketSettingChange(String s, String v) {
        this.setting = s;
        this.value = v;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.setting = ByteBufUtils.readUTF8String(buf);
        this.value = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.setting);
        ByteBufUtils.writeUTF8String(buf, this.value);
    }

    public Setting<?> getSetting() {
        return this.resolveSetting();
    }

    private Setting<?> resolveSetting() {
        Setting<?> teSetting = ThESettings.get(this.setting);
        if (teSetting != null) {
            return teSetting;
        }
        try {
            return Settings.getOrThrow(this.setting);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Enum<?> getValue() {
        Setting<?> resolvedSetting = this.resolveSetting();
        if (resolvedSetting == null) {
            return null;
        }
        return this.resolveValue(resolvedSetting);
    }

    private Enum<?> resolveValue(Setting<?> resolvedSetting) {
        for (Enum<?> e : resolvedSetting.getValues())
            if (e.name().equalsIgnoreCase(this.value))
                return e;
        return null;
    }

    public static class HandlerServer implements IMessageHandler<PacketSettingChange, IMessage> {

        @Override
        public IMessage onMessage(PacketSettingChange message, MessageContext ctx) {
            Setting<?> resolvedSetting = message.resolveSetting();
            Enum<?> resolvedValue = resolvedSetting == null ? null : message.resolveValue(resolvedSetting);
            if (resolvedSetting == null || resolvedValue == null) {
                return null;
            }

            NetHandlerPlayServer handler = ctx.getServerHandler();
            EntityPlayerMP player = handler.player;
            IThreadListener thread = (IThreadListener) player.world;
            thread.addScheduledTask(() -> {
                if (player.openContainer instanceof IConfigurableObject) {
                    IConfigManager cm = ((IConfigurableObject) player.openContainer).getConfigManager();
                    if (cm != null) {
                        if (!cm.getSettings().contains(resolvedSetting)) {
                            return;
                        }
                        putSetting(cm, resolvedSetting, resolvedValue);
                    }
                }
            });
            return null;
        }
    }

    public static class HandlerClient implements IMessageHandler<PacketSettingChange, IMessage> {

        @Override
        public IMessage onMessage(PacketSettingChange message, MessageContext ctx) {
            Setting<?> resolvedSetting = message.resolveSetting();
            Enum<?> resolvedValue = resolvedSetting == null ? null : message.resolveValue(resolvedSetting);
            if (resolvedSetting == null || resolvedValue == null) {
                return null;
            }

            Minecraft.getMinecraft().addScheduledTask(() -> {
                GuiScreen gui = Minecraft.getMinecraft().currentScreen;
                if (gui instanceof GuiBase) {
                    GuiBase guiBase = (GuiBase) gui;
                    if (!guiBase.hasConfigSetting(resolvedSetting)) {
                        return;
                    }
                    guiBase.updateSetting(resolvedSetting, resolvedValue);
                }
            });
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void putSetting(IConfigManager configManager, Setting<?> setting, Enum<?> value) {
        putSettingUnchecked(configManager, (Setting) setting, value);
    }

    private static <T extends Enum<T>> void putSettingUnchecked(IConfigManager configManager, Setting<T> setting, Enum<?> value) {
        configManager.putSetting(setting, setting.getEnumClass().cast(value));
    }
}
