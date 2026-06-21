package thaumicenergistics.core.definitions;

import ae2.api.upgrades.Upgrades;
import ae2.core.definitions.BlockDefinition;
import net.minecraft.block.Block;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.api.ids.ThEBlockIds;
import thaumicenergistics.block.BlockArcaneAssembler;
import thaumicenergistics.block.BlockInfusionProvider;
import thaumicenergistics.init.ThEBlocks;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupergiantDefinitionMigrationTest {

    @Test
    void thaumicBlocksUseSupergiantBlockDefinitions() {
        bootstrapMinecraft();

        assertBlockDefinition(
                ThEBlocks.INFUSION_PROVIDER,
                ThaumicEnergistics.id("infusion_provider"),
                BlockInfusionProvider.class);
        assertBlockDefinition(
                ThEBlocks.ARCANE_ASSEMBLER,
                ThaumicEnergistics.id("arcane_assembler"),
                BlockArcaneAssembler.class);
    }

    @Test
    void thaumicBlockIdsUseThaumicEnergisticsIds() {
        assertEquals(ThaumicEnergistics.id("infusion_provider"), ThEBlockIds.INFUSION_PROVIDER);
        assertEquals(ThaumicEnergistics.id("arcane_assembler"), ThEBlockIds.ARCANE_ASSEMBLER);
    }

    @Test
    void thaumicBlockDefinitionsAreExposedAsAClonedArray() {
        bootstrapMinecraft();

        BlockDefinition<?>[] definitions = ThEBlocks.all();

        assertTrue(Arrays.stream(definitions).anyMatch(definition -> definition == ThEBlocks.INFUSION_PROVIDER));
        assertTrue(Arrays.stream(definitions).anyMatch(definition -> definition == ThEBlocks.ARCANE_ASSEMBLER));

        BlockDefinition<?>[] anotherSnapshot = ThEBlocks.all();
        assertNotSame(definitions, anotherSnapshot);
        assertArrayEquals(definitions, anotherSnapshot);

        definitions[0] = null;
        assertArrayEquals(anotherSnapshot, ThEBlocks.all());
    }

    @Test
    void supergiantUpgradeRegistryAcceptsThaumicDefinitions() {
        bootstrapMinecraft();

        Upgrades.add(ThEItems.UPGRADE_ARCANE.item(), ThEBlocks.ARCANE_ASSEMBLER.item(), 1);
        Upgrades.add(ThEItems.BLANK_KNOWLEDGE_CORE.item(), ThEParts.ARCANE_INSCRIBER.item(), 1);
        Upgrades.add(ThEItems.KNOWLEDGE_CORE.item(), ThEParts.ARCANE_INSCRIBER.item(), 1);

        assertEquals(1, Upgrades.getMaxInstallable(ThEItems.UPGRADE_ARCANE.item(), ThEBlocks.ARCANE_ASSEMBLER.item()));
        assertEquals(1, Upgrades.getMaxInstallable(ThEItems.BLANK_KNOWLEDGE_CORE.item(), ThEParts.ARCANE_INSCRIBER.item()));
        assertEquals(1, Upgrades.getMaxInstallable(ThEItems.KNOWLEDGE_CORE.item(), ThEParts.ARCANE_INSCRIBER.item()));
    }

    private static <T extends Block> void assertBlockDefinition(
            BlockDefinition<T> definition,
            ResourceLocation id,
            Class<T> blockType) {
        T block = definition.block();
        ItemBlock item = definition.item();

        assertAll(
                () -> assertEquals(id, definition.id()),
                () -> assertInstanceOf(blockType, block),
                () -> assertNotNull(item),
                () -> assertEquals(block, item.getBlock()),
                () -> assertEquals(id, block.getRegistryName()),
                () -> assertEquals(id, item.getRegistryName()),
                () -> assertEquals("tile." + id.getNamespace() + "." + id.getPath(), block.getTranslationKey()),
                () -> assertEquals(block.getTranslationKey(), item.getTranslationKey()));
    }

    private static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }
}
