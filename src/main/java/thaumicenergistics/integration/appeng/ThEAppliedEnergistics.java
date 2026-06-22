package thaumicenergistics.integration.appeng;

import ae2.api.storage.StorageCells;
import thaumicenergistics.integration.IThEIntegration;
import thaumicenergistics.me.cell.CreativeEssentiaCellHandler;

/**
 * @author BrockWS
 */
public class ThEAppliedEnergistics implements IThEIntegration {

    @Override
    public void init() {
        StorageCells.addCellHandler(CreativeEssentiaCellHandler.INSTANCE);
    }

    @Override
    public void postInit() {

    }
}
