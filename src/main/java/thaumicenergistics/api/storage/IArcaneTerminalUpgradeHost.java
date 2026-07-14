package thaumicenergistics.api.storage;

import ae2.api.upgrades.IUpgradeInventory;

/**
 * Specialized host contract for Arcane Terminal implementations that accept AE2 upgrade cards.
 * <p>
 * This interface was split from {@link IArcaneTerminalHost} because the Arcane Inscriber owns a knowledge-core slot,
 * not an AE2 upgrade inventory. Terminal containers depend on this contract whenever Vis-range upgrade behavior is
 * required.
 */
public interface IArcaneTerminalUpgradeHost extends IArcaneTerminalHost {

    /**
     * Returns the AE2-native upgrade inventory that controls Arcane Terminal upgrade behavior.
     */
    @Override
    IUpgradeInventory getUpgrades();

}
