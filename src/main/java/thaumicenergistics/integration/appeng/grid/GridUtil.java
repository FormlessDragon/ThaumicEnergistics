package thaumicenergistics.integration.appeng.grid;

import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridService;
import ae2.api.networking.crafting.ICraftingService;
import ae2.api.networking.energy.IEnergyService;
import ae2.api.networking.storage.IStorageService;
import thaumicenergistics.integration.appeng.compat.GridAccessException;

import javax.annotation.Nonnull;

/**
 * Grid helper class
 *
 * @author BrockWS
 */
@SuppressWarnings("ConstantConditions")
public class GridUtil {

    public static ICraftingService getCraftingGrid(@Nonnull IThEGridHost host) throws GridAccessException {
        return GridUtil.getCraftingGrid(GridUtil.getGrid(host));
    }

    public static ICraftingService getCraftingGrid(@Nonnull IGridNode node) throws GridAccessException {
        return GridUtil.getCraftingGrid(GridUtil.getGrid(node));
    }

    public static ICraftingService getCraftingGrid(@Nonnull IGrid grid) throws GridAccessException {
        return GridUtil.getService(grid, ICraftingService.class);
    }

    public static IEnergyService getEnergyGrid(@Nonnull IThEGridHost host) throws GridAccessException {
        return GridUtil.getEnergyGrid(GridUtil.getGrid(host));
    }

    public static IEnergyService getEnergyGrid(@Nonnull IGridNode node) throws GridAccessException {
        return GridUtil.getEnergyGrid(GridUtil.getGrid(node));
    }

    public static IEnergyService getEnergyGrid(@Nonnull IGrid grid) throws GridAccessException {
        return GridUtil.getService(grid, IEnergyService.class);
    }

    public static IStorageService getStorageGrid(@Nonnull IThEGridHost host) throws GridAccessException {
        return GridUtil.getStorageGrid(GridUtil.getGrid(host));
    }

    public static IStorageService getStorageGrid(@Nonnull IGridNode node) throws GridAccessException {
        return GridUtil.getStorageGrid(GridUtil.getGrid(node));
    }

    public static IStorageService getStorageGrid(@Nonnull IGrid grid) throws GridAccessException {
        return GridUtil.getService(grid, IStorageService.class);
    }

    public static <T extends IGridService> T getService(@Nonnull IGrid grid, @Nonnull Class<T> clazz) throws GridAccessException {
        T service = grid.getService(clazz);
        if (service == null)
            throw new GridAccessException();
        return service;
    }

    public static IGrid getGrid(@Nonnull IThEGridHost host) throws GridAccessException {
        IGridNode node = host.getGridNode();
        if (node == null)
            throw new GridAccessException();
        return GridUtil.getGrid(node);
    }

    public static IGrid getGrid(@Nonnull IGridNode node) throws GridAccessException {
        IGrid grid = node.grid();
        if (grid == null)
            throw new GridAccessException();
        return grid;
    }
}
