package thaumicenergistics.api.storage;

import ae2.api.storage.ITerminalHost;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import thaumicenergistics.init.ModGUIs;

public interface IArcaneTerminalHost extends ITerminalHost {

    ModGUIs getGui();

    IItemHandler getInventoryByName(String name);

    boolean hasVisSource();

    World getVisWorld();

    BlockPos getVisPos();

    BlockPos getReturnPos();

    EnumFacing getReturnSide();
}
