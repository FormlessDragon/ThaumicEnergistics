package thaumicenergistics.config;

import ae2.api.config.Setting;
import ae2.api.config.Settings;
import ae2.api.util.IConfigManager;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Imports old uppercase AE setting keys written by earlier Thaumic Energistics builds.
 */
public final class LegacyAESettingKeys {
    private static final Logger LOG = LoggerFactory.getLogger(LegacyAESettingKeys.class);
    private static final Map<Setting<?>, String> LEGACY_NAMES = Map.of(
            Settings.SORT_BY, "SORT_BY",
            Settings.SORT_DIRECTION, "SORT_DIRECTION",
            Settings.VIEW_MODE, "VIEW_MODE",
            Settings.REDSTONE_CONTROLLED, "REDSTONE_CONTROLLED",
            Settings.ACCESS, "ACCESS",
            Settings.STORAGE_FILTER, "STORAGE_FILTER");

    private LegacyAESettingKeys() {
    }

    /**
     * Callers should let Supergiant read current lowercase NBT first; legacy uppercase keys are imported only when
     * the current key is absent. A malformed current key returns a failure and blocks legacy import for that setting.
     */
    public static ImportResult importFrom(NBTTagCompound tag, IConfigManager configManager) {
        boolean changed = false;
        List<ImportFailure> failures = new ArrayList<>();
        for (Setting<?> setting : configManager.getSettings()) {
            String legacyKey = LEGACY_NAMES.get(setting);
            if (legacyKey == null || !tag.hasKey(legacyKey)) {
                continue;
            }
            if (tag.hasKey(setting.getName())) {
                if (!tag.hasKey(setting.getName(), Constants.NBT.TAG_STRING)) {
                    ImportFailure failure = failedCurrentType(setting, tag.getTag(setting.getName()));
                    failures.add(failure);
                }
                continue;
            }
            if (!tag.hasKey(legacyKey, Constants.NBT.TAG_STRING)) {
                ImportFailure failure = failedType(legacyKey, setting, tag.getTag(legacyKey));
                failures.add(failure);
                continue;
            }
            ImportAttempt attempt = importSetting(tag, configManager, setting, legacyKey);
            changed |= attempt.changed();
            if (attempt.failure() != null) {
                failures.add(attempt.failure());
            }
        }
        return new ImportResult(changed, List.copyOf(failures));
    }

    private static ImportFailure failedType(String legacyKey, Setting<?> setting, NBTBase tag) {
        String value = tag.toString();
        String message = "Expected TAG_String for legacy AE setting " + legacyKey + " but found NBT tag id "
                + tag.getId();
        LOG.warn("Failed to import legacy AE setting {} into {} from non-string value '{}': {}", legacyKey,
                setting.getName(), value, message);
        return new ImportFailure(legacyKey, setting, value, message);
    }

    private static ImportFailure failedCurrentType(Setting<?> setting, NBTBase tag) {
        String value = tag.toString();
        String currentKey = setting.getName();
        String message = "Expected TAG_String for current AE setting " + currentKey + " but found NBT tag id "
                + tag.getId();
        LOG.warn("Failed to import legacy AE setting for {} because current key has non-string value '{}': {}",
                currentKey, value, message);
        return new ImportFailure(currentKey, setting, value, message);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ImportAttempt importSetting(NBTTagCompound tag, IConfigManager configManager, Setting<?> setting,
                                               String legacyKey) {
        return importSettingUnchecked(tag, configManager, (Setting) setting, legacyKey);
    }

    private static <T extends Enum<T>> ImportAttempt importSettingUnchecked(NBTTagCompound tag,
                                                                            IConfigManager configManager,
                                                                            Setting<T> setting,
                                                                            String legacyKey) {
        T oldValue = configManager.getSetting(setting);
        String value = tag.getString(legacyKey);
        try {
            setting.setFromString(configManager, value);
        } catch (IllegalArgumentException e) {
            LOG.warn("Failed to import legacy AE setting {} into {} from value '{}': {}", legacyKey,
                    setting.getName(), value, e.getMessage());
            return new ImportAttempt(false, new ImportFailure(legacyKey, setting, value, e.getMessage()));
        }
        return new ImportAttempt(oldValue != configManager.getSetting(setting), null);
    }

    public record ImportResult(boolean changed, List<ImportFailure> failures) {
    }

    public record ImportFailure(String legacyKey, Setting<?> setting, String value, String message) {
    }

    private record ImportAttempt(boolean changed, ImportFailure failure) {
    }
}
