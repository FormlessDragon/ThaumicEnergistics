package thaumicenergistics.integration.appeng;

import ae2.api.storage.StorageCells;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.integration.IThEIntegration;
import thaumicenergistics.integration.appeng.cell.CreativeEssentiaCellHandler;
import thaumicenergistics.integration.appeng.compat.Upgrades;

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

        ThEApi.instance().items().essentiaImportBus().maybeStack(1).ifPresent(stack -> {
            Upgrades.REDSTONE.registerItem(stack, 1);
            Upgrades.CAPACITY.registerItem(stack, 2);
            Upgrades.SPEED.registerItem(stack, 4);
        });
        ThEApi.instance().items().essentiaExportBus().maybeStack(1).ifPresent(stack -> {
            Upgrades.REDSTONE.registerItem(stack, 1);
            Upgrades.CAPACITY.registerItem(stack, 2);
            Upgrades.SPEED.registerItem(stack, 4);
        });
        ThEApi.instance().items().essentiaStorageBus().maybeStack(1).ifPresent(stack -> {
            Upgrades.INVERTER.registerItem(stack, 1);
            Upgrades.CAPACITY.registerItem(stack, 5);
        });
    }

    @Override
    public void postInit() {

    }
}
