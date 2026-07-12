package thaumicenergistics.common.crafting;

import ae2.api.stacks.AEItemKey;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable worker-safe description of one assembler and the aura chunks it can drain.
 *
 * @param stableId           coordinate-derived provider identity used only for deterministic allocation order
 * @param patternDefinitions the complete encoded definitions this provider actually advertises
 * @param reachableChunks    the distinct chunks this provider can drain for one craft
 */
public record ArcaneVisProviderSnapshot(
    String stableId,
    Set<AEItemKey> patternDefinitions,
    List<ArcaneVisChunk> reachableChunks) {

    public ArcaneVisProviderSnapshot {
        if (stableId == null || stableId.isBlank()) {
            throw new IllegalArgumentException("Arcane Vis provider stable id cannot be blank");
        }
        Objects.requireNonNull(patternDefinitions, "patternDefinitions");
        Objects.requireNonNull(reachableChunks, "reachableChunks");
        patternDefinitions = Set.copyOf(new HashSet<>(patternDefinitions));
        reachableChunks = reachableChunks.stream()
            .map(chunk -> Objects.requireNonNull(chunk, "reachableChunks entry"))
            .distinct()
            .sorted()
            .toList();
    }
}
