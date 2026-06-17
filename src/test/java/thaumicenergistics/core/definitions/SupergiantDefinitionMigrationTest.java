package thaumicenergistics.core.definitions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupergiantDefinitionMigrationTest {

    private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

    @Test
    void legacyThaumicDefinitionPackageIsRemoved() throws IOException {
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/api/definitions/IThEItemDefinition.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/api/definitions/IThEBlockDefinition.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/api/definitions/IThETileDefinition.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/core/definitions/ThEItemDefinition.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/core/definitions/ThEBlockDefinition.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/core/definitions/ThETileDefinition.java")));

        for (Path source : javaSources()) {
            String code = Files.readString(source);
            assertFalse(code.contains("thaumicenergistics.api.definitions"),
                    () -> source + " still imports the legacy definition package");
            assertFalse(code.contains("ThEItemDefinition"),
                    () -> source + " still references the legacy item definition wrapper");
            assertFalse(code.contains("ThEBlockDefinition"),
                    () -> source + " still references the legacy block definition wrapper");
            assertFalse(code.contains("ThETileDefinition"),
                    () -> source + " still references the legacy tile definition wrapper");
        }
    }

    @Test
    void thaumicBlocksUseSupergiantBlockDefinitions() throws IOException {
        String blocks = Files.readString(MAIN_SOURCES.resolve("thaumicenergistics/init/ThEBlocks.java"));

        assertTrue(blocks.contains("import ae2.core.definitions.BlockDefinition;"));
        assertTrue(blocks.contains("public static final BlockDefinition<BlockInfusionProvider> INFUSION_PROVIDER"));
        assertTrue(blocks.contains("public static final BlockDefinition<BlockArcaneAssembler> ARCANE_ASSEMBLER"));
        assertTrue(blocks.contains("event.getRegistry().register(definition.block())"));
        assertTrue(blocks.contains("event.getRegistry().register(definition.item())"));
        assertFalse(blocks.contains("public static List<BlockBase> BLOCKS"));
        assertFalse(blocks.contains("new ItemBlock(block)"));

        String blockIds = Files.readString(MAIN_SOURCES.resolve("thaumicenergistics/api/ids/ThEBlockIds.java"));
        assertTrue(blockIds.contains("INFUSION_PROVIDER = id(\"infusion_provider\")"));
        assertTrue(blockIds.contains("ARCANE_ASSEMBLER = id(\"arcane_assembler\")"));
    }

    @Test
    void upgradeRegistrationUsesSupergiantInitPattern() throws IOException {
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/api/IThEUpgrade.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/api/IThEUpgrades.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/upgrade/ThEUpgrade.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/upgrade/ThEUpgrades.java")));

        String mod = Files.readString(MAIN_SOURCES.resolve("thaumicenergistics/ThaumicEnergistics.java"));
        assertTrue(mod.contains("import thaumicenergistics.init.internal.InitUpgrades;"));
        assertTrue(mod.contains("InitUpgrades.init();"));
        assertFalse(mod.contains("registerUpgrade("));
        assertFalse(mod.contains("IThEUpgrades"));

        String initUpgrades = Files.readString(MAIN_SOURCES.resolve("thaumicenergistics/init/internal/InitUpgrades.java"));
        assertTrue(initUpgrades.contains("import ae2.api.upgrades.Upgrades;"));
        assertTrue(initUpgrades.contains("Upgrades.add(AEItems.SPEED_CARD.item(), ThEBlocks.ARCANE_ASSEMBLER.item(), 5);"));
    }

    private static List<Path> javaSources() throws IOException {
        try (Stream<Path> walk = Files.walk(MAIN_SOURCES)) {
            return walk.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }
}
