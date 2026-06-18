package thaumicenergistics.container;

import ae2.api.config.Setting;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IContainerListener;
import thaumicenergistics.config.AESettings;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketSettingChange;
import thaumicenergistics.util.ForgeUtil;

/**
 * Container that implements {@link IConfigurableObject} and syncs the client with the server.
 * If you're looking to register new settings, see {@link AESettings}.
 *
 * @author Alex811
 */
public abstract class ContainerBaseConfigurable extends ContainerBase implements IConfigurableObject {
    protected IConfigManager serverConfigManager;
    protected IConfigManager clientConfigManager;

    public ContainerBaseConfigurable(EntityPlayer player, IConfigManager serverConfigManager) {
        super(player);
        this.clientConfigManager = AESettings.createConfigManager(this.getAESettingSubject(), () -> {
        });
        if (ForgeUtil.isServer())
            this.serverConfigManager = serverConfigManager;
    }

    protected abstract AESettings.SUBJECT getAESettingSubject();

    @Override
    public void detectAndSendChanges() {
        if (ForgeUtil.isServer()) {
            if (this.listeners.isEmpty()) {
                // If the Player listener is not attached yet, no sense doing the checks
                return;
            }
            for (Setting<?> setting : this.serverConfigManager.getSettings()) {
                Enum<?> server = getSetting(this.serverConfigManager, setting);
                Enum<?> client = getSetting(this.clientConfigManager, setting);
                if (client != server) {
                    putSetting(this.clientConfigManager, setting, server);
                    for (IContainerListener player : this.listeners)
                        if (player instanceof EntityPlayerMP) {
                            PacketHandler.sendToPlayer((EntityPlayerMP) player, new PacketSettingChange(setting, server));
                        }
                }
            }
        }
        super.detectAndSendChanges();
    }

    @Override
    public IConfigManager getConfigManager() {
        return ForgeUtil.isClient() ? this.clientConfigManager : this.serverConfigManager;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Enum<?> getSetting(IConfigManager configManager, Setting<?> setting) {
        return configManager.getSetting((Setting) setting);
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
}
