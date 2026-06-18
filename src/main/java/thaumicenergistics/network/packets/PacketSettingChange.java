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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thaumicenergistics.client.gui.GuiBase;
import thaumicenergistics.config.ThESettings;

/**
 * @author BrockWS
 * @author Alex811
 */
public class PacketSettingChange implements IMessage {
    private static final Logger LOG = LoggerFactory.getLogger(PacketSettingChange.class);

    private String setting;
    private String value;

    public PacketSettingChange() {
    }

    public PacketSettingChange(Setting<?> setting, Enum<?> value) {
        this(setting.getName(), requireValue(setting, value).name());
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

    ValidationResult validate() {
        Setting<?> resolvedSetting = this.resolveSetting();
        if (resolvedSetting == null) {
            return ValidationResult.failed(new ValidationFailure(this.setting, this.value,
                    "Unknown config setting: " + this.setting));
        }
        Enum<?> resolvedValue = this.resolveValue(resolvedSetting);
        if (resolvedValue == null) {
            return ValidationResult.failed(new ValidationFailure(this.setting, this.value,
                    "Invalid value '" + this.value + "' for config setting " + resolvedSetting.getName()));
        }
        return ValidationResult.valid(resolvedSetting, resolvedValue);
    }

    private Enum<?> resolveValue(Setting<?> resolvedSetting) {
        if (this.value == null) {
            throw new IllegalArgumentException("Missing value for config setting " + resolvedSetting.getName());
        }
        for (Enum<?> e : resolvedSetting.getValues())
            if (e.name().equalsIgnoreCase(this.value))
                return e;
        return null;
    }

    public static class HandlerServer implements IMessageHandler<PacketSettingChange, IMessage> {

        @Override
        public IMessage onMessage(PacketSettingChange message, MessageContext ctx) {
            ValidationResult validation = message.validate();
            if (!validation.valid()) {
                LOG.warn("Rejected server config setting change packet: {}", validation.failure().message());
                return null;
            }
            Setting<?> resolvedSetting = validation.setting();
            Enum<?> resolvedValue = validation.value();

            NetHandlerPlayServer handler = ctx.getServerHandler();
            EntityPlayerMP player = handler.player;
            IThreadListener thread = (IThreadListener) player.world;
            thread.addScheduledTask(() -> {
                if (!(player.openContainer instanceof IConfigurableObject)) {
                    LOG.warn("Ignored server config setting change {}={} because open container {} is not configurable",
                            resolvedSetting.getName(), resolvedValue.name(), describeObject(player.openContainer));
                    return;
                }
                IConfigManager cm = ((IConfigurableObject) player.openContainer).getConfigManager();
                if (cm == null) {
                    LOG.warn("Ignored server config setting change {}={} because open container {} has no config manager",
                            resolvedSetting.getName(), resolvedValue.name(), describeObject(player.openContainer));
                    return;
                }
                if (!cm.getSettings().contains(resolvedSetting)) {
                    LOG.warn("Ignored server config setting change {}={} because config manager {} does not support it",
                            resolvedSetting.getName(), resolvedValue.name(), describeObject(cm));
                    return;
                }
                putSetting(cm, resolvedSetting, resolvedValue);
            });
            return null;
        }
    }

    public static class HandlerClient implements IMessageHandler<PacketSettingChange, IMessage> {

        @Override
        public IMessage onMessage(PacketSettingChange message, MessageContext ctx) {
            ValidationResult validation = message.validate();
            if (!validation.valid()) {
                LOG.warn("Rejected client config setting change packet: {}", validation.failure().message());
                return null;
            }
            Setting<?> resolvedSetting = validation.setting();
            Enum<?> resolvedValue = validation.value();

            Minecraft.getMinecraft().addScheduledTask(() -> {
                GuiScreen gui = Minecraft.getMinecraft().currentScreen;
                if (!(gui instanceof GuiBase)) {
                    LOG.warn("Ignored client config setting change {}={} because current screen {} is not a GuiBase",
                            resolvedSetting.getName(), resolvedValue.name(), describeObject(gui));
                    return;
                }
                GuiBase guiBase = (GuiBase) gui;
                if (!guiBase.hasConfigSetting(resolvedSetting)) {
                    LOG.warn("Ignored client config setting change {}={} because gui {} does not support it",
                            resolvedSetting.getName(), resolvedValue.name(), describeObject(guiBase));
                    return;
                }
                guiBase.updateSetting(resolvedSetting, resolvedValue);
            });
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void putSetting(IConfigManager configManager, Setting<?> setting, Enum<?> value) {
        putSettingUnchecked(configManager, (Setting) setting, value);
    }

    private static <T extends Enum<T>> void putSettingUnchecked(IConfigManager configManager, Setting<T> setting, Enum<?> value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot set config setting " + setting.getName() + " to null");
        }
        configManager.putSetting(setting, setting.getEnumClass().cast(value));
    }

    private static String describeObject(Object object) {
        return object == null ? "null" : object.getClass().getName();
    }

    private static Enum<?> requireValue(Setting<?> setting, Enum<?> value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing value for config setting " + setting.getName());
        }
        return value;
    }

    record ValidationResult(boolean valid, Setting<?> setting, Enum<?> value, ValidationFailure failure) {
        static ValidationResult valid(Setting<?> setting, Enum<?> value) {
            return new ValidationResult(true, setting, value, null);
        }

        static ValidationResult failed(ValidationFailure failure) {
            return new ValidationResult(false, null, null, failure);
        }
    }

    record ValidationFailure(String settingName, String valueName, String message) {
    }
}
