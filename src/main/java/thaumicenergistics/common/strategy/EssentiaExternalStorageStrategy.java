package thaumicenergistics.common.strategy;

import ae2.api.behaviors.ExternalStorageStrategy;
import ae2.api.storage.MEStorage;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.me.key.AEEssentiaKeys;

public class EssentiaExternalStorageStrategy implements ExternalStorageStrategy {

    private final WorldServer level;
    private final BlockPos fromPos;

    public EssentiaExternalStorageStrategy(WorldServer level, BlockPos fromPos, EnumFacing fromSide) {
        this.level = level;
        this.fromPos = fromPos;
    }

    public static void register() {
        ExternalStorageStrategy.register(AEEssentiaKeys.INSTANCE, EssentiaExternalStorageStrategy::new);
    }

    @Override
    public @Nullable MEStorage createWrapper(boolean extractableOnly, Runnable callback) {
        IAspectContainer container = this.getContainer();
        return container == null ? null : EssentiaContainerStrategyUtil.createStorage(container, extractableOnly, callback);
    }

    protected IAspectContainer getContainer() {
        TileEntity tile = this.level.getTileEntity(this.fromPos);
        return tile instanceof IAspectContainer ? (IAspectContainer) tile : null;
    }

}
