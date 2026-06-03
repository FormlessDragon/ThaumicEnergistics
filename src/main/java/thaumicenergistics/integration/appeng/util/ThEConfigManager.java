package thaumicenergistics.integration.appeng.util;

import ae2.api.config.Setting;
import ae2.api.config.Settings;
import ae2.api.util.IConfigManager;
import net.minecraft.nbt.NBTTagCompound;
import thaumicenergistics.config.AESettings;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author BrockWS
 */
public class ThEConfigManager implements IConfigManager {

    private final Map<Setting<?>, Enum<?>> settings = new HashMap<>();
    private static final Map<Setting<?>, String> LEGACY_NAMES = new HashMap<>();

    static {
        LEGACY_NAMES.put(Settings.SORT_BY, "SORT_BY");
        LEGACY_NAMES.put(Settings.SORT_DIRECTION, "SORT_DIRECTION");
        LEGACY_NAMES.put(Settings.VIEW_MODE, "VIEW_MODE");
        LEGACY_NAMES.put(Settings.REDSTONE_CONTROLLED, "REDSTONE_CONTROLLED");
        LEGACY_NAMES.put(Settings.ACCESS, "ACCESS");
        LEGACY_NAMES.put(Settings.STORAGE_FILTER, "STORAGE_FILTER");
    }

    public ThEConfigManager() {
    }

    public <T extends Enum<T>> void registerSetting(Setting<T> setting, T defaultValue) {
        this.settings.put(setting, defaultValue);
    }

    public void registerSettings(@Nullable AESettings.SUBJECT settingSubject) {
        AESettings.registerSettings(settingSubject, this);
    }

    @Override
    public <T extends Enum<T>> void putSetting(Setting<T> setting, T value) {
        if (!this.settings.containsKey(setting)) {
            throw new IllegalStateException("Setting '" + setting + "' has not been registered!");
        }
        if (value == null) {
            throw new IllegalArgumentException("Setting '" + setting + "' cannot be set to null!");
        }
        this.settings.put(setting, value);
    }

    @Override
    public <T extends Enum<T>> T getSetting(Setting<T> setting) {
        Enum<?> v = this.settings.get(setting);
        if (v == null)
            throw new IllegalStateException("Setting '" + setting + "' has not been registered!");
        return setting.getEnumClass().cast(v);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        this.settings.forEach((key, value) -> tag.setString(key.getName(), value.name()));
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        this.settings.forEach((key, old) -> {
            String keyName = key.getName();
            if (tag.hasKey(keyName)) {
                this.setFromString(key, tag.getString(keyName));
                return;
            }

            String legacyName = LEGACY_NAMES.get(key);
            if (legacyName != null && tag.hasKey(legacyName)) {
                this.setFromString(key, tag.getString(legacyName));
            }
        });
    }

    @Override
    public Set<Setting<?>> getSettings() {
        return this.settings.keySet();
    }

    @Override
    public boolean importSettings(Map<String, String> settings) {
        boolean changed = false;
        for (Map.Entry<Setting<?>, Enum<?>> entry : this.settings.entrySet()) {
            String value = settings.get(entry.getKey().getName());
            if (value != null) {
                changed |= this.setFromString(entry.getKey(), value);
            }
        }
        return changed;
    }

    @Override
    public Map<String, String> exportSettings() {
        Map<String, String> exported = new HashMap<>();
        this.settings.forEach((key, value) -> exported.put(key.getName(), value.name()));
        return exported;
    }

    private <T extends Enum<T>> boolean setFromString(Setting<T> setting, String value) {
        T oldValue = this.getSetting(setting);
        try {
            setting.setFromString(this, value);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
        return oldValue != this.getSetting(setting);
    }
}
