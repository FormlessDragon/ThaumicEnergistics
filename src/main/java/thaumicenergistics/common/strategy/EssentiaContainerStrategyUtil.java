package thaumicenergistics.common.strategy;

import ae2.api.stacks.AEKey;
import thaumicenergistics.util.ThELog;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.me.key.AEEssentiaKey;

final class EssentiaContainerStrategyUtil {

    static final int LEGACY_JAR_ASPECT_CAPACITY_ESTIMATE = 250;

    private EssentiaContainerStrategyUtil() {
    }

    static AEEssentiaKey toEssentiaKey(AEKey key) {
        if (key instanceof AEEssentiaKey) {
            AEEssentiaKey essentiaKey = (AEEssentiaKey) key;
            return essentiaKey.getAspect() == null ? null : essentiaKey;
        }
        return null;
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
            int singleInsertLimit = Math.min(remaining, LEGACY_JAR_ASPECT_CAPACITY_ESTIMATE);
            for (int attempts = 0; attempts < singleInsertLimit; attempts++) {
                if (insertOnce(container, aspect, 1) != 1) {
                    break;
                }
                inserted++;
            }
        }

        if (simulate && inserted > 0 && !container.takeFromContainer(aspect, inserted)) {
            ThELog.warn("Could not roll back simulated essentia insert of {} {}.", inserted, aspect.getTag());
            return 0;
        }
        return inserted;
    }

    static boolean canAttemptInsert(IAspectContainer container, Aspect aspect) {
        return container != null
                && aspect != null
                && container.doesContainerAccept(aspect);
    }

    private static int insertOnce(IAspectContainer container, Aspect aspect, int requested) {
        try {
            int notAdded = container.addToContainer(aspect, requested);
            return Math.max(0, requested - Math.max(0, notAdded));
        } catch (NullPointerException ignored) {
            return 0;
        }
    }

}
