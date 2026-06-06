package thaumicenergistics.common.strategy;

import ae2.api.config.Actionable;
import ae2.api.behaviors.ExternalStorageStrategy;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.WorldServer;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.integration.appeng.SupergiantEssentiaUtil;
import thaumicenergistics.me.key.AEEssentiaKey;
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
        return container == null ? null : new EssentiaExternalStorage(container, extractableOnly, callback);
    }

    protected IAspectContainer getContainer() {
        TileEntity tile = this.level.getTileEntity(this.fromPos);
        return tile instanceof IAspectContainer ? (IAspectContainer) tile : null;
    }

    private static final class EssentiaExternalStorage implements MEStorage {
        private final IAspectContainer container;
        private final boolean extractableOnly;
        private final Runnable callback;

        private EssentiaExternalStorage(IAspectContainer container, boolean extractableOnly, Runnable callback) {
            this.container = container;
            this.extractableOnly = extractableOnly;
            this.callback = callback;
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            AEEssentiaKey key = EssentiaContainerStrategyUtil.toEssentiaKey(what);
            if (this.extractableOnly || key == null || amount <= 0 || !EssentiaContainerStrategyUtil.canAttemptInsert(this.container, key.getAspect())) {
                return 0;
            }

            int requested = EssentiaContainerStrategyUtil.clampRequested(amount);
            if (mode == Actionable.SIMULATE) {
                return EssentiaContainerStrategyUtil.simulateInsert(this.container, key.getAspect(), requested);
            }

            int inserted = EssentiaContainerStrategyUtil.insert(this.container, key.getAspect(), requested);
            if (inserted > 0 && this.callback != null) {
                this.callback.run();
            }
            return inserted;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            AEEssentiaKey key = EssentiaContainerStrategyUtil.toEssentiaKey(what);
            if (key == null || amount <= 0) {
                return 0;
            }

            int available = this.container.containerContains(key.getAspect());
            if (available <= 0) {
                return 0;
            }

            int extracted = (int) Math.min(available, Math.min(Integer.MAX_VALUE, amount));
            if (mode == Actionable.MODULATE && !this.container.takeFromContainer(key.getAspect(), extracted)) {
                return 0;
            }
            if (mode == Actionable.MODULATE && extracted > 0 && this.callback != null) {
                this.callback.run();
            }
            return extracted;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            AspectList aspects = this.container.getAspects();
            if (aspects == null) {
                return;
            }

            for (Aspect aspect : aspects.getAspects()) {
                AEEssentiaKey key = AEEssentiaKey.of(aspect);
                int amount = this.container.containerContains(aspect);
                if (key != null && amount > 0) {
                    out.add(key, amount);
                }
            }
        }

        @Override
        public ITextComponent getDescription() {
            return SupergiantEssentiaUtil.description();
        }
    }

}
