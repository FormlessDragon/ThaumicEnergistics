package thaumicenergistics.container;

import ae2.api.config.Setting;
import ae2.api.util.IConfigurableObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IContainerListener;
import thaumicenergistics.config.AESettings;
import thaumicenergistics.integration.appeng.util.ThEConfigManager;
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
    protected ThEConfigManager serverConfigManager;
    protected ThEConfigManager clientConfigManager;

    public ContainerBaseConfigurable(EntityPlayer player, ThEConfigManager serverConfigManager) {
        super(player);
        this.clientConfigManager = new ThEConfigManager();
        this.clientConfigManager.registerSettings(this.getAESettingSubject());
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
    public ThEConfigManager getConfigManager() {
        return ForgeUtil.isClient() ? this.clientConfigManager : this.serverConfigManager;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Enum<?> getSetting(ThEConfigManager configManager, Setting<?> setting) {
        return configManager.getSetting((Setting) setting);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void putSetting(ThEConfigManager configManager, Setting<?> setting, Enum<?> value) {
        putSettingUnchecked(configManager, (Setting) setting, value);
    }

    private static <T extends Enum<T>> void putSettingUnchecked(ThEConfigManager configManager, Setting<T> setting, Enum<?> value) {
        configManager.putSetting(setting, setting.getEnumClass().cast(value));
    }
}
