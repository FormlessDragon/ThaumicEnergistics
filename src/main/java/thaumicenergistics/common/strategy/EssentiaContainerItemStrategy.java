package thaumicenergistics.common.strategy;

import ae2.api.behaviors.ContainerItemStrategy;
import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.GenericStack;
import ae2.api.storage.ITerminalHost;
import ae2.api.storage.MEStorage;
import ae2.container.AEBaseContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.items.consumables.ItemPhial;
import thaumicenergistics.api.stacks.EssentiaStack;
import thaumicenergistics.common.me.key.AEEssentiaKey;
import thaumicenergistics.common.me.key.AEEssentiaKeys;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.ThEUtil;

@SuppressWarnings("UnstableApiUsage")
public class EssentiaContainerItemStrategy implements ContainerItemStrategy<AEEssentiaKey, EssentiaContainerItemStrategy.Context> {

    public static void register() {
        ContainerItemStrategy.register(AEEssentiaKeys.INSTANCE, AEEssentiaKey.class, new EssentiaContainerItemStrategy());
    }

    @Override
    public @Nullable GenericStack getContainedStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }

        IEssentiaContainerItem container = getContainer(itemStack);
        if (container == null) {
            return null;
        }

        AspectList list = container.getAspects(itemStack);
        if (list == null || list.size() == 0) {
            return null;
        }

        Aspect aspect = list.getAspects()[0];
        if (aspect == null) {
            return null;
        }

        int amount = list.getAmount(aspect);
        if (amount <= 0) {
            return null;
        }

        AEEssentiaKey key = AEEssentiaKey.of(new EssentiaStack(aspect, amount));
        return key != null ? new GenericStack(key, amount) : null;
    }

    @Override
    public @Nullable Context findCarriedContext(EntityPlayer player, Container container) {
        ItemStack carried = player.inventory.getItemStack();
        if (isContainerItem(carried)) {
            return new CarriedContext(player);
        }
        if (carried.isEmpty()) {
            return findNetworkEmptyContainerContext(player, container);
        }
        return null;
    }

    @Override
    public @Nullable Context findPlayerSlotContext(EntityPlayer player, int slot) {
        if (isContainerItem(player.inventory.getStackInSlot(slot))) {
            return new PlayerInvContext(player, slot);
        }
        return null;
    }

    @Override
    public long extract(Context context, AEEssentiaKey what, long amount, Actionable mode) {
        if (amount <= 0) {
            return 0;
        }

        ItemStack stack = context.getStack();
        IEssentiaContainerItem container = getContainer(stack);
        if (container == null) {
            return 0;
        }

        ItemStack single = stack.copy();
        single.setCount(1);

        AspectList list = container.getAspects(single);
        if (list == null || list.size() == 0) {
            return 0;
        }

        Aspect aspect = what.getAspect();
        int available = list.getAmount(aspect);
        if (available <= 0) {
            return 0;
        }

        if (mode == Actionable.SIMULATE) {
            return available;
        }

        int extracted = (int) Math.min(amount, available);
        if (extracted <= 0) {
            return 0;
        }

        int remaining = available - extracted;
        if (remaining > 0) {
            container.setAspects(single, new AspectList().add(aspect, remaining));
        } else {
            clearAspects(single);
        }

        if (!context.canAddOverflow(single) || !context.consumeSourceContainer(Actionable.MODULATE)) {
            return 0;
        }

        context.addOverflow(single);
        return extracted;
    }

    @Override
    public long insert(Context context, AEEssentiaKey what, long amount, Actionable mode) {
        if (amount <= 0) {
            return 0;
        }

        ItemStack stack = context.getStack();
        IEssentiaContainerItem container = getContainer(stack);
        if (container == null) {
            return 0;
        }

        ItemStack copy = stack.copy();
        copy.setCount(1);

        int capacity = ThEUtil.getEssentiaCapacity(copy);
        if (capacity <= 0) {
            return 0;
        }

        Aspect aspect = what.getAspect();
        AspectList list = container.getAspects(copy);
        if (!containsOnly(list, aspect)) {
            return 0;
        }

        int current = list == null ? 0 : list.getAmount(aspect);
        int remainingSpace = capacity - current;
        if (remainingSpace <= 0) {
            return 0;
        }

        if (copy.getItem() instanceof ItemPhial) {
            copy.setItemDamage(1);
        }

        if (mode == Actionable.SIMULATE) {
            int simulated = getSimulatedInsertAmount(amount, remainingSpace);
            if (simulated <= 0) {
                return 0;
            }
            container.setAspects(copy, new AspectList().add(aspect, current + simulated));
            if (!context.canAddOverflow(copy)) {
                return 0;
            }
            return simulated;
        }

        int inserted = (int) Math.min(amount, remainingSpace);
        if (inserted <= 0) {
            return 0;
        }

        container.setAspects(copy, new AspectList().add(aspect, current + inserted));
        if (!context.canAddOverflow(copy) || !context.consumeSourceContainer(Actionable.MODULATE)) {
            return 0;
        }

        context.addOverflow(copy);
        return inserted;
    }

    @Override
    public void playFillSound(EntityPlayer entityPlayer, AEEssentiaKey aeEssentiaKey) {
        // No-op: Thaumcraft containers do not define a default fill sound here.
    }

    @Override
    public void playEmptySound(EntityPlayer entityPlayer, AEEssentiaKey aeEssentiaKey) {
        // No-op: Thaumcraft containers do not define a default empty sound here.
    }

    @Override
    public @Nullable GenericStack getExtractableContent(Context context) {
        return getContainedStack(context.getStack());
    }

    private static boolean isContainerItem(ItemStack stack) {
        return getContainer(stack) != null && ThEUtil.getEssentiaCapacity(stack) > 0;
    }

    private static void clearAspects(ItemStack stack) {
        stack.setTagCompound(null);
        stack.setItemDamage(0);
    }

    private static boolean containsOnly(@Nullable AspectList list, Aspect expected) {
        if (list == null || list.size() == 0) {
            return true;
        }

        for (Aspect aspect : list.getAspects()) {
            if (aspect != expected && list.getAmount(aspect) > 0) {
                return false;
            }
        }
        return true;
    }

    private static int getSimulatedInsertAmount(long amount, int remainingSpace) {
        if (amount == 1) {
            return remainingSpace;
        }
        return (int) Math.min(amount, remainingSpace);
    }

    static @Nullable Context findNetworkEmptyContainerContext(EntityPlayer player, MEStorage storage, IActionSource source) {
        return player == null ? null
                : findNetworkEmptyContainerContext(new PlayerCarriedStackAccessor(player), storage, source);
    }

    static @Nullable Context findNetworkEmptyContainerContext(CarriedStackAccessor carried, MEStorage storage, IActionSource source) {
        if (carried == null || storage == null || source == null || !carried.getCarried().isEmpty()) {
            return null;
        }
        if (ItemsTC.phial == null) {
            return null;
        }

        ItemStack emptyPhial = new ItemStack(ItemsTC.phial, 1, 0);
        if (!isContainerItem(emptyPhial)) {
            return null;
        }

        AEItemKey emptyKey = AEItemKey.of(emptyPhial);
        if (emptyKey == null || storage.extract(emptyKey, 1, Actionable.SIMULATE, source) < 1) {
            return null;
        }

        return new NetworkEmptyContainerContext(carried, storage, source, emptyPhial);
    }

    private static @Nullable Context findNetworkEmptyContainerContext(EntityPlayer player, Container container) {
        if (!(container instanceof AEBaseContainer aeContainer)) {
            return null;
        }

        Object target = aeContainer.getTarget();
        if (!(target instanceof ITerminalHost)) {
            return null;
        }

        return findNetworkEmptyContainerContext(player, ((ITerminalHost) target).getInventory(),
                aeContainer.getActionSource());
    }

    private static @Nullable IEssentiaContainerItem getContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (!(stack.getItem() instanceof IEssentiaContainerItem container)) {
            return null;
        }

        return container.ignoreContainedAspects() ? null : container;
    }

    public interface Context {

        ItemStack getStack();

        void setStack(ItemStack stack);

        boolean canAddOverflow(ItemStack stack);

        default boolean consumeSourceContainer(Actionable mode) {
            if (mode == Actionable.MODULATE) {
                this.getStack().shrink(1);
            }
            return true;
        }

        void addOverflow(ItemStack stack);

    }

    interface CarriedStackAccessor {
        ItemStack getCarried();

        void setCarried(ItemStack stack);

        void addOverflow(ItemStack stack);
    }

    private record CarriedContext(EntityPlayer player) implements Context {

        @Override
        public ItemStack getStack() {
            return player.inventory.getItemStack();
        }

        @Override
        public void setStack(ItemStack stack) {
            player.inventory.setItemStack(stack);
        }

        @Override
        public boolean canAddOverflow(ItemStack stack) {
            ItemStack carried = player.inventory.getItemStack();
            return carried.isEmpty() || carried.getCount() <= 1 || ForgeUtil.addStackToPlayerInventory(player, stack, true).isEmpty();
        }

        @Override
        public void addOverflow(ItemStack stack) {
            if (player.inventory.getItemStack().isEmpty()) {
                player.inventory.setItemStack(stack);
            } else {
                player.inventory.addItemStackToInventory(stack);
            }
        }

    }

    private record PlayerInvContext(EntityPlayer player, int slot) implements Context {

        @Override
        public ItemStack getStack() {
            return player.inventory.getStackInSlot(slot);
        }

        @Override
        public void setStack(ItemStack stack) {
            player.inventory.setInventorySlotContents(slot, stack);
        }

        @Override
        public boolean canAddOverflow(ItemStack stack) {
            ItemStack current = player.inventory.getStackInSlot(slot);
            return current.isEmpty() || current.getCount() <= 1 || ForgeUtil.addStackToPlayerInventory(player, stack, true).isEmpty();
        }

        @Override
        public void addOverflow(ItemStack stack) {
            player.inventory.addItemStackToInventory(stack);
        }

    }

    private record PlayerCarriedStackAccessor(EntityPlayer player) implements CarriedStackAccessor {

        @Override
        public ItemStack getCarried() {
            return this.player.inventory.getItemStack();
        }

        @Override
        public void setCarried(ItemStack stack) {
            this.player.inventory.setItemStack(stack);
        }

        @Override
        public void addOverflow(ItemStack stack) {
            if (this.player.inventory.getItemStack().isEmpty()) {
                this.player.inventory.setItemStack(stack);
            } else {
                this.player.inventory.addItemStackToInventory(stack);
            }
        }

    }

    private record NetworkEmptyContainerContext(CarriedStackAccessor carried, MEStorage storage, IActionSource source,
                                                ItemStack emptyContainer) implements Context {

        @Override
        public ItemStack getStack() {
            return this.emptyContainer.copy();
        }

        @Override
        public void setStack(ItemStack stack) {
        }

        @Override
        public boolean canAddOverflow(ItemStack stack) {
            AEItemKey emptyKey = AEItemKey.of(this.emptyContainer);
            return this.carried.getCarried().isEmpty()
                    && emptyKey != null
                    && this.storage.extract(emptyKey, 1, Actionable.SIMULATE, this.source) >= 1;
        }

        @Override
        public boolean consumeSourceContainer(Actionable mode) {
            AEItemKey emptyKey = AEItemKey.of(this.emptyContainer);
            if (emptyKey == null) {
                return false;
            }
            return this.storage.extract(emptyKey, 1, mode, this.source) >= 1;
        }

        @Override
        public void addOverflow(ItemStack stack) {
            this.carried.addOverflow(stack);
        }

    }

}
