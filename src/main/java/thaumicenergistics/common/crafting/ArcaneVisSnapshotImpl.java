package thaumicenergistics.common.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validated immutable {@link ArcaneVisSnapshot} implementation used by the crafting worker.
 */
public final class ArcaneVisSnapshotImpl implements ArcaneVisSnapshot {

    private static final ArcaneVisSnapshot EMPTY = new ArcaneVisSnapshotImpl(List.of(), Map.of());

    private final List<ArcaneVisProviderSnapshot> providers;
    private final Map<ArcaneVisChunk, Long> availableUnits;

    public ArcaneVisSnapshotImpl(
        List<ArcaneVisProviderSnapshot> providers,
        Map<ArcaneVisChunk, Long> availableUnits) {
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
        providerCopy.sort(java.util.Comparator.comparing(ArcaneVisProviderSnapshot::stableId));
        this.providers = List.copyOf(providerCopy);

        List<Map.Entry<ArcaneVisChunk, Long>> budgets = new ArrayList<>(availableUnits.entrySet());
        budgets.sort(Map.Entry.comparingByKey());
        Map<ArcaneVisChunk, Long> budgetCopy = new LinkedHashMap<>();
        for (Map.Entry<ArcaneVisChunk, Long> budget : budgets) {
            ArcaneVisChunk chunk = Objects.requireNonNull(budget.getKey(), "availableUnits key");
            long amount = Objects.requireNonNull(budget.getValue(), "availableUnits value");
            if (amount < 0) {
                throw new IllegalArgumentException("Arcane Vis chunk budget cannot be negative: " + chunk);
            }
            budgetCopy.put(chunk, amount);
        }
        this.availableUnits = Collections.unmodifiableMap(budgetCopy);
    }

    public static ArcaneVisSnapshot empty() {
        return EMPTY;
    }

    @Override
    public List<ArcaneVisProviderSnapshot> providers() {
        return this.providers;
    }

    @Override
    public Map<ArcaneVisChunk, Long> availableUnits() {
        return this.availableUnits;
    }
}
