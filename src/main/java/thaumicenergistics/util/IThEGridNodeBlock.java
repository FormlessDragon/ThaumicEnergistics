package thaumicenergistics.util;

import ae2.api.networking.IGridNode;
import thaumicenergistics.integration.appeng.grid.ThEGridBlock;

/**
 * @author Alex811
 */
public interface IThEGridNodeBlock {
    ThEGridBlock getGridBlock();

    IGridNode getGridNode();
}
