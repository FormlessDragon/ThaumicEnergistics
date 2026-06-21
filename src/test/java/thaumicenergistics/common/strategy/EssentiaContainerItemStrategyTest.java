package thaumicenergistics.common.strategy;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.items.ItemsTC;
import thaumicenergistics.config.ThEConfig;
import thaumicenergistics.me.key.AEEssentiaKey;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EssentiaContainerItemStrategyTest {

    @Test
    void emptyCarriedStackCanUseNetworkPhialWhenFillIsModulated() {
        bootstrapMinecraft();

        Item previousPhial = ItemsTC.phial;
        Map<String, Integer> previousCapacities = installEssentiaCapacityConfig();
        TestEssentiaContainerItem phial = new TestEssentiaContainerItem("thaumcraft", "phial");
        ItemsTC.phial = phial;
        try {
            TestCarriedStack carried = new TestCarriedStack();
            CountingStorage storage = new CountingStorage();
            ItemStack emptyPhial = new ItemStack(phial);
            storage.add(AEItemKey.of(emptyPhial), 1);

            assertTrue(carried.getCarried().isEmpty());
            EssentiaContainerItemStrategy.Context context = EssentiaContainerItemStrategy.findNetworkEmptyContainerContext(
                    carried,
                    storage,
                    IActionSource.empty());

            EssentiaContainerItemStrategy strategy = new EssentiaContainerItemStrategy();
            AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);

            assertEquals(10, strategy.insert(context, air, 10, Actionable.SIMULATE));
            assertTrue(carried.getCarried().isEmpty());
            assertEquals(1, storage.amount(AEItemKey.of(emptyPhial)));

            assertEquals(10, strategy.insert(context, air, 10, Actionable.MODULATE));

            assertFalse(carried.getCarried().isEmpty());
            assertEquals(phial, carried.getCarried().getItem());
            assertEquals(0, storage.amount(AEItemKey.of(emptyPhial)));

            AspectList filled = phial.getAspects(carried.getCarried());
            assertEquals(10, filled.getAmount(Aspect.AIR));
        } finally {
            ItemsTC.phial = previousPhial;
            restoreEssentiaCapacityConfig(previousCapacities);
        }
    }

    @Test
    void insertAllowsPartialFillWhenSourceProvidesLessThanCapacity() {
        bootstrapMinecraft();

        Map<String, Integer> previousCapacities = installEssentiaCapacityConfig();
        try {
            TestEssentiaContainerItem phial = new TestEssentiaContainerItem("thaumcraft", "phial");
            TestStackContext context = new TestStackContext(new ItemStack(phial));
            EssentiaContainerItemStrategy strategy = new EssentiaContainerItemStrategy();
            AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);

            assertEquals(10, strategy.insert(context, air, 1, Actionable.SIMULATE));
            assertEquals(7, strategy.insert(context, air, 7, Actionable.MODULATE));

            assertEquals(7, phial.getAspects(context.getStack()).getAmount(Aspect.AIR));
        } finally {
            restoreEssentiaCapacityConfig(previousCapacities);
        }
    }

    @Test
    void insertModulateDoesNotPromoteSingleEssentiaToFullContainer() {
        bootstrapMinecraft();

        Map<String, Integer> previousCapacities = installEssentiaCapacityConfig();
        try {
            TestEssentiaContainerItem phial = new TestEssentiaContainerItem("thaumcraft", "phial");
            TestStackContext context = new TestStackContext(new ItemStack(phial));
            EssentiaContainerItemStrategy strategy = new EssentiaContainerItemStrategy();
            AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);

            assertEquals(1, strategy.insert(context, air, 1, Actionable.MODULATE));

            assertEquals(1, phial.getAspects(context.getStack()).getAmount(Aspect.AIR));
        } finally {
            restoreEssentiaCapacityConfig(previousCapacities);
        }
    }

    @Test
    void insertClampsToRemainingSpaceInPartiallyFilledContainer() {
        bootstrapMinecraft();

        Map<String, Integer> previousCapacities = installEssentiaCapacityConfig();
        try {
            TestEssentiaContainerItem phial = new TestEssentiaContainerItem("thaumcraft", "phial");
            ItemStack partialPhial = new ItemStack(phial);
            phial.setAspects(partialPhial, new AspectList().add(Aspect.AIR, 6));

            TestStackContext context = new TestStackContext(partialPhial);
            EssentiaContainerItemStrategy strategy = new EssentiaContainerItemStrategy();
            AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);

            assertEquals(4, strategy.insert(context, air, 1, Actionable.SIMULATE));
            assertEquals(4, strategy.insert(context, air, 8, Actionable.MODULATE));

            assertEquals(10, phial.getAspects(context.getStack()).getAmount(Aspect.AIR));
        } finally {
            restoreEssentiaCapacityConfig(previousCapacities);
        }
    }

    @Test
    void insertSimulationUsesKnownPartialAmountWhenFullContainerCannotFit() {
        bootstrapMinecraft();

        Map<String, Integer> previousCapacities = installEssentiaCapacityConfig();
        try {
            TestEssentiaContainerItem phial = new TestEssentiaContainerItem("thaumcraft", "phial");
            OnlyAmountContext context = new OnlyAmountContext(new ItemStack(phial), 7);
            EssentiaContainerItemStrategy strategy = new EssentiaContainerItemStrategy();
            AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);

            assertEquals(7, strategy.insert(context, air, 7, Actionable.SIMULATE));
            assertEquals(7, strategy.insert(context, air, 7, Actionable.MODULATE));

            assertEquals(7, phial.getAspects(context.getStack()).getAmount(Aspect.AIR));
        } finally {
            restoreEssentiaCapacityConfig(previousCapacities);
        }
    }

    @Test
    void extractAllowsPartialEmptyWhenNetworkCanOnlyAcceptPart() {
        bootstrapMinecraft();

        Map<String, Integer> previousCapacities = installEssentiaCapacityConfig();
        try {
            TestEssentiaContainerItem phial = new TestEssentiaContainerItem("thaumcraft", "phial");
            ItemStack partialPhial = new ItemStack(phial);
            phial.setAspects(partialPhial, new AspectList().add(Aspect.AIR, 7));

            TestStackContext context = new TestStackContext(partialPhial);
            EssentiaContainerItemStrategy strategy = new EssentiaContainerItemStrategy();
            AEEssentiaKey air = AEEssentiaKey.of(Aspect.AIR);

            assertEquals(7, strategy.extract(context, air, 1, Actionable.SIMULATE));
            assertEquals(3, strategy.extract(context, air, 3, Actionable.MODULATE));

            assertEquals(4, phial.getAspects(context.getStack()).getAmount(Aspect.AIR));
        } finally {
            restoreEssentiaCapacityConfig(previousCapacities);
        }
    }

    private static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    private static Map<String, Integer> installEssentiaCapacityConfig() {
        Map<String, Integer> previousCapacities = new HashMap<>(ThEConfig.essentiaContainerCapacity);
        ThEConfig.essentiaContainerCapacity.clear();
        ThEConfig.essentiaContainerCapacity.put("thaumcraft:phial", 10);
        return previousCapacities;
    }

    private static void restoreEssentiaCapacityConfig(Map<String, Integer> previousCapacities) {
        ThEConfig.essentiaContainerCapacity.clear();
        ThEConfig.essentiaContainerCapacity.putAll(previousCapacities);
    }

    private static final class TestEssentiaContainerItem extends Item implements IEssentiaContainerItem {
        private TestEssentiaContainerItem(String namespace, String path) {
            this.setRegistryName(new ResourceLocation(namespace, path));
        }

        @Override
        public AspectList getAspects(ItemStack itemStack) {
            NBTTagCompound tag = itemStack.getTagCompound();
            if (tag == null || !tag.hasKey("Aspect")) {
                return null;
            }
            Aspect aspect = Aspect.getAspect(tag.getString("Aspect"));
            return aspect == null ? null : new AspectList().add(aspect, tag.getInteger("Amount"));
        }

        @Override
        public void setAspects(ItemStack itemStack, AspectList aspectList) {
            if (aspectList == null || aspectList.size() == 0) {
                itemStack.setTagCompound(null);
                return;
            }
            Aspect aspect = aspectList.getAspects()[0];
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Aspect", aspect.getTag());
            tag.setInteger("Amount", aspectList.getAmount(aspect));
            itemStack.setTagCompound(tag);
        }

        @Override
        public boolean ignoreContainedAspects() {
            return false;
        }
    }

    private static final class TestCarriedStack implements EssentiaContainerItemStrategy.CarriedStackAccessor {
        private ItemStack carried = ItemStack.EMPTY;

        @Override
        public ItemStack getCarried() {
            return this.carried;
        }

        @Override
        public void setCarried(ItemStack stack) {
            this.carried = stack;
        }

        @Override
        public void addOverflow(ItemStack stack) {
            this.carried = stack;
        }
    }

    private static final class TestStackContext implements EssentiaContainerItemStrategy.Context {
        private ItemStack stack;

        private TestStackContext(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public ItemStack getStack() {
            return this.stack;
        }

        @Override
        public void setStack(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public boolean canAddOverflow(ItemStack stack) {
            return true;
        }

        @Override
        public void addOverflow(ItemStack stack) {
            assertTrue(this.stack.isEmpty());
            assertNotNull(stack);
            this.stack = stack;
        }
    }

    private static final class OnlyAmountContext implements EssentiaContainerItemStrategy.Context {
        private final int acceptedAmount;
        private ItemStack stack;

        private OnlyAmountContext(ItemStack stack, int acceptedAmount) {
            this.stack = stack;
            this.acceptedAmount = acceptedAmount;
        }

        @Override
        public ItemStack getStack() {
            return this.stack;
        }

        @Override
        public void setStack(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public boolean canAddOverflow(ItemStack stack) {
            IEssentiaContainerItem container = (IEssentiaContainerItem) stack.getItem();
            AspectList aspects = container.getAspects(stack);
            return aspects != null && aspects.getAmount(Aspect.AIR) == this.acceptedAmount;
        }

        @Override
        public void addOverflow(ItemStack stack) {
            this.stack = stack;
        }
    }

    private static final class CountingStorage implements MEStorage {
        private final Map<AEKey, Long> amounts = new LinkedHashMap<>();

        void add(AEKey key, long amount) {
            this.amounts.merge(key, amount, Long::sum);
        }

        long amount(AEKey key) {
            return this.amounts.getOrDefault(key, 0L);
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (mode == Actionable.MODULATE && amount > 0) {
                this.add(what, amount);
            }
            return amount;
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
}
