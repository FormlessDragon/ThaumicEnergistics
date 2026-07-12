package thaumicenergistics.common.crafting;

import ae2.api.crafting.IPatternDetails;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.KeyCounter;
import ae2.crafting.CraftingPlan;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import thaumicenergistics.common.me.key.ArcaneVisKey;
import thaumicenergistics.common.me.key.ArcaneVisKeys;
import thaumicenergistics.core.ThELog;
import thaumicenergistics.util.knowledgeCoreUtil.KnowledgeCoreUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic conservative {@link ArcaneVisAccounting} implementation.
 */
public final class ArcaneVisAccountingImpl implements ArcaneVisAccounting {

    public static final int UNITS_PER_VIS = ArcaneVisKeys.AMOUNT_PER_VIS;

    private static final BigDecimal MAX_LONG = BigDecimal.valueOf(Long.MAX_VALUE);

    @Override
    public long requiredUnits(float vis) {
        return convert(vis, RoundingMode.CEILING, "recipe requirement");
    }

    @Override
    public long availableUnits(float vis) {
        return convert(vis, RoundingMode.FLOOR, "aura supply");
    }

    @Override
    public CraftingPlan decorate(CraftingPlan plan, ArcaneVisSnapshot snapshot) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(snapshot, "snapshot");

        KeyCounter usedItems = copyCounter(plan.usedItems());
        KeyCounter emittedItems = copyCounter(plan.emittedItems());
        KeyCounter missingItems = copyCounter(plan.missingItems());
        removeUnexpectedBaseVis(usedItems, "used");
        removeUnexpectedBaseVis(emittedItems, "emitted");
        removeUnexpectedBaseVis(missingItems, "missing");

        Map<ArcaneVisChunk, Long> remainingBudgets = new HashMap<>(snapshot.availableUnits());
        Map<ArcaneVisChunk, Integer> reachCounts = countProviderReach(snapshot.providers());
        List<VisDemand> demands = collectDemands(plan, snapshot.providers());
        long usedVis = 0;
        long missingVis = 0;
        for (VisDemand demand : demands) {
            if (demand.invalid()) {
                missingVis = saturatedAdd(missingVis, Long.MAX_VALUE, "invalid recipe Vis");
                continue;
            }
            if (demand.unitsPerCraft() == 0 || demand.craftCount() == 0) {
                continue;
            }
            long allocatedCrafts = allocateCrafts(demand, remainingBudgets, reachCounts);
            long missingCrafts = demand.craftCount() - allocatedCrafts;
            usedVis = saturatedAdd(
                usedVis,
                saturatedMultiply(allocatedCrafts, demand.unitsPerCraft(), "used Vis"),
                "total used Vis");
            missingVis = saturatedAdd(
                missingVis,
                saturatedMultiply(missingCrafts, demand.unitsPerCraft(), "missing Vis"),
                "total missing Vis");
        }
        if (usedVis > 0) {
            usedItems.add(ArcaneVisKey.INSTANCE, usedVis);
        }
        if (missingVis > 0) {
            missingItems.add(ArcaneVisKey.INSTANCE, missingVis);
        }
        return copyPlan(plan, usedItems, emittedItems, missingItems);
    }

    @Override
    public CraftingPlan sanitize(CraftingPlan plan) {
        Objects.requireNonNull(plan, "plan");
        KeyCounter usedItems = copyCounter(plan.usedItems());
        KeyCounter emittedItems = copyCounter(plan.emittedItems());
        KeyCounter missingItems = copyCounter(plan.missingItems());
        if (emittedItems.get(ArcaneVisKey.INSTANCE) != 0) {
            ThELog.error("Decorated crafting plan unexpectedly contains emitted Arcane Vis; removing it before CPU submission");
        }
        usedItems.remove(ArcaneVisKey.INSTANCE);
        emittedItems.remove(ArcaneVisKey.INSTANCE);
        missingItems.remove(ArcaneVisKey.INSTANCE);
        return copyPlan(plan, usedItems, emittedItems, missingItems);
    }

    private void removeUnexpectedBaseVis(KeyCounter counter, String counterName) {
        long amount = counter.get(ArcaneVisKey.INSTANCE);
        if (amount != 0) {
            ThELog.error("Base crafting plan unexpectedly contains {} Arcane Vis in its {} counter; replacing it",
                amount, counterName);
        }
        counter.remove(ArcaneVisKey.INSTANCE);
    }

    private long convert(float vis, RoundingMode roundingMode, String context) {
        if (!Float.isFinite(vis) || vis < 0.0F) {
            throw new IllegalArgumentException("Arcane Vis " + context + " must be finite and non-negative");
        }
        BigDecimal units = new BigDecimal(Float.toString(vis))
            .multiply(BigDecimal.valueOf(UNITS_PER_VIS))
            .setScale(0, roundingMode);
        if (units.compareTo(MAX_LONG) > 0) {
            ThELog.error("Arcane Vis {} {} exceeds the fixed-point range; saturating to {}",
                context, vis, Long.MAX_VALUE);
            return Long.MAX_VALUE;
        }
        return units.longValueExact();
    }

    private List<VisDemand> collectDemands(
        CraftingPlan plan,
        List<ArcaneVisProviderSnapshot> providers) {
        List<VisDemand> demands = new ArrayList<>();
        for (Object2LongMap.Entry<IPatternDetails> entry : plan.patternTimes().object2LongEntrySet()) {
            if (!(entry.getKey() instanceof KnowledgeCoreUtil.KnowledgeCorePatternDetails pattern)) {
                continue;
            }
            long craftCount = entry.getLongValue();
            if (craftCount < 0) {
                ThELog.error("Knowledge Core pattern {} has invalid negative craft count {}",
                    pattern.getDefinition(), craftCount);
                demands.add(new VisDemand(pattern.getDefinition(), Long.MAX_VALUE, 1, List.of(), true));
                continue;
            }
            long unitsPerCraft;
            try {
                unitsPerCraft = requiredUnits(pattern.getRecipe().visCost());
            } catch (IllegalArgumentException exception) {
                ThELog.error("Knowledge Core pattern {} has invalid Vis cost {}: {}",
                    pattern.getDefinition(), pattern.getRecipe().visCost(), exception.getMessage());
                demands.add(new VisDemand(pattern.getDefinition(), Long.MAX_VALUE, 1, List.of(), true));
                continue;
            }
            List<ArcaneVisProviderSnapshot> candidates = providers.stream()
                .filter(provider -> provider.patternDefinitions().contains(pattern.getDefinition()))
                .toList();
            demands.add(new VisDemand(pattern.getDefinition(), craftCount, unitsPerCraft, candidates, false));
        }
        demands.sort(Comparator
            .comparingInt((VisDemand demand) -> demand.candidates().size())
            .thenComparing(demand -> demand.definition().toTag().toString()));
        return demands;
    }

    private long allocateCrafts(
        VisDemand demand,
        Map<ArcaneVisChunk, Long> budgets,
        Map<ArcaneVisChunk, Integer> reachCounts) {
        long remainingCrafts = demand.craftCount();
        List<ArcaneVisProviderSnapshot> candidates = new ArrayList<>(demand.candidates());
        while (remainingCrafts > 0) {
            candidates.sort((left, right) -> compareProviders(left, right, budgets, reachCounts));
            boolean allocated = false;
            for (ArcaneVisProviderSnapshot provider : candidates) {
                long available = providerBudget(provider, budgets, false, reachCounts);
                long craftCapacity = available / demand.unitsPerCraft();
                long craftBatch = Math.min(remainingCrafts, craftCapacity);
                if (craftBatch <= 0) {
                    continue;
                }
                long units = saturatedMultiply(craftBatch, demand.unitsPerCraft(), "provider Vis allocation");
                drainProvider(provider, units, budgets, reachCounts);
                remainingCrafts -= craftBatch;
                allocated = true;
                break;
            }
            if (!allocated) {
                break;
            }
        }
        return demand.craftCount() - remainingCrafts;
    }

    private int compareProviders(
        ArcaneVisProviderSnapshot left,
        ArcaneVisProviderSnapshot right,
        Map<ArcaneVisChunk, Long> budgets,
        Map<ArcaneVisChunk, Integer> reachCounts) {
        long leftExclusive = providerBudget(left, budgets, true, reachCounts);
        long rightExclusive = providerBudget(right, budgets, true, reachCounts);
        int comparison = Long.compare(rightExclusive, leftExclusive);
        if (comparison == 0) {
            comparison = Long.compare(
                providerBudget(right, budgets, false, reachCounts),
                providerBudget(left, budgets, false, reachCounts));
        }
        return comparison == 0 ? left.stableId().compareTo(right.stableId()) : comparison;
    }

    private long providerBudget(
        ArcaneVisProviderSnapshot provider,
        Map<ArcaneVisChunk, Long> budgets,
        boolean exclusiveOnly,
        Map<ArcaneVisChunk, Integer> reachCounts) {
        long total = 0;
        for (ArcaneVisChunk chunk : provider.reachableChunks()) {
            if (!exclusiveOnly || reachCounts.getOrDefault(chunk, 0) == 1) {
                total = saturatedAdd(total, budgets.getOrDefault(chunk, 0L), "provider chunk budget");
            }
        }
        return total;
    }

    private void drainProvider(
        ArcaneVisProviderSnapshot provider,
        long units,
        Map<ArcaneVisChunk, Long> budgets,
        Map<ArcaneVisChunk, Integer> reachCounts) {
        List<ArcaneVisChunk> chunks = new ArrayList<>(provider.reachableChunks());
        chunks.sort(Comparator
            .comparingInt((ArcaneVisChunk chunk) -> reachCounts.getOrDefault(chunk, 0))
            .thenComparing(Comparator.naturalOrder()));
        long remaining = units;
        for (ArcaneVisChunk chunk : chunks) {
            long available = budgets.getOrDefault(chunk, 0L);
            long drained = Math.min(available, remaining);
            budgets.put(chunk, available - drained);
            remaining -= drained;
            if (remaining == 0) {
                return;
            }
        }
        ThELog.error("Arcane Vis allocation for provider {} overran its captured chunk budget by {} units",
            provider.stableId(), remaining);
    }

    private Map<ArcaneVisChunk, Integer> countProviderReach(List<ArcaneVisProviderSnapshot> providers) {
        Map<ArcaneVisChunk, Integer> reachCounts = new HashMap<>();
        for (ArcaneVisProviderSnapshot provider : providers) {
            for (ArcaneVisChunk chunk : provider.reachableChunks()) {
                reachCounts.merge(chunk, 1, Integer::sum);
            }
        }
        return reachCounts;
    }

    private CraftingPlan copyPlan(
        CraftingPlan plan,
        KeyCounter usedItems,
        KeyCounter emittedItems,
        KeyCounter missingItems) {
        return new CraftingPlan(
            plan.finalOutput(),
            plan.bytes(),
            plan.simulation(),
            plan.multiplePaths(),
            usedItems,
            emittedItems,
            missingItems,
            plan.intermediateFinalOutputAmount(),
            plan.patternTimes(),
            plan.tree(),
            plan.temporaryProviders());
    }

    private KeyCounter copyCounter(KeyCounter original) {
        KeyCounter copy = KeyCounter.saturating();
        copy.addAll(Objects.requireNonNull(original, "plan counter"));
        return copy;
    }

    private long saturatedAdd(long left, long right, String context) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            ThELog.error("Arcane Vis {} overflowed while adding {} and {}; saturating", context, left, right);
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private long saturatedMultiply(long left, long right, String context) {
        if (left > 0 && right > Long.MAX_VALUE / left) {
            ThELog.error("Arcane Vis {} overflowed while multiplying {} and {}; saturating", context, left, right);
            return Long.MAX_VALUE;
        }
        return left * right;
    }

    private record VisDemand(
        AEItemKey definition,
        long craftCount,
        long unitsPerCraft,
        List<ArcaneVisProviderSnapshot> candidates,
        boolean invalid) {
    }
}
