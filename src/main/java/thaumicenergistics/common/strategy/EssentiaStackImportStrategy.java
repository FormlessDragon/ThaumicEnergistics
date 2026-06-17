package thaumicenergistics.common.strategy;

import ae2.api.config.Actionable;
import ae2.api.behaviors.StackImportStrategy;
import ae2.api.behaviors.StackTransferContext;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.me.key.AEEssentiaKeys;
import thaumicenergistics.util.ThELog;

public class EssentiaStackImportStrategy implements StackImportStrategy {

    private final WorldServer level;
    private final BlockPos fromPos;

    public EssentiaStackImportStrategy(WorldServer level, BlockPos fromPos, EnumFacing fromSide) {
        this.level = level;
        this.fromPos = fromPos;
    }

    public static void register() {
        StackImportStrategy.register(AEEssentiaKeys.INSTANCE, EssentiaStackImportStrategy::new);
    }

    @Override
    public boolean transfer(StackTransferContext context) {
        if (!context.hasOperationsLeft() || !context.isKeyTypeEnabled(AEEssentiaKeys.INSTANCE)) {
            return false;
        }

        IAspectContainer container = this.getContainer();
        if (container == null) {
            return false;
        }

        MEStorage containerStorage = EssentiaContainerStrategyUtil.createStorage(container, false, null);
        KeyCounter availableStacks = containerStorage.getAvailableStacks();
        MEStorage networkStorage = context.getInternalStorage().getInventory();
        for (Object2LongMap.Entry<AEKey> entry : availableStacks) {
            AEKey key = entry.getKey();
            if (!matchesFilter(context, key)) {
                continue;
            }

            long available = entry.getLongValue();
            if (available <= 0) {
                continue;
            }

            long maxTransfer = (long) Math.max(1, context.getOperationsRemaining()) * key.getAmountPerOperation();
            long requested = Math.min(available, maxTransfer);
            long accepted = networkStorage.insert(key, requested, Actionable.SIMULATE, context.getActionSource());
            if (accepted <= 0) {
                continue;
            }

            long toMove = Math.min(requested, accepted);
            long extracted = containerStorage.extract(key, toMove, Actionable.MODULATE, context.getActionSource());
            if (extracted <= 0) {
                continue;
            }

            long inserted = networkStorage.insert(key, extracted, Actionable.MODULATE, context.getActionSource());
            if (inserted < extracted) {
                long remaining = extracted - inserted;
                long returned = containerStorage.insert(key, remaining, Actionable.MODULATE, context.getActionSource());
                if (returned < remaining) {
                    ThELog.warn(
                            "Essentia import strategy could not roll back all {} essentia for {} after a partial ME insert at {}",
                            remaining,
                            key,
                            this.fromPos);
                }
            }

            if (inserted > 0) {
                context.reduceOperationsRemaining(Math.max(1, inserted / key.getAmountPerOperation()));
                return true;
            }
        }
        return false;
    }

    protected IAspectContainer getContainer() {
        TileEntity tile = this.level.getTileEntity(this.fromPos);
        return tile instanceof IAspectContainer ? (IAspectContainer) tile : null;
    }

    private static boolean matchesFilter(StackTransferContext context, AEKey key) {
        boolean listed = context.isInFilter(key);
        return context.isInverted() ? !listed : listed;
    }

}
