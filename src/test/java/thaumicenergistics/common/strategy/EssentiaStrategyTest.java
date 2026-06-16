package thaumicenergistics.common.strategy;

import ae2.api.behaviors.StackTransferContext;
import ae2.api.config.Actionable;
import ae2.api.networking.IGridNode;
import ae2.api.networking.energy.IEnergySource;
import ae2.api.networking.security.IActionSource;
import ae2.api.networking.storage.IStorageService;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.AEKeyType;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.IStorageProvider;
import ae2.api.storage.MEStorage;
import ae2.util.prioritylist.IPartitionList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.me.key.AEEssentiaKeys;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EssentiaStrategyTest {

    @Test
    void externalStorageSimulatesWithoutMutatingAndCallsBackOnlyWhenChanged() {
        TestAspectContainer container = new TestAspectContainer(10, false, 1);
        CountingCallback callback = new CountingCallback();
        MEStorage storage = new TestExternalStorageStrategy(container).createWrapper(false, callback);
        AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);

        assertNotNull(storage);
        assertEquals(4, storage.insert(air, 4, Actionable.SIMULATE, IActionSource.empty()));
        assertEquals(0, container.containerContains(Aspect.AIR));
        assertEquals(0, callback.calls);

        assertEquals(4, storage.insert(air, 4, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(4, container.containerContains(Aspect.AIR));
        assertEquals(1, callback.calls);

        assertEquals(2, storage.extract(air, 2, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(2, container.containerContains(Aspect.AIR));
        assertEquals(2, callback.calls);
    }

    @Test
    void externalStorageHonorsExtractableOnly() {
        TestAspectContainer container = new TestAspectContainer(10, false, Integer.MAX_VALUE);
        MEStorage storage = new TestExternalStorageStrategy(container).createWrapper(true, null);
        AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);

        assertNotNull(storage);
        assertEquals(0, storage.insert(air, 4, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(0, container.containerContains(Aspect.AIR));
    }

    @Test
    void exportPushSupportsContainersThatOnlyAcceptOneEssentiaAtATime() {
        TestAspectContainer container = new TestAspectContainer(10, false, 1);
        EssentiaStackExportStrategy strategy = new TestExportStrategy(container);
        AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);

        assertEquals(4, strategy.push(air, 4, Actionable.SIMULATE));
        assertEquals(0, container.containerContains(Aspect.AIR));

        assertEquals(4, strategy.push(air, 4, Actionable.MODULATE));
        assertEquals(4, container.containerContains(Aspect.AIR));
    }

    @Test
    void exportTransferRollsBackNetworkRemainderAfterPartialExternalInsert() {
        TestAspectContainer container = new TestAspectContainer(2, false, Integer.MAX_VALUE);
        EssentiaStackExportStrategy strategy = new TestExportStrategy(container);
        AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);
        CountingStorage network = new CountingStorage();
        network.add(air, 5);
        TestTransferContext context = new TestTransferContext(network, 5);

        assertEquals(2, strategy.transfer(context, air, 5));
        assertEquals(2, container.containerContains(Aspect.AIR));
        assertEquals(3, network.amount(air));
    }

    @Test
    void importTransferRespectsFilterAndOperationLimit() {
        TestAspectContainer container = new TestAspectContainer(10, false, Integer.MAX_VALUE);
        container.addToContainer(Aspect.AIR, 4);
        container.addToContainer(Aspect.FIRE, 4);
        CountingStorage network = new CountingStorage();
        AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);
        AEEssentiaKey fire = AEEssentiaKey.of(Aspect.FIRE);
        TestTransferContext context = new TestTransferContext(network, 2);
        context.filter(air);

        assertTrue(new TestImportStrategy(container).transfer(context));
        assertEquals(2, network.amount(air));
        assertEquals(0, network.amount(fire));
        assertEquals(2, container.containerContains(Aspect.AIR));
        assertEquals(4, container.containerContains(Aspect.FIRE));
        assertFalse(context.hasOperationsLeft());
    }

    @Test
    void importTransferRollsBackRemainderWhenNetworkPartiallyAccepts() {
        TestAspectContainer container = new TestAspectContainer(10, false, Integer.MAX_VALUE);
        container.addToContainer(Aspect.AIR, 4);
        AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);
        CountingStorage network = new CountingStorage();
        network.modulateInsertLimit = 2;
        TestTransferContext context = new TestTransferContext(network, 4);

        assertTrue(new TestImportStrategy(container).transfer(context));
        assertEquals(2, network.amount(air));
        assertEquals(2, container.containerContains(Aspect.AIR));
        assertEquals(2, context.getOperationsRemaining());
    }

    private static final class TestExternalStorageStrategy extends EssentiaExternalStorageStrategy {
        private final IAspectContainer container;

        private TestExternalStorageStrategy(IAspectContainer container) {
            super(null, null, null);
            this.container = container;
        }

        @Override
        protected IAspectContainer getContainer() {
            return this.container;
        }
    }

    private static final class TestExportStrategy extends EssentiaStackExportStrategy {
        private final IAspectContainer container;

        private TestExportStrategy(IAspectContainer container) {
            super(null, null, null);
            this.container = container;
        }

        @Override
        protected IAspectContainer getContainer() {
            return this.container;
        }
    }

    private static final class TestImportStrategy extends EssentiaStackImportStrategy {
        private final IAspectContainer container;

        private TestImportStrategy(IAspectContainer container) {
            super(null, null, null);
            this.container = container;
        }

        @Override
        protected IAspectContainer getContainer() {
            return this.container;
        }
    }

    private static final class TestAspectContainer implements IAspectContainer {
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

    private static final class CountingStorage implements MEStorage {
        private final Map<AEKey, Long> amounts = new LinkedHashMap<>();
        private long modulateInsertLimit = Long.MAX_VALUE;

        void add(AEKey key, long amount) {
            this.amounts.merge(key, amount, Long::sum);
        }

        long amount(AEKey key) {
            return this.amounts.getOrDefault(key, 0L);
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            long inserted = Math.min(amount, this.modulateInsertLimit);
            if (mode == Actionable.MODULATE && inserted > 0) {
                this.add(what, inserted);
            }
            return inserted;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            long extracted = Math.min(amount, this.amount(what));
            if (mode == Actionable.MODULATE && extracted > 0) {
                long remaining = this.amount(what) - extracted;
                if (remaining > 0) {
                    this.amounts.put(what, remaining);
                } else {
                    this.amounts.remove(what);
                }
            }
            return extracted;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            for (Map.Entry<AEKey, Long> entry : this.amounts.entrySet()) {
                out.add(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public ITextComponent getDescription() {
            return new TextComponentString("test");
        }
    }

    private static final class TestTransferContext implements StackTransferContext {
        private final CountingStorage storage;
        private final KeyCounter filter = new KeyCounter();
        private final int initialOperations;
        private int operationsRemaining;
        private boolean inverted;

        private TestTransferContext(CountingStorage storage, int operationsRemaining) {
            this.storage = storage;
            this.operationsRemaining = operationsRemaining;
            this.initialOperations = operationsRemaining;
        }

        void filter(AEKey key) {
            this.filter.add(key, 1);
        }

        @Override
        public IStorageService getInternalStorage() {
            return new TestStorageService(this.storage);
        }

        @Override
        public IEnergySource getEnergySource() {
            return (amt, mode, multiplier) -> amt;
        }

        @Override
        public IActionSource getActionSource() {
            return IActionSource.empty();
        }

        @Override
        public int getOperationsRemaining() {
            return this.operationsRemaining;
        }

        @Override
        public void setOperationsRemaining(int operationsRemaining) {
            this.operationsRemaining = operationsRemaining;
        }

        @Override
        public boolean hasOperationsLeft() {
            return this.operationsRemaining > 0;
        }

        @Override
        public boolean hasDoneWork() {
            return this.operationsRemaining < this.initialOperations;
        }

        @Override
        public boolean isKeyTypeEnabled(AEKeyType space) {
            return space == AEEssentiaKeys.INSTANCE;
        }

        @Override
        public boolean isInFilter(AEKey key) {
            return this.filter.isEmpty() || this.filter.get(key) > 0;
        }

        @Override
        public IPartitionList getFilter() {
            return IPartitionList.builder().build();
        }

        @Override
        public boolean isInverted() {
            return this.inverted;
        }

        @Override
        public void setInverted(boolean inverted) {
            this.inverted = inverted;
        }

        @Override
        public boolean canInsert(AEItemKey what, long amount) {
            return this.storage.insert(what, amount, Actionable.SIMULATE, this.getActionSource()) > 0;
        }

        @Override
        public void reduceOperationsRemaining(long inserted) {
            this.operationsRemaining -= (int) inserted;
        }
    }

    private record TestStorageService(MEStorage inventory) implements IStorageService {
        @Override
        public MEStorage getInventory() {
            return this.inventory;
        }

        @Override
        public KeyCounter getCachedInventory() {
            return this.inventory.getAvailableStacks();
        }

        @Override
        public void addGlobalStorageProvider(IStorageProvider provider) {
        }

        @Override
        public void removeGlobalStorageProvider(IStorageProvider provider) {
        }

        @Override
        public void refreshNodeStorageProvider(IGridNode node) {
        }

        @Override
        public void refreshGlobalStorageProvider(IStorageProvider provider) {
        }

        @Override
        public void invalidateCache() {
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
