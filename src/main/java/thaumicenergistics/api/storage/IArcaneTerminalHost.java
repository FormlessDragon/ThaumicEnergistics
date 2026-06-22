package thaumicenergistics.api.storage;

import ae2.api.storage.ITerminalHost;
import ae2.api.upgrades.IUpgradeInventory;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import thaumicenergistics.init.ModGUIs;

/**
 * Host contract for Thaumic Energistics arcane terminal containers.
 * <p>
 * Arcane terminal hosts expose two different inventory boundaries: the legacy named item-handler lookup and the typed
 * ThE arcane upgrade / knowledge-core inventory. The typed upgrade inventory is intentionally not AE2's generic
 * upgrade surface; it represents the ThE-specific arcane upgrade slot on terminals and the knowledge-core slot on
 * arcane inscribers. The explicit method removes the old {@code getInventoryByName("upgrades")} string boundary for
 * callers that need arcane upgrade semantics.
 */
public interface IArcaneTerminalHost extends ITerminalHost {

    ModGUIs getGui();

    IItemHandler getInventoryByName(String name);

    /**
     * Returns the ThE arcane upgrade / knowledge-core inventory for this host.
     * <p>
     * This is the typed boundary for ThE-specific arcane upgrade semantics, not a request for AE2 generic upgrades.
     * Part hosts override it so callers can avoid {@code getInventoryByName("upgrades")} and its stringly-typed
     * boundary.
     */
    IUpgradeInventory getArcaneUpgradeInventory();

    boolean hasVisSource();

    World getVisWorld();

    BlockPos getVisPos();

    BlockPos getReturnPos();

    EnumFacing getReturnSide();
}
