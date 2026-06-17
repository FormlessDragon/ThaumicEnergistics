package thaumicenergistics.common.strategy;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.me.key.AEEssentiaKey;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EssentiaContainerStrategyUtilTest {

    @Test
    void simulatedInsertUsesActualContainerCapacity() {
        TestAspectContainer container = new TestAspectContainer(5, false, Integer.MAX_VALUE);
        container.addToContainer(Aspect.AIR, 4);

        int inserted = EssentiaContainerStrategyUtil.simulateInsert(container, Aspect.AIR, 3);

        assertEquals(1, inserted);
        assertEquals(4, container.containerContains(Aspect.AIR));
    }

    @Test
    void simulatedInsertCanAcceptAnotherAspectWhenContainerSupportsIt() {
        TestAspectContainer container = new TestAspectContainer(10, false, 1);
        container.addToContainer(Aspect.FIRE, 1);

        int inserted = EssentiaContainerStrategyUtil.simulateInsert(container, Aspect.AIR, 1);

        assertEquals(1, inserted);
        assertEquals(0, container.containerContains(Aspect.AIR));
        assertEquals(1, container.containerContains(Aspect.FIRE));
    }

    @Test
    void simulatedInsertCanProbeContainersThatOnlyAcceptOneEssentiaAtATime() {
        TestAspectContainer container = new TestAspectContainer(10, false, 1);

        int inserted = EssentiaContainerStrategyUtil.simulateInsert(container, Aspect.AIR, 4);

        assertEquals(4, inserted);
        assertEquals(0, container.containerContains(Aspect.AIR));
    }

    @Test
    void sharedStorageAdapterSimulatesModulatesExtractsAndCallsBackWhenMutated() {
        TestAspectContainer container = new TestAspectContainer(5, false, Integer.MAX_VALUE);
        CountingCallback callback = new CountingCallback();
        MEStorage storage = EssentiaContainerStrategyUtil.createStorage(container, false, callback);
        AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);

        assertEquals(3, storage.insert(air, 3, Actionable.SIMULATE, IActionSource.empty()));
        assertEquals(0, container.containerContains(Aspect.AIR));
        assertEquals(0, callback.calls);

        assertEquals(3, storage.insert(air, 3, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(3, container.containerContains(Aspect.AIR));
        assertEquals(1, callback.calls);

        KeyCounter available = storage.getAvailableStacks();
        assertEquals(3, available.get(air));

        assertEquals(2, storage.extract(air, 2, Actionable.SIMULATE, IActionSource.empty()));
        assertEquals(3, container.containerContains(Aspect.AIR));
        assertEquals(1, callback.calls);

        assertEquals(2, storage.extract(air, 2, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(1, container.containerContains(Aspect.AIR));
        assertEquals(2, callback.calls);
    }

    @Test
    void sharedStorageAdapterHonorsExtractableOnly() {
        TestAspectContainer container = new TestAspectContainer(5, false, Integer.MAX_VALUE);
        MEStorage storage = EssentiaContainerStrategyUtil.createStorage(container, true, null);

        assertEquals(0, storage.insert(AEEssentiaKey.of(Aspect.AIR), 1, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(0, container.containerContains(Aspect.AIR));
    }

    @Test
    void containerNullPointerExceptionIsNotSilentlySwallowed() {
        TestAspectContainer container = new ThrowingAspectContainer();

        assertThrows(NullPointerException.class, () -> EssentiaContainerStrategyUtil.insert(container, Aspect.AIR, 1));
    }

    @Test
    void simulatedInsertRejectsAnotherAspectWhenContainerIsSingleAspect() {
        TestAspectContainer container = new TestAspectContainer(10, true, Integer.MAX_VALUE);
        container.addToContainer(Aspect.FIRE, 1);

        int inserted = EssentiaContainerStrategyUtil.simulateInsert(container, Aspect.AIR, 1);

        assertEquals(0, inserted);
        assertEquals(0, container.containerContains(Aspect.AIR));
        assertEquals(1, container.containerContains(Aspect.FIRE));
    }

    private static class TestAspectContainer implements IAspectContainer {
        private final Map<Aspect, Integer> aspects = new LinkedHashMap<>();
        private final int capacity;
        private final boolean singleAspect;
        private final int maxAcceptedPerCall;

        private TestAspectContainer(int capacity, boolean singleAspect, int maxAcceptedPerCall) {
            this.capacity = capacity;
            this.singleAspect = singleAspect;
            this.maxAcceptedPerCall = maxAcceptedPerCall;
        }

        @Override
        public AspectList getAspects() {
            AspectList list = new AspectList();
            for (Map.Entry<Aspect, Integer> entry : this.aspects.entrySet()) {
                if (entry.getValue() > 0) {
                    list.add(entry.getKey(), entry.getValue());
                }
            }
            return list;
        }

        @Override
        public void setAspects(AspectList aspectList) {
            this.aspects.clear();
            if (aspectList != null) {
                for (Aspect aspect : aspectList.getAspects()) {
                    this.aspects.put(aspect, aspectList.getAmount(aspect));
                }
            }
        }

        @Override
        public boolean doesContainerAccept(Aspect aspect) {
            return aspect != null;
        }

        @Override
        public int addToContainer(Aspect aspect, int amount) {
            if (amount <= 0 || aspect == null || amount > this.maxAcceptedPerCall) {
                return amount;
            }
            if (this.singleAspect && this.hasDifferentAspect(aspect)) {
                return amount;
            }

            int inserted = Math.min(amount, Math.max(0, this.capacity - this.totalAmount()));
            if (inserted > 0) {
                this.aspects.merge(aspect, inserted, Integer::sum);
            }
            return amount - inserted;
        }

        @Override
        public boolean takeFromContainer(Aspect aspect, int amount) {
            if (amount <= 0 || this.containerContains(aspect) < amount) {
                return false;
            }

            int remaining = this.containerContains(aspect) - amount;
            if (remaining > 0) {
                this.aspects.put(aspect, remaining);
            } else {
                this.aspects.remove(aspect);
            }
            return true;
        }

        @Override
        public boolean takeFromContainer(AspectList aspectList) {
            return false;
        }

        @Override
        public boolean doesContainerContainAmount(Aspect aspect, int amount) {
            return this.containerContains(aspect) >= amount;
        }

        @Override
        public boolean doesContainerContain(AspectList aspectList) {
            if (aspectList == null) {
                return false;
            }
            for (Aspect aspect : aspectList.getAspects()) {
                if (!this.doesContainerContainAmount(aspect, aspectList.getAmount(aspect))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int containerContains(Aspect aspect) {
            return this.aspects.getOrDefault(aspect, 0);
        }

        private boolean hasDifferentAspect(Aspect aspect) {
            for (Map.Entry<Aspect, Integer> entry : this.aspects.entrySet()) {
                if (entry.getValue() > 0 && !entry.getKey().equals(aspect)) {
                    return true;
                }
            }
            return false;
        }

        private int totalAmount() {
            int total = 0;
            for (Integer amount : this.aspects.values()) {
                total += amount;
            }
            return total;
        }
    }

    private static final class ThrowingAspectContainer extends TestAspectContainer {
        private ThrowingAspectContainer() {
            super(1, false, Integer.MAX_VALUE);
        }

        @Override
        public int addToContainer(Aspect aspect, int amount) {
            throw new NullPointerException("test container failure");
        }
    }

    private static final class CountingCallback implements Runnable {
        private int calls;

        @Override
        public void run() {
            this.calls++;
        }
    }
}
