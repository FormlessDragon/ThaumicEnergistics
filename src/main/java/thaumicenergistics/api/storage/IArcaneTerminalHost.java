package thaumicenergistics.api.storage;

import ae2.api.inventories.InternalInventory;
import ae2.api.storage.ITerminalHost;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumicenergistics.client.gui.ModGUIs;

/**
 * Shared host contract for every arcane terminal-style GUI.
 * <p>
 * The contract exists because the Arcane Terminal and Arcane Inscriber share their matrix, GUI navigation and Vis
 * context, while their auxiliary inventories have different semantics. Consumers that need an upgrade slot or a
 * knowledge-core slot must depend on the corresponding specialized host interface.
 */
public interface IArcaneTerminalHost extends ITerminalHost {

    /**
     * Returns the concrete ThE GUI owned by this host so sub-GUIs can navigate back to the correct screen.
     */
    ModGUIs getGui();

    /**
     * Returns the fifteen-slot arcane matrix used by recipe lookup and matrix slot interactions.
     */
    InternalInventory getArcaneCraftingInventory();

    /**
     * Reports whether the host currently has a valid world location from which Vis can be queried.
     */
    boolean hasVisSource();

    /**
     * Returns the world containing the Vis source after {@link #hasVisSource()} has succeeded.
     */
    World getVisWorld();

    /**
     * Returns the block position used for Vis queries after {@link #hasVisSource()} has succeeded.
     */
    BlockPos getVisPos();

    /**
     * Returns the position used when the container navigates back from an arcane sub-GUI.
     */
    BlockPos getReturnPos();

    /**
     * Returns the side used when the container navigates back from an arcane sub-GUI.
     */
    EnumFacing getReturnSide();

}
