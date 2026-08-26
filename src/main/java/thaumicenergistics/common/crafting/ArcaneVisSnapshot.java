package thaumicenergistics.common.crafting;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.util.List;

/**
 * Immutable aura and provider view captured before AE2 moves a crafting calculation to its worker thread.
 * Implementations must not retain worlds, tiles, grid nodes, or other mutable server objects.
 */
public interface ArcaneVisSnapshot {

    /**
     * Returns the assemblers that can execute knowledge-core patterns in stable coordinate order.
     *
     * @return immutable provider snapshots
     */
    List<ArcaneVisProviderSnapshot> providers();

    /**
     * Returns each captured chunk's available aura exactly once, expressed as fixed-point Vis units.
     *
     * @return immutable, globally deduplicated chunk budgets
     */
    Object2LongMap<ArcaneVisChunk> availableUnits();
}
