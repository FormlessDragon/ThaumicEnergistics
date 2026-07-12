package thaumicenergistics.common.strategy;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import net.minecraft.util.text.ITextComponent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.tiles.essentia.TileJarFillable;
import thaumicenergistics.core.ThELog;
import thaumicenergistics.common.me.key.AEEssentiaKey;
import thaumicenergistics.common.me.key.AEEssentiaKeys;

import java.util.Objects;

final class EssentiaContainerStrategyUtil {

    static final int THAUMCRAFT_JAR_ASPECT_CAPACITY = TileJarFillable.CAPACITY;
    static final int GENERIC_SINGLE_INSERT_PROBE_LIMIT = 250;

    private EssentiaContainerStrategyUtil() {
    }

    static AEEssentiaKey toEssentiaKey(AEKey key) {
        if (!(key instanceof AEEssentiaKey essentiaKey)) {
            return null;
        }

        return essentiaKey.getAspect() == null ? null : essentiaKey;
    }

    static int clampRequested(long amount) {
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }

    static int simulateInsert(IAspectContainer container, Aspect aspect, int requested) {
        return insert(container, aspect, requested, true);
    }

    static int insert(IAspectContainer container, Aspect aspect, int requested) {
        return insert(container, aspect, requested, false);
    }

    static MEStorage createStorage(IAspectContainer container, boolean extractableOnly, Runnable mutationCallback) {
        return new EssentiaContainerStorage(Objects.requireNonNull(container, "container"), extractableOnly, mutationCallback);
    }

    private static int insert(IAspectContainer container, Aspect aspect, int requested, boolean simulate) {
        if (requested <= 0 || !canAttemptInsert(container, aspect)) {
            return 0;
        }

        int inserted = insertOnce(container, aspect, requested);
        int remaining = requested - inserted;
        if (remaining > 0 && requested > 1 && (inserted > 0 || insertOnce(container, aspect, 1) == 1)) {
            if (inserted == 0) {
                inserted++;
                remaining--;
            }
            int singleInsertLimit = Math.min(remaining, getSingleInsertProbeBudget(container, inserted));
            for (int attempts = 0; attempts < singleInsertLimit; attempts++) {
                if (insertOnce(container, aspect, 1) != 1) {
                    break;
                }
                inserted++;
            }
        }

        if (simulate && inserted > 0 && !container.takeFromContainer(aspect, inserted)) {
            ThELog.warn(
                    "Could not roll back simulated essentia insert: aspect={}, tag={}, inserted={}, requested={}.",
                    aspect,
                    getAspectTag(aspect),
                    inserted,
                    requested);
            return 0;
        }
        return inserted;
    }

    private static int getSingleInsertProbeBudget(IAspectContainer container, int alreadyInserted) {
        int boundary = container instanceof TileJarFillable
                ? THAUMCRAFT_JAR_ASPECT_CAPACITY
                : alreadyInserted + GENERIC_SINGLE_INSERT_PROBE_LIMIT;
        return Math.max(0, boundary - alreadyInserted);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean canAttemptInsert(IAspectContainer container, Aspect aspect) {
        return container != null
                && aspect != null
                && container.doesContainerAccept(aspect);
    }

    private static int insertOnce(IAspectContainer container, Aspect aspect, int requested) {
        try {
            int notAdded = container.addToContainer(aspect, requested);
            return Math.max(0, requested - Math.max(0, notAdded));
        } catch (NullPointerException e) {
            ThELog.warn(
                    "Thaumcraft essentia container threw while inserting essentia: aspect={}, tag={}, requested={}.",
                    aspect,
                    getAspectTag(aspect),
                    requested,
                    e);
            throw e;
        }
    }

    private static String getAspectTag(Aspect aspect) {
        return aspect == null ? "<null>" : aspect.getTag();
    }

    private record EssentiaContainerStorage(IAspectContainer container, boolean extractableOnly,
                                            Runnable mutationCallback) implements MEStorage {

        @Override
            public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
                Objects.requireNonNull(mode, "mode");
                Objects.requireNonNull(source, "source");

                AEEssentiaKey key = toEssentiaKey(what);
                if (this.extractableOnly || key == null || amount <= 0 || !canAttemptInsert(this.container, key.getAspect())) {
                    return 0;
                }

                int requested = clampRequested(amount);
                int inserted = mode == Actionable.SIMULATE
                    ? simulateInsert(this.container, key.getAspect(), requested)
                    : EssentiaContainerStrategyUtil.insert(this.container, key.getAspect(), requested);
                if (mode == Actionable.MODULATE && inserted > 0) {
                    this.notifyMutation();
                }
                return inserted;
            }

            @Override
            public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
                Objects.requireNonNull(mode, "mode");
                Objects.requireNonNull(source, "source");

                AEEssentiaKey key = toEssentiaKey(what);
                if (key == null || amount <= 0) {
                    return 0;
                }

                Aspect aspect = key.getAspect();
                int available = this.container.containerContains(aspect);
                if (available <= 0) {
                    return 0;
                }

                int extracted = (int) Math.min(available, Math.min(Integer.MAX_VALUE, amount));
                if (mode == Actionable.MODULATE) {
                    if (!this.container.takeFromContainer(aspect, extracted)) {
                        return 0;
                    }
                    this.notifyMutation();
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
                return AEEssentiaKeys.INSTANCE.getDescription();
            }

            private void notifyMutation() {
                if (this.mutationCallback != null) {
                    this.mutationCallback.run();
                }
            }
        }

}
