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
import thaumicenergistics.api.stacks.EssentiaStack;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.me.key.AEEssentiaKeys;
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
    public long extract(Context context, AEEssentiaKey aeEssentiaKey, long l, Actionable actionable) {
        ItemStack stack = context.getStack();
        IEssentiaContainerItem container = getContainer(stack);
        if (container == null) {
            return 0;
        }

        AspectList list = container.getAspects(stack);
        if (list == null || list.size() == 0) {
            return 0;
        }

        Aspect aspect = aeEssentiaKey.getAspect();
        int available = list.getAmount(aspect);
        if (available <= 0) {
            return 0;
        }

        int toExtract = (int) Math.min(Integer.MAX_VALUE, l);
        int extracted = Math.min(toExtract, available);
        if (extracted > 0 && actionable == Actionable.MODULATE) {
            int remaining = available - extracted;
            AspectList updated = remaining > 0 ? new AspectList().add(aspect, remaining) : new AspectList();
            container.setAspects(stack, updated);
            context.setStack(stack);
        }
        return extracted;
    }

    @Override
    public long insert(Context context, AEEssentiaKey aeEssentiaKey, long l, Actionable actionable) {
        ItemStack stack = context.getStack();
        IEssentiaContainerItem container = getContainer(stack);
        if (container == null) {
            return 0;
        }

        int capacity = ThEUtil.getEssentiaCapacity(stack);
        if (capacity <= 0) {
            return 0;
        }

        Aspect aspect = aeEssentiaKey.getAspect();
        AspectList list = container.getAspects(stack);
        int current = 0;
        if (list != null && list.size() > 0) {
            Aspect existing = list.getAspects()[0];
            if (existing == null || !existing.equals(aspect)) {
                return 0;
            }
            current = list.getAmount(existing);
        }

        int toInsert = (int) Math.min(Integer.MAX_VALUE, l);
        int room = Math.max(0, capacity - current);
        int inserted = Math.min(toInsert, room);
        if (inserted > 0 && actionable == Actionable.MODULATE) {
            AspectList updated = new AspectList().add(aspect, current + inserted);
            container.setAspects(stack, updated);
            context.setStack(stack);
        }
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

    private static @Nullable IEssentiaContainerItem getContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (!(stack.getItem() instanceof IEssentiaContainerItem)) {
            return null;
        }

        IEssentiaContainerItem container = (IEssentiaContainerItem) stack.getItem();
        return container.ignoreContainedAspects() ? null : container;
    }

    interface Context {

        ItemStack getStack();

        void setStack(ItemStack stack);

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
        public void addOverflow(ItemStack stack) {
            // No overflow handling needed for carried stacks.
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
        public void addOverflow(ItemStack stack) {
            // No overflow handling needed for player inventory slots.
        }

    }

}
