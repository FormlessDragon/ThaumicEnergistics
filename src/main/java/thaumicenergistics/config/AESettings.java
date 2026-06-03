package thaumicenergistics.config;

import ae2.api.config.AccessRestriction;
import ae2.api.config.RedstoneMode;
import ae2.api.config.Setting;
import ae2.api.config.Settings;
import ae2.api.config.SortDir;
import ae2.api.config.SortOrder;
import ae2.api.config.StorageFilter;
import ae2.api.config.ViewItems;
import ae2.api.util.IConfigManager;
import thaumicenergistics.integration.appeng.util.ThEConfigManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

/**
 * Manages AE settings for Parts and TileEntities.
 *
 * @author Alex811
 */
public final class AESettings {
    private static final HashMap<SUBJECT, HashMap<Setting<?>, Enum<?>>> SETTINGS = new HashMap<>();

    public enum SUBJECT {
        ARCANE_TERMINAL,
        ESSENTIA_TERMINAL,
        ESSENTIA_IMPORT_BUS,
        ESSENTIA_EXPORT_BUS,
        ESSENTIA_STORAGE_BUS
    }

    static {
        addSetting(SUBJECT.ARCANE_TERMINAL, Settings.SORT_BY, SortOrder.NAME);
        addSetting(SUBJECT.ARCANE_TERMINAL, Settings.VIEW_MODE, ViewItems.ALL);
        addSetting(SUBJECT.ARCANE_TERMINAL, Settings.SORT_DIRECTION, SortDir.ASCENDING);

        addSetting(SUBJECT.ESSENTIA_TERMINAL, Settings.SORT_BY, SortOrder.NAME);
        addSetting(SUBJECT.ESSENTIA_TERMINAL, Settings.SORT_DIRECTION, SortDir.ASCENDING);

        addSetting(SUBJECT.ESSENTIA_IMPORT_BUS, Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);

        addSetting(SUBJECT.ESSENTIA_EXPORT_BUS, Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);

        addSetting(SUBJECT.ESSENTIA_STORAGE_BUS, Settings.ACCESS, AccessRestriction.READ_WRITE);
        addSetting(SUBJECT.ESSENTIA_STORAGE_BUS, Settings.STORAGE_FILTER, StorageFilter.EXTRACTABLE_ONLY);
    }

    private static void addSetting(SUBJECT settingSubject, Setting<?> setting, Enum<?> def) {
        if (!SETTINGS.containsKey(settingSubject))
            SETTINGS.put(settingSubject, new HashMap<>());
        SETTINGS.get(settingSubject).put(setting, def);
    }

    public static void registerSettings(@Nullable SUBJECT settingSubject, @Nonnull IConfigManager configManager) {
        if (settingSubject != null && configManager instanceof ThEConfigManager)
            SETTINGS.get(settingSubject).forEach((setting, value) -> registerSetting((ThEConfigManager) configManager, setting, value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerSetting(ThEConfigManager configManager, Setting<?> setting, Enum<?> value) {
        registerSettingUnchecked(configManager, (Setting) setting, value);
    }

    private static <T extends Enum<T>> void registerSettingUnchecked(ThEConfigManager configManager, Setting<T> setting, Enum<?> value) {
        configManager.registerSetting(setting, setting.getEnumClass().cast(value));
    }
}
