package thaumicenergistics.integration.appeng.grid;

import ae2.api.networking.IGridNode;
import thaumicenergistics.integration.appeng.compat.DimensionalCoord;

/**
 * @author BrockWS
 */
public interface IThEGridHost {
    IGridNode getGridNode();

    DimensionalCoord getLocation();

    void gridChanged();
}
