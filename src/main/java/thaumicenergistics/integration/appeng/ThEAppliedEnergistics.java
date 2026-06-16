package thaumicenergistics.integration.appeng;

import ae2.api.storage.StorageCells;
import thaumicenergistics.integration.IThEIntegration;
import thaumicenergistics.me.cell.CreativeEssentiaCellHandler;

/**
 * @author BrockWS
 */
public class ThEAppliedEnergistics implements IThEIntegration {

    private static final CreativeEssentiaCellHandler CREATIVE_ESSENTIA_CELL_HANDLER = new CreativeEssentiaCellHandler();
    private static boolean cellHandlerRegistered;

    @Override
    public void preInit() {
        // Stage 1: AE2 storage-channel registration is replaced by Supergiant AEKey registration.
    }

    @Override
    public void init() {
        if (!cellHandlerRegistered) {
            StorageCells.addCellHandler(CREATIVE_ESSENTIA_CELL_HANDLER);
            cellHandlerRegistered = true;
        }
    }

    @Override
    public void postInit() {

    }
}
