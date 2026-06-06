package thaumicenergistics.common.strategy;

import ae2.api.config.Actionable;
import ae2.api.behaviors.StackImportStrategy;
import ae2.api.behaviors.StackTransferContext;
import ae2.api.stacks.AEKey;
import ae2.api.storage.MEStorage;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.me.key.AEEssentiaKey;
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

        AspectList aspects = container.getAspects();
        if (aspects == null) {
            return false;
        }

        MEStorage storage = context.getInternalStorage().getInventory();
        for (Aspect aspect : aspects.getAspects()) {
            AEEssentiaKey key = AEEssentiaKey.of(aspect);
            if (key == null || !matchesFilter(context, key)) {
                continue;
            }

            int available = container.containerContains(aspect);
            if (available <= 0) {
                continue;
            }

            long maxTransfer = (long) Math.max(1, context.getOperationsRemaining()) * key.getAmountPerOperation();
            int requested = (int) Math.min(available, Math.min(Integer.MAX_VALUE, maxTransfer));
            long accepted = storage.insert(key, requested, Actionable.SIMULATE, context.getActionSource());
            if (accepted <= 0) {
                continue;
            }

            int toMove = (int) Math.min(requested, accepted);
            if (!container.takeFromContainer(aspect, toMove)) {
                continue;
            }

            long inserted = storage.insert(key, toMove, Actionable.MODULATE, context.getActionSource());
            if (inserted < toMove) {
                int remaining = toMove - (int) inserted;
                if (EssentiaContainerStrategyUtil.insert(container, aspect, remaining) < remaining) {
                    ThELog.warn("Essentia import strategy could not roll back all {} essentia after a partial ME insert at {}", aspect.getTag(), this.fromPos);
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
