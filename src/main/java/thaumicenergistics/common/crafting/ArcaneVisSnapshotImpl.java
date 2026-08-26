package thaumicenergistics.common.crafting;

import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validated immutable {@link ArcaneVisSnapshot} implementation used by the crafting worker.
 */
public final class ArcaneVisSnapshotImpl implements ArcaneVisSnapshot {

    private static final ArcaneVisSnapshot EMPTY = new ArcaneVisSnapshotImpl(List.of(), Object2LongMaps.emptyMap());

    private final List<ArcaneVisProviderSnapshot> providers;
    private final Object2LongMap<ArcaneVisChunk> availableUnits;

    public ArcaneVisSnapshotImpl(
        List<ArcaneVisProviderSnapshot> providers,
        Object2LongMap<ArcaneVisChunk> availableUnits) {
        Objects.requireNonNull(providers, "providers");
        Objects.requireNonNull(availableUnits, "availableUnits");

        List<ArcaneVisProviderSnapshot> providerCopy = new ArrayList<>(providers.size());
        Set<String> providerIds = new HashSet<>();
        for (ArcaneVisProviderSnapshot provider : providers) {
            ArcaneVisProviderSnapshot checked = Objects.requireNonNull(provider, "providers entry");
            if (!providerIds.add(checked.stableId())) {
                throw new IllegalArgumentException("Duplicate Arcane Vis provider id: " + checked.stableId());
            }
            providerCopy.add(checked);
        }
        providerCopy.sort(Comparator.comparing(ArcaneVisProviderSnapshot::stableId));
        this.providers = List.copyOf(providerCopy);

        List<Object2LongMap.Entry<ArcaneVisChunk>> budgets = new ArrayList<>(availableUnits.object2LongEntrySet());
        budgets.sort(Map.Entry.comparingByKey());
        Object2LongMap<ArcaneVisChunk> budgetCopy = new Object2LongLinkedOpenHashMap<>();
        for (var budget : budgets) {
            ArcaneVisChunk chunk = Objects.requireNonNull(budget.getKey(), "availableUnits key");
            long amount = budget.getLongValue();
            if (amount < 0) {
                throw new IllegalArgumentException("Arcane Vis chunk budget cannot be negative: " + chunk);
            }
            budgetCopy.put(chunk, amount);
        }
        this.availableUnits = Object2LongMaps.unmodifiable(budgetCopy);
    }

    public static ArcaneVisSnapshot empty() {
        return EMPTY;
    }

    @Override
    public List<ArcaneVisProviderSnapshot> providers() {
        return this.providers;
    }

    @Override
    public Object2LongMap<ArcaneVisChunk> availableUnits() {
        return this.availableUnits;
    }
}
