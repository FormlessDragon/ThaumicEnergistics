package thaumicenergistics.common.strategy;

import ae2.api.behaviors.StackExportStrategy;
import ae2.api.behaviors.StackTransferContext;
import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.storage.MEStorage;
import ae2.api.storage.StorageHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.common.me.key.AEEssentiaKey;
import thaumicenergistics.common.me.key.AEEssentiaKeys;
import thaumicenergistics.core.ThELog;

public class EssentiaStackExportStrategy implements StackExportStrategy {

    private final WorldServer level;
    private final BlockPos fromPos;

    public EssentiaStackExportStrategy(WorldServer level, BlockPos fromPos, EnumFacing fromSide) {
        this.level = level;
        this.fromPos = fromPos;
    }

    public static void register() {
        StackExportStrategy.register(AEEssentiaKeys.INSTANCE, EssentiaStackExportStrategy::new);
    }

    @Override
    public long transfer(StackTransferContext context, AEKey key, long amount) {
        AEEssentiaKey essentiaKey = EssentiaContainerStrategyUtil.toEssentiaKey(key);
        if (essentiaKey == null || amount <= 0) {
            return 0;
        }

        long canPush = this.push(essentiaKey, amount, Actionable.SIMULATE);
        if (canPush <= 0) {
            return 0;
        }

        MEStorage storage = context.getInternalStorage().getInventory();
        long toExtract = Math.min(amount, canPush);
        long available = StorageHelper.poweredExtraction(context.getEnergySource(), storage, essentiaKey, toExtract, context.getActionSource(), Actionable.SIMULATE);
        if (available <= 0) {
            return 0;
        }

        long extracted = StorageHelper.poweredExtraction(context.getEnergySource(), storage, essentiaKey, Math.min(toExtract, available), context.getActionSource(), Actionable.MODULATE);
        if (extracted <= 0) {
            return 0;
        }

        long inserted = this.push(essentiaKey, extracted, Actionable.MODULATE);
        if (inserted < extracted) {
            long returned = storage.insert(essentiaKey, extracted - inserted, Actionable.MODULATE, context.getActionSource());
            if (returned < extracted - inserted) {
                ThELog.warn("Essentia export strategy could not return all {} essentia after a partial external insert at {}", essentiaKey.getAspectTag(), this.fromPos);
            }
        }
        return inserted;
    }

    @Override
    public long push(AEKey key, long amount, Actionable mode) {
        AEEssentiaKey essentiaKey = EssentiaContainerStrategyUtil.toEssentiaKey(key);
        if (essentiaKey == null || amount <= 0) {
            return 0;
        }

        IAspectContainer container = this.getContainer();
        if (container == null) {
            return 0;
        }

        MEStorage containerStorage = EssentiaContainerStrategyUtil.createStorage(container, false, null);
        return containerStorage.insert(essentiaKey, amount, mode, IActionSource.empty());
    }

    protected IAspectContainer getContainer() {
        TileEntity tile = this.level.getTileEntity(this.fromPos);
        return tile instanceof IAspectContainer ? (IAspectContainer) tile : null;
    }

}
