package thaumicenergistics.api.storage;

import ae2.api.inventories.InternalInventory;
import ae2.api.storage.ITerminalHost;
import ae2.api.upgrades.IUpgradeInventory;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.util.ThELog;

/**
 * Host contract for Thaumic Energistics arcane terminal containers.
 * <p>
 * Arcane terminal hosts expose two different inventory boundaries: the legacy named item-handler bridge and typed ThE
 * inventories. The typed inventories are the production path for ThE containers and logic. The legacy bridge remains
 * only for existing external ABI callers that still request Forge {@link IItemHandler} views by name.
 */
public interface IArcaneTerminalHost extends ITerminalHost {

    ModGUIs getGui();

    /**
     * Legacy named Forge item-handler bridge retained for external ABI callers.
     * <p>
     * New ThE production logic must prefer {@link #getArcaneCraftingInventory()} and
     * {@link #getArcaneUpgradeInventory()} so inventory consumers fail at compile time instead of through string names.
     */
    IItemHandler getInventoryByName(String name);

    /**
     * Returns the typed ThE arcane crafting matrix inventory for terminal-style hosts.
     * <p>
     * This default preserves binary compatibility for hosts that have not migrated yet. Production callers that require
     * the matrix must use hosts that override this method; otherwise the failure is explicit and logged.
     */
    default InternalInventory getArcaneCraftingInventory() {
        ThELog.error("Arcane terminal host {} does not expose a typed arcane crafting inventory",
                this.getClass().getName());
        throw new UnsupportedOperationException("Arcane terminal host does not expose typed arcane crafting inventory");
    }

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
