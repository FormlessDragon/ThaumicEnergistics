package thaumicenergistics.core.definitions;

import ae2.core.definitions.BlockDefinition;
import ae2.core.definitions.TileDefinition;
import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import thaumicenergistics.api.ids.ThEBlockIds;
import thaumicenergistics.block.BlockArcaneAssembler;
import thaumicenergistics.block.BlockInfusionProvider;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.tile.TileInfusionProvider;
import thaumicenergistics.util.ThELog;

public final class ThEBlocks {

    public static final BlockDefinition<BlockInfusionProvider> INFUSION_PROVIDER = new BlockDefinition<>(
            ThEBlockIds.INFUSION_PROVIDER, new BlockInfusionProvider(), ModGlobals.CREATIVE_TAB);
    public static final BlockDefinition<BlockArcaneAssembler> ARCANE_ASSEMBLER = new BlockDefinition<>(
            ThEBlockIds.ARCANE_ASSEMBLER, new BlockArcaneAssembler(), ModGlobals.CREATIVE_TAB);

    private static final BlockDefinition<?>[] BLOCKS = {
            INFUSION_PROVIDER,
            ARCANE_ASSEMBLER
    };

    private static final TileDefinition<?>[] TILES = {
            new TileDefinition<>(ThEBlockIds.INFUSION_PROVIDER, TileInfusionProvider.class),
            new TileDefinition<>(ThEBlockIds.ARCANE_ASSEMBLER, TileArcaneAssembler.class)
    };

    private ThEBlocks() {}

    public static void register(RegistryEvent.Register<Block> event) {
        ThELog.info("Registering Blocks");

        for (BlockDefinition<?> definition : BLOCKS) {
            event.getRegistry().register(definition.block());
        }

        for (TileDefinition<?> definition : TILES) {
            definition.register();
        }
    }

    public static BlockDefinition<?>[] all() {
        return BLOCKS.clone();
    }
}
