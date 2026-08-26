package thaumicenergistics.common.crafting;

import ae2.crafting.CraftingPlan;

/**
 * Converts Vis to fixed-point plan units and decorates AE2 plans without turning Vis into a CPU material.
 */
public interface ArcaneVisAccounting {

    /**
     * Converts one recipe's Vis cost to fixed-point units, rounding up so a craft is never undercharged.
     *
     * @param vis finite, non-negative recipe Vis
     * @return required units, saturated at {@link Long#MAX_VALUE}
     */
    long requiredUnits(float vis);

    /**
     * Converts one aura chunk's Vis to fixed-point units, rounding down so supply is never overstated.
     *
     * @param vis finite, non-negative aura Vis
     * @return available units, saturated at {@link Long#MAX_VALUE}
     */
    long availableUnits(float vis);

    /**
     * Adds atomically allocatable Vis to the mutable counters of a concrete AE2 plan.
     *
     * @param plan     the completed AE2 calculation result
     * @param snapshot the immutable provider/aura snapshot captured before worker execution
     * @return the same plan, with Vis statistics added to its counters
     */
    CraftingPlan decorate(CraftingPlan plan, ArcaneVisSnapshot snapshot);

    /**
     * Removes only the synthetic Vis key from a decorated plan before CPU submission.
     *
     * @param plan plan previously shown to the submitting player
     * @return the same plan, after its counters are made CPU-safe
     */
    CraftingPlan sanitize(CraftingPlan plan);
}
