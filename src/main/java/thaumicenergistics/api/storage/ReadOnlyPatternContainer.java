package thaumicenergistics.api.storage;

import ae2.helpers.patternprovider.PatternContainer;

/**
 * Marks a pattern container whose terminal inventory is a read-only projection rather than physical pattern storage.
 *
 * <p>The marker exists because AE2's Pattern Access Terminal normally assumes that every exposed pattern can be moved
 * into a player's inventory or replaced. Implementors require server-side interaction guards so projected patterns
 * can still be searched, inspected, located, and opened without ever becoming obtainable items.</p>
 */
public interface ReadOnlyPatternContainer extends PatternContainer {
}
