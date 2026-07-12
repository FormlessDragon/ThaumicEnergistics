package thaumicenergistics.common.crafting;

import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.stacks.AEItemKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Supplies the server-thread data required to snapshot one Vis-consuming crafting provider.
 * The provider itself never crosses into AE2's crafting worker.
 */
public interface ArcaneVisProvider extends ICraftingProvider {

    /**
     * Bounds both snapshot allocation and the authoritative square chunk-drain loop. The arcane range upgrade is
     * configured for one installation, so larger values indicate corrupted provider state.
     */
    int MAX_CHUNK_RADIUS = 1;

    /**
     * Identifies the world containing this provider so aura is read from the correct dimension.
     *
     * @return the live server world, or {@code null} while the provider is detached
     */
    World getArcaneVisWorld();

    /**
     * Locates the provider and anchors its square aura-drain range.
     *
     * @return the provider block position
     */
    BlockPos getArcaneVisPosition();

    /**
     * Defines the same chunk radius later passed to the authoritative aura drain operation.
     *
     * @return chunk radius from zero through {@link #MAX_CHUNK_RADIUS}
     */
    int getArcaneVisChunkRadius();

    /**
     * Lists only complete encoded definitions that this provider can execute.
     *
     * @return immutable pattern definitions in knowledge-core slot order
     */
    List<AEItemKey> getArcaneVisPatternDefinitions();
}
