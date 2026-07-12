package thaumicenergistics.common.crafting;

import ae2.api.networking.IGrid;
import ae2.api.networking.crafting.ICraftingProvider;

import java.util.List;

/**
 * Captures mutable grid, provider, and aura state into an immutable worker-safe snapshot.
 * Callers must invoke this interface on the server thread before scheduling crafting calculation work.
 */
public interface ArcaneVisSnapshotFactory {

    /**
     * Captures active grid assemblers plus simulation-only temporary providers and deduplicates shared chunks.
     *
     * @param grid               the grid used by the pending crafting calculation
     * @param temporaryProviders additional providers supplied by the calculation requester
     * @return immutable provider and aura snapshot
     */
    ArcaneVisSnapshot capture(IGrid grid, List<ICraftingProvider> temporaryProviders);
}
