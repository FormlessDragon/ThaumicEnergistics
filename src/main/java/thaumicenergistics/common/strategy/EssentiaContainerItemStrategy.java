package thaumicenergistics.common.strategy;

import ae2.api.behaviors.ContainerItemStrategy;
import ae2.api.config.Actionable;
import ae2.api.stacks.GenericStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.items.consumables.ItemPhial;
import thaumicenergistics.api.stacks.EssentiaStack;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.me.key.AEEssentiaKeys;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.ThEUtil;

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
    public @Nullable Context findCarriedContext(EntityPlayer entityPlayer, Container container) {
        ItemStack carried = entityPlayer.inventory.getItemStack();
        return isContainerItem(carried) ? new CarriedContext(entityPlayer) : null;
    }

    @Override
    public @Nullable Context findPlayerSlotContext(EntityPlayer entityPlayer, int slot) {
        ItemStack stack = entityPlayer.inventory.getStackInSlot(slot);
        return isContainerItem(stack) ? new PlayerInvContext(entityPlayer, slot) : null;
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

        clearAspects(single);
        if (!context.canAddOverflow(single)) {
            return 0;
        }

        if (mode == Actionable.SIMULATE) {
            return available;
        }

        if (amount < available) {
            return 0;
        }

        stack.shrink(1);
        context.addOverflow(single);
        return available;
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

        AspectList list = container.getAspects(copy);
        if (list != null && list.size() > 0) {
            return 0;
        }

        if (copy.getItem() instanceof ItemPhial) {
            copy.setItemDamage(1);
        }

        container.setAspects(copy, new AspectList().add(what.getAspect(), capacity));
        if (!context.canAddOverflow(copy)) {
            return 0;
        }

        if (mode == Actionable.SIMULATE) {
            return capacity;
        }

        if (amount < capacity) {
            return 0;
        }

        stack.shrink(1);
        context.addOverflow(copy);
        return capacity;
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

    private static @Nullable IEssentiaContainerItem getContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (!(stack.getItem() instanceof IEssentiaContainerItem container)) {
            return null;
        }

        return container.ignoreContainedAspects() ? null : container;
    }

    interface Context {

        ItemStack getStack();

        void setStack(ItemStack stack);

        boolean canAddOverflow(ItemStack stack);

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

}
