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
     * Copies a concrete AE2 plan and adds atomically allocatable Vis to used and missing statistics.
     *
     * @param plan     the completed AE2 calculation result
     * @param snapshot the immutable provider/aura snapshot captured before worker execution
     * @return a concrete copied plan containing Vis statistics
     */
    CraftingPlan decorate(CraftingPlan plan, ArcaneVisSnapshot snapshot);

    /**
     * Copies a decorated plan while removing only the synthetic Vis key before CPU submission.
     *
     * @param plan plan previously shown to the submitting player
     * @return a concrete CPU-safe copied plan
     */
    CraftingPlan sanitize(CraftingPlan plan);
}
