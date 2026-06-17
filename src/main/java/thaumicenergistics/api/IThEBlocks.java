package thaumicenergistics.api;

import ae2.core.definitions.BlockDefinition;
import thaumicenergistics.block.BlockArcaneAssembler;
import thaumicenergistics.block.BlockInfusionProvider;

/**
 * Contains functions that return the Block/Tile Definitions for each block/tile in Thaumic Energistics
 *
 * @author BrockWS
 * @version 1.0.0
 * @since 1.0.0
 */
@Deprecated
public interface IThEBlocks {

    BlockDefinition<BlockInfusionProvider> infusionProvider();

    BlockDefinition<BlockArcaneAssembler> arcaneAssembler();
}
