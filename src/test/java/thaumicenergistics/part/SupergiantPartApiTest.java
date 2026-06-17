package thaumicenergistics.part;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupergiantPartApiTest {

    private static final Path MAIN_SOURCES = Path.of("src", "main", "java");

    @Test
    void thaumicPartsUseSupergiantPartApiDirectly() throws IOException {
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/part/PartBase.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/part/PartSharedTerminal.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/items/ItemPartBase.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/items/part/ArcaneTerminalPart.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/items/part/ItemArcaneTerminal.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/items/part/ItemArcaneInscriber.java")));

        String arcaneTerminal = Files.readString(MAIN_SOURCES.resolve("thaumicenergistics/part/PartArcaneTerminal.java"));
        assertTrue(arcaneTerminal.contains("extends AbstractTerminalPart"));
        assertTrue(arcaneTerminal.contains("IPartItem<?>"));
        assertFalse(arcaneTerminal.contains("PartSharedTerminal"));
        assertFalse(arcaneTerminal.contains("PartBase"));

        List<Path> sources;
        try (Stream<Path> walk = Files.walk(MAIN_SOURCES)) {
            sources = walk.filter(path -> path.toString().endsWith(".java")).toList();
        }

        for (Path source : sources) {
            String code = Files.readString(source);
            assertFalse(code.contains("PartSharedTerminal"), () -> source + " still references PartSharedTerminal");
            assertFalse(code.contains("ItemPartBase"), () -> source + " still references ItemPartBase");
        }
    }

    @Test
    void thaumicPartsDoNotUseLegacyPartStackCompatibilityContexts() throws IOException {
        assertFalse(Files.exists(MAIN_SOURCES.resolve(
                "thaumicenergistics/integration/appeng/compat/ThEPartItemStack.java")));

        List<Path> sources;
        try (Stream<Path> walk = Files.walk(MAIN_SOURCES)) {
            sources = walk.filter(path -> path.toString().endsWith(".java")).toList();
        }

        for (Path source : sources) {
            String code = Files.readString(source);
            assertFalse(code.contains("ThEPartItemStack"), () -> source + " still references ThEPartItemStack");
        }

        String arcaneTerminal = Files.readString(MAIN_SOURCES.resolve("thaumicenergistics/part/PartArcaneTerminal.java"));
        assertTrue(arcaneTerminal.contains(
                "new ThEUpgradeInventory(\"upgrades\", 1, 1, this.getPartItem().asItemStack())"));
        assertFalse(arcaneTerminal.contains("getItemStack(ThEPartItemStack"));
    }

    @Test
    void upgradeInventoriesUseSupergiantUpgradeApiDirectly() throws IOException {
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/integration/appeng/compat/Upgrades.java")));

        List<Path> sources;
        try (Stream<Path> walk = Files.walk(MAIN_SOURCES)) {
            sources = walk.filter(path -> path.toString().endsWith(".java")).toList();
        }

        for (Path source : sources) {
            String code = Files.readString(source);
            assertFalse(code.contains("thaumicenergistics.integration.appeng.compat.Upgrades"),
                    () -> source + " still imports the legacy upgrade compatibility wrapper");
        }

        String upgradeInventory = Files.readString(MAIN_SOURCES.resolve(
                "thaumicenergistics/util/inventory/ThEUpgradeInventory.java"));
        assertTrue(upgradeInventory.contains("Upgrades.isUpgradeCardItem(stack)"));
        assertTrue(upgradeInventory.contains(
                "Upgrades.getMaxInstallable(upgradeStack.getItem(), this.upgradable.getItem())"));

        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/upgrade/ThEUpgrade.java")));
        String initUpgrades = Files.readString(MAIN_SOURCES.resolve("thaumicenergistics/init/internal/InitUpgrades.java"));
        assertTrue(initUpgrades.contains("Upgrades.add(AEItems.SPEED_CARD.item(), ThEBlocks.ARCANE_ASSEMBLER.item(), 5);"));
    }

    @Test
    void networkTilesUseSupergiantGridApiDirectly() throws IOException {
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/integration/appeng/compat/DimensionalCoord.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/integration/appeng/compat/GridAccessException.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/integration/appeng/grid/GridUtil.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/integration/appeng/grid/IThEGridHost.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/integration/appeng/grid/ThEGridBlock.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/util/IThEGridNodeBlock.java")));
        assertFalse(Files.exists(MAIN_SOURCES.resolve("thaumicenergistics/util/IThEOwnable.java")));

        String tileNetwork = Files.readString(MAIN_SOURCES.resolve("thaumicenergistics/tile/TileNetwork.java"));
        assertTrue(tileNetwork.contains("import ae2.block.IOwnerAwareTile;"));
        assertTrue(tileNetwork.contains("import ae2.api.AECapabilities;"));
        assertTrue(tileNetwork.contains("import ae2.api.networking.IInWorldGridNodeHost;"));
        assertTrue(tileNetwork.contains("import ae2.api.util.DimensionalBlockPos;"));
        assertTrue(tileNetwork.contains("implements IInWorldGridNodeHost, IActionHost, IPowerChannelState, IOwnerAwareTile"));
        assertTrue(tileNetwork.contains("new DimensionalBlockPos(this)"));
        assertTrue(tileNetwork.contains("AECapabilities.IN_WORLD_GRID_NODE_HOST"));
        assertTrue(tileNetwork.contains("return AECapabilities.IN_WORLD_GRID_NODE_HOST.cast(this);"));
        assertFalse(tileNetwork.contains("GridUtil"));
        assertFalse(tileNetwork.contains("GridAccessException"));
        assertFalse(tileNetwork.contains("ThEGridBlock"));
        assertFalse(tileNetwork.contains("IThEGridHost"));
        assertFalse(tileNetwork.contains("IThEGridNodeBlock"));
        assertFalse(tileNetwork.contains("IThEOwnable"));

        String infusionProvider = Files.readString(MAIN_SOURCES.resolve("thaumicenergistics/tile/TileInfusionProvider.java"));
        assertFalse(infusionProvider.contains("GridUtil"));
        assertFalse(infusionProvider.contains("GridAccessException"));
    }
}
