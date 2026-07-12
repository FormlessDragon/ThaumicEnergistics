package thaumicenergistics.common.crafting;

import ae2.api.networking.IGrid;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.stacks.AEItemKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumcraft.api.aura.AuraHelper;
import thaumicenergistics.core.ThELog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Server-thread {@link ArcaneVisSnapshotFactory} implementation for arcane assemblers.
 */
public final class ArcaneVisSnapshotFactoryImpl implements ArcaneVisSnapshotFactory {

    public static final ArcaneVisSnapshotFactory INSTANCE = new ArcaneVisSnapshotFactoryImpl();

    private final ArcaneVisAccounting accounting;

    public ArcaneVisSnapshotFactoryImpl() {
        this(new ArcaneVisAccountingImpl());
    }

    ArcaneVisSnapshotFactoryImpl(ArcaneVisAccounting accounting) {
        this.accounting = Objects.requireNonNull(accounting, "accounting");
    }

    @Override
    public ArcaneVisSnapshot capture(IGrid grid, List<ICraftingProvider> temporaryProviders) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(temporaryProviders, "temporaryProviders");

        Set<ArcaneVisProvider> sources = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Class<?> machineClass : grid.getMachineClasses()) {
            for (Object machine : grid.getActiveMachines(machineClass)) {
                if (machine instanceof ArcaneVisProvider arcaneProvider) {
                    sources.add(arcaneProvider);
                }
            }
        }
        for (ICraftingProvider provider : temporaryProviders) {
            if (provider instanceof ArcaneVisProvider arcaneProvider) {
                sources.add(arcaneProvider);
            }
        }

        List<ArcaneVisProviderSnapshot> providers = new ArrayList<>();
        Map<ArcaneVisChunk, Long> budgets = new LinkedHashMap<>();
        for (ArcaneVisProvider source : sources) {
            captureProvider(source, providers, budgets);
        }
        return new ArcaneVisSnapshotImpl(providers, budgets);
    }

    private void captureProvider(
        ArcaneVisProvider source,
        List<ArcaneVisProviderSnapshot> providers,
        Map<ArcaneVisChunk, Long> budgets) {
        World world = source.getArcaneVisWorld();
        BlockPos position = source.getArcaneVisPosition();
        if (world == null || position == null) {
            ThELog.error("Cannot snapshot detached Arcane Vis provider {}", source);
            return;
        }
        int radius;
        try {
            radius = validateChunkRadius(source.getArcaneVisChunkRadius());
        } catch (IllegalArgumentException exception) {
            ThELog.error("Arcane Vis provider @ {} reported invalid chunk radius: {}", position, exception.getMessage());
            return;
        }

        Set<AEItemKey> definitions = new LinkedHashSet<>(source.getArcaneVisPatternDefinitions());
        if (definitions.isEmpty()) {
            return;
        }

        int dimension = world.provider.getDimension();
        String stableId = dimension + ":" + position.getX() + ":" + position.getY() + ":" + position.getZ();
        List<ArcaneVisChunk> reachableChunks = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        int originChunkX = position.getX() >> 4;
        int originChunkZ = position.getZ() >> 4;
        for (int offsetX = -radius; offsetX <= radius; offsetX++) {
            for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                ArcaneVisChunk chunk = new ArcaneVisChunk(
                    dimension,
                    originChunkX + offsetX,
                    originChunkZ + offsetZ);
                reachableChunks.add(chunk);
                if (!budgets.containsKey(chunk)) {
                    BlockPos auraPosition = position.add(offsetX * 16, 0, offsetZ * 16);
                    budgets.put(chunk, captureAvailableUnits(world, auraPosition, chunk));
                }
            }
        }
        providers.add(new ArcaneVisProviderSnapshot(stableId, definitions, reachableChunks));
    }

    static int validateChunkRadius(int radius) {
        if (radius < 0 || radius > ArcaneVisProvider.MAX_CHUNK_RADIUS) {
            throw new IllegalArgumentException(
                radius + " is outside 0.." + ArcaneVisProvider.MAX_CHUNK_RADIUS);
        }
        return radius;
    }

    private long captureAvailableUnits(World world, BlockPos position, ArcaneVisChunk chunk) {
        float vis = AuraHelper.getVis(world, position);
        try {
            return this.accounting.availableUnits(vis);
        } catch (IllegalArgumentException exception) {
            ThELog.error("Aura chunk {} reported invalid Vis {}: {}", chunk, vis, exception.getMessage());
            return 0;
        }
    }
}
