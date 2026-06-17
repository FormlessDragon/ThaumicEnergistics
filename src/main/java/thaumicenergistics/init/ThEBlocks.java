package thaumicenergistics.init;

import ae2.core.definitions.BlockDefinition;
import ae2.core.definitions.TileDefinition;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import thaumicenergistics.api.IThEBlocks;
import thaumicenergistics.api.ids.ThEBlockIds;
import thaumicenergistics.block.BlockArcaneAssembler;
import thaumicenergistics.block.BlockInfusionProvider;
import thaumicenergistics.client.render.IThEModel;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.tile.TileInfusionProvider;

import static thaumicenergistics.ThaumicEnergistics.LOGGER;

/**
 * @author BrockWS
 */
@Deprecated
@Mod.EventBusSubscriber
public class ThEBlocks implements IThEBlocks {

    public static final BlockDefinition<BlockInfusionProvider> INFUSION_PROVIDER = new BlockDefinition<>(
            ThEBlockIds.INFUSION_PROVIDER, new BlockInfusionProvider("infusion_provider"), ModGlobals.CREATIVE_TAB);
    public static final BlockDefinition<BlockArcaneAssembler> ARCANE_ASSEMBLER = new BlockDefinition<>(
            ThEBlockIds.ARCANE_ASSEMBLER, new BlockArcaneAssembler("arcane_assembler"), ModGlobals.CREATIVE_TAB);

    private static final BlockDefinition<?>[] BLOCKS = {
            INFUSION_PROVIDER,
            ARCANE_ASSEMBLER
    };

    private static final TileDefinition<?>[] TILES = {
            new TileDefinition<>(ThEBlockIds.INFUSION_PROVIDER, TileInfusionProvider.class),
            new TileDefinition<>(ThEBlockIds.ARCANE_ASSEMBLER, TileArcaneAssembler.class)
    };

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        LOGGER.info("Registering Blocks");

        for (BlockDefinition<?> definition : BLOCKS) {
            event.getRegistry().register(definition.block());
        }

        for (TileDefinition<?> definition : TILES) {
            definition.register();
        }
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        LOGGER.info("Registering ItemBlocks");

        for (BlockDefinition<?> definition : BLOCKS) {
            event.getRegistry().register(definition.item());
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (BlockDefinition<?> definition : BLOCKS) {
            if (definition.block() instanceof IThEModel model) {
                model.initModel();
            }
        }
    }

    public static BlockDefinition<?>[] all() {
        return BLOCKS.clone();
    }

    @Override
    public BlockDefinition<BlockInfusionProvider> infusionProvider() {
        return INFUSION_PROVIDER;
    }

    @Override
    public BlockDefinition<BlockArcaneAssembler> arcaneAssembler() {
        return ARCANE_ASSEMBLER;
    }
}
