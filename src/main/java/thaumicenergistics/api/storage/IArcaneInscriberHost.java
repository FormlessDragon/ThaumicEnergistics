package thaumicenergistics.api.storage;

import ae2.api.inventories.InternalInventory;

/**
 * Specialized host contract for Arcane Inscribers.
 * <p>
 * The dedicated contract prevents knowledge cores from being treated as generic AE2 upgrades and gives inscriber
 * containers a compile-time boundary for their single real inventory slot.
 */
public interface IArcaneInscriberHost extends IArcaneTerminalHost {

    /**
     * Returns the single-slot AE2-native inventory that stores the installed knowledge core.
     */
    InternalInventory getKnowledgeCoreInventory();
}
