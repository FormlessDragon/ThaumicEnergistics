package thaumicenergistics.config;

import ae2.api.config.AccessRestriction;
import ae2.api.config.RedstoneMode;
import ae2.api.config.Settings;
import ae2.api.config.SortDir;
import ae2.api.config.SortOrder;
import ae2.api.config.StorageFilter;
import ae2.api.config.ViewItems;
import ae2.api.util.IConfigManager;
import ae2.api.util.UnsupportedSettingException;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.core.definitions.ThEParts;
import thaumicenergistics.part.PartArcaneTerminal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupergiantConfigManagerMigrationTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void arcaneTerminalDefaultsAreRegisteredOnSupergiantConfigManager() {
        IConfigManager manager = AESettings.createConfigManager(AESettings.SUBJECT.ARCANE_TERMINAL, () -> {
        });

        assertAll(
                () -> assertEquals(SortOrder.NAME, manager.getSetting(Settings.SORT_BY)),
                () -> assertEquals(ViewItems.ALL, manager.getSetting(Settings.VIEW_MODE)),
                () -> assertEquals(SortDir.ASCENDING, manager.getSetting(Settings.SORT_DIRECTION)));
    }

    @Test
    void otherSubjectsRegisterTheirCoreDefaults() {
        IConfigManager essentiaTerminal = AESettings.createConfigManager(AESettings.SUBJECT.ESSENTIA_TERMINAL, () -> {
        });
        IConfigManager importBus = AESettings.createConfigManager(AESettings.SUBJECT.ESSENTIA_IMPORT_BUS, () -> {
        });
        IConfigManager exportBus = AESettings.createConfigManager(AESettings.SUBJECT.ESSENTIA_EXPORT_BUS, () -> {
        });
        IConfigManager storageBus = AESettings.createConfigManager(AESettings.SUBJECT.ESSENTIA_STORAGE_BUS, () -> {
        });

        assertAll(
                () -> assertEquals(SortOrder.NAME, essentiaTerminal.getSetting(Settings.SORT_BY)),
                () -> assertEquals(SortDir.ASCENDING, essentiaTerminal.getSetting(Settings.SORT_DIRECTION)),
                () -> assertEquals(RedstoneMode.IGNORE, importBus.getSetting(Settings.REDSTONE_CONTROLLED)),
                () -> assertEquals(RedstoneMode.IGNORE, exportBus.getSetting(Settings.REDSTONE_CONTROLLED)),
                () -> assertEquals(AccessRestriction.READ_WRITE, storageBus.getSetting(Settings.ACCESS)),
                () -> assertEquals(StorageFilter.EXTRACTABLE_ONLY, storageBus.getSetting(Settings.STORAGE_FILTER)));
    }

    @Test
    void supergiantManagerWritesAndExportsLowercaseSettingNames() {
        IConfigManager manager = AESettings.createConfigManager(AESettings.SUBJECT.ARCANE_TERMINAL, () -> {
        });
        manager.putSetting(Settings.SORT_BY, SortOrder.AMOUNT);
        NBTTagCompound tag = new NBTTagCompound();

        manager.writeToNBT(tag);

        assertAll(
                () -> assertEquals("AMOUNT", tag.getString("sort_by")),
                () -> assertFalse(tag.hasKey("SORT_BY")),
                () -> assertEquals("AMOUNT", manager.exportSettings().get("sort_by")),
                () -> assertFalse(manager.exportSettings().containsKey("SORT_BY")));
    }

    @Test
    void supergiantManagerReadsLowercaseKeyRoundTrip() {
        IConfigManager source = AESettings.createConfigManager(AESettings.SUBJECT.ARCANE_TERMINAL, () -> {
        });
        source.putSetting(Settings.SORT_BY, SortOrder.MOD);
        source.putSetting(Settings.VIEW_MODE, ViewItems.CRAFTABLE);
        source.putSetting(Settings.SORT_DIRECTION, SortDir.DESCENDING);
        NBTTagCompound tag = new NBTTagCompound();
        source.writeToNBT(tag);

        IConfigManager target = AESettings.createConfigManager(AESettings.SUBJECT.ARCANE_TERMINAL, () -> {
        });
        target.readFromNBT(tag);

        assertAll(
                () -> assertEquals(SortOrder.MOD, target.getSetting(Settings.SORT_BY)),
                () -> assertEquals(ViewItems.CRAFTABLE, target.getSetting(Settings.VIEW_MODE)),
                () -> assertEquals(SortDir.DESCENDING, target.getSetting(Settings.SORT_DIRECTION)));
    }

    @Test
    void legacyUppercaseNbtKeysCanBeImportedExplicitly() {
        IConfigManager manager = AESettings.createConfigManager(AESettings.SUBJECT.ARCANE_TERMINAL, () -> {
        });
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("SORT_BY", "AMOUNT");
        tag.setString("VIEW_MODE", "CRAFTABLE");
        tag.setString("SORT_DIRECTION", "DESCENDING");

        LegacyAESettingKeys.ImportResult result = LegacyAESettingKeys.importFrom(tag, manager);

        assertAll(
                () -> assertTrue(result.changed()),
                () -> assertTrue(result.failures().isEmpty()),
                () -> assertEquals(SortOrder.AMOUNT, manager.getSetting(Settings.SORT_BY)),
                () -> assertEquals(ViewItems.CRAFTABLE, manager.getSetting(Settings.VIEW_MODE)),
                () -> assertEquals(SortDir.DESCENDING, manager.getSetting(Settings.SORT_DIRECTION)));
    }

    @Test
    void arcaneTerminalReadFromNbtImportsLegacyUppercaseSettingsFromOldSaves() {
        PartArcaneTerminal terminal = ThEParts.ARCANE_TERMINAL.item().createPart();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("SORT_BY", "AMOUNT");
        tag.setString("VIEW_MODE", "CRAFTABLE");
        tag.setString("SORT_DIRECTION", "DESCENDING");

        terminal.readFromNBT(tag);

        IConfigManager manager = terminal.getConfigManager();
        assertAll(
                () -> assertEquals(SortOrder.AMOUNT, manager.getSetting(Settings.SORT_BY)),
                () -> assertEquals(ViewItems.CRAFTABLE, manager.getSetting(Settings.VIEW_MODE)),
                () -> assertEquals(SortDir.DESCENDING, manager.getSetting(Settings.SORT_DIRECTION)));
    }

    @Test
    void lowercaseKeysTakePrecedenceOverLegacyUppercaseKeys() {
        IConfigManager manager = AESettings.createConfigManager(AESettings.SUBJECT.ARCANE_TERMINAL, () -> {
        });
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("sort_by", "MOD");
        tag.setString("SORT_BY", "AMOUNT");

        manager.readFromNBT(tag);
        LegacyAESettingKeys.ImportResult result = LegacyAESettingKeys.importFrom(tag, manager);

        assertAll(
                () -> assertFalse(result.changed()),
                () -> assertTrue(result.failures().isEmpty()),
                () -> assertEquals(SortOrder.MOD, manager.getSetting(Settings.SORT_BY)));
    }

    @Test
    void malformedLowercaseKeyStillTakesPrecedenceOverLegacyUppercaseKeyAndIsDiagnosable() {
        IConfigManager manager = AESettings.createConfigManager(AESettings.SUBJECT.ARCANE_TERMINAL, () -> {
        });
        manager.putSetting(Settings.SORT_BY, SortOrder.MOD);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("sort_by", 1);
        tag.setString("SORT_BY", "AMOUNT");

        LegacyAESettingKeys.ImportResult result = LegacyAESettingKeys.importFrom(tag, manager);

        assertAll(
                () -> assertFalse(result.changed()),
                () -> assertEquals(SortOrder.MOD, manager.getSetting(Settings.SORT_BY)),
                () -> assertEquals(1, result.failures().size()),
                () -> assertEquals("sort_by", result.failures().get(0).legacyKey()),
                () -> assertEquals(Settings.SORT_BY, result.failures().get(0).setting()),
                () -> assertEquals("1", result.failures().get(0).value()),
                () -> assertTrue(result.failures().get(0).message().contains("current AE setting")),
                () -> assertTrue(result.failures().get(0).message().contains("TAG_String")));
    }

    @Test
    void invalidLegacyValueDoesNotChangeExistingValueAndIsDiagnosable() {
        IConfigManager manager = AESettings.createConfigManager(AESettings.SUBJECT.ARCANE_TERMINAL, () -> {
        });
        manager.putSetting(Settings.SORT_BY, SortOrder.MOD);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("SORT_BY", "NOT_A_SORT");

        LegacyAESettingKeys.ImportResult result = LegacyAESettingKeys.importFrom(tag, manager);

        assertAll(
                () -> assertFalse(result.changed()),
                () -> assertEquals(SortOrder.MOD, manager.getSetting(Settings.SORT_BY)),
                () -> assertEquals(1, result.failures().size()),
                () -> assertEquals("SORT_BY", result.failures().get(0).legacyKey()),
                () -> assertEquals(Settings.SORT_BY, result.failures().get(0).setting()),
                () -> assertEquals("NOT_A_SORT", result.failures().get(0).value()));
    }

    @Test
    void nonStringLegacyValueDoesNotChangeExistingValueAndIsDiagnosable() {
        IConfigManager manager = AESettings.createConfigManager(AESettings.SUBJECT.ARCANE_TERMINAL, () -> {
        });
        manager.putSetting(Settings.SORT_BY, SortOrder.MOD);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("SORT_BY", 1);

        LegacyAESettingKeys.ImportResult result = LegacyAESettingKeys.importFrom(tag, manager);

        assertAll(
                () -> assertFalse(result.changed()),
                () -> assertEquals(SortOrder.MOD, manager.getSetting(Settings.SORT_BY)),
                () -> assertEquals(1, result.failures().size()),
                () -> assertEquals("SORT_BY", result.failures().get(0).legacyKey()),
                () -> assertEquals(Settings.SORT_BY, result.failures().get(0).setting()),
                () -> assertEquals("1", result.failures().get(0).value()),
                () -> assertTrue(result.failures().get(0).message().contains("TAG_String")));
    }

    @Test
    void unregisteredSettingsFailFast() {
        IConfigManager manager = AESettings.createConfigManager(AESettings.SUBJECT.ESSENTIA_IMPORT_BUS, () -> {
        });

        assertAll(
                () -> assertThrows(UnsupportedSettingException.class,
                        () -> manager.getSetting(Settings.SORT_BY)),
                () -> assertThrows(UnsupportedSettingException.class,
                        () -> manager.putSetting(Settings.SORT_BY, SortOrder.NAME)));
    }
}
