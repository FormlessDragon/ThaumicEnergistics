package thaumicenergistics.container.part;

import ae2.api.config.Actionable;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import ae2.api.util.IConfigurableObject;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.items.consumables.ItemPhial;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.api.stacks.AEEssentiaKey;
import thaumicenergistics.config.AESettings;
import thaumicenergistics.container.ActionType;
import thaumicenergistics.container.ContainerBaseTerminal;
import thaumicenergistics.integration.appeng.SupergiantEssentiaUtil;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketInvHeldUpdate;
import thaumicenergistics.network.packets.PacketMEEssentiaUpdate;
import thaumicenergistics.network.packets.PacketUIAction;
import thaumicenergistics.part.PartBase;
import thaumicenergistics.part.PartEssentiaTerminal;
import thaumicenergistics.util.ForgeUtil;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author BrockWS
 */
public class ContainerEssentiaTerminal extends ContainerBaseTerminal implements IConfigurableObject {

    private final PartEssentiaTerminal part;

    public ContainerEssentiaTerminal(EntityPlayer player, PartEssentiaTerminal part) {
        super(player, part);
        this.part = part;

        this.bindPlayerInventory(new PlayerMainInvWrapper(player.inventory), 0, 30);
    }

    @Override
    protected AESettings.SUBJECT getAESettingSubject() {
        return AESettings.SUBJECT.ESSENTIA_TERMINAL;
    }

    @Override
    public PartBase getPart() {
        return this.part;
    }

    @Override
    public void onAction(EntityPlayerMP player, PacketUIAction packet) {
        InventoryPlayer inv = player.inventory;
        if (packet.action == ActionType.FILL_ESSENTIA_ITEM && packet.requestedKey instanceof AEEssentiaKey) {
            AEEssentiaKey requestedKey = (AEEssentiaKey) packet.requestedKey;
            ItemStack toFill = inv.getItemStack().copy();
            ResourceLocation registryName = toFill.getItem().getRegistryName();
            if (toFill.isEmpty() || !(toFill.getItem() instanceof IEssentiaContainerItem) || registryName == null)
                return;
            toFill.setCount(1);

            IEssentiaContainerItem containerItem = (IEssentiaContainerItem) toFill.getItem();
            int max = ThEApi.instance().config().essentiaContainerCapacity().getOrDefault(registryName.toString(), 0);
            if (max < 1 || (containerItem.getAspects(toFill) != null && containerItem.getAspects(toFill).size() > 0))
                return;

            MEStorage storage = this.getNetworkStorage();
            if (storage == null)
                return;
            long extracted = storage.extract(requestedKey, max, Actionable.SIMULATE, this.part.source);
            if (extracted < max)
                return;
            containerItem.setAspects(toFill, new AspectList().add(requestedKey.getAspect(), max));
            if (toFill.getItem() instanceof ItemPhial) {
                toFill.setItemDamage(1);
            }
            if (!canReceiveFilledItem(inv, toFill)) {
                return;
            }

            long actualExtracted = storage.extract(requestedKey, max, Actionable.MODULATE, this.part.source);
            if (actualExtracted < max) {
                if (actualExtracted > 0) {
                    storage.insert(requestedKey, actualExtracted, Actionable.MODULATE, this.part.source);
                }
                return;
            }
            if (inv.getItemStack().getCount() > 1) { // Player tried to fill multiple at once
                if (inv.addItemStackToInventory(toFill)) {
                    ItemStack held = inv.getItemStack();
                    held.setCount(held.getCount() - 1);
                    inv.setItemStack(held);
                    PacketHandler.sendToPlayer(player, new PacketInvHeldUpdate(held));
                } else {
                    storage.insert(requestedKey, actualExtracted, Actionable.MODULATE, this.part.source);
                }
            } else {
                player.inventory.setItemStack(toFill);
                PacketHandler.sendToPlayer(player, new PacketInvHeldUpdate(toFill));
            }
        } else if (packet.action == ActionType.EMPTY_ESSENTIA_ITEM) {
            ItemStack toEmpty = inv.getItemStack().copy();
            ResourceLocation registryName = toEmpty.getItem().getRegistryName();
            if (toEmpty.isEmpty() || !(toEmpty.getItem() instanceof IEssentiaContainerItem) || registryName == null)
                return;
            IEssentiaContainerItem containerItem = (IEssentiaContainerItem) toEmpty.getItem();
            AspectList list = containerItem.getAspects(toEmpty);
            if (list == null || list.size() < 1 || ThEApi.instance().config().essentiaContainerCapacity().getOrDefault(registryName.toString(), 0) < 1)
                return;
            AtomicBoolean canInsert = new AtomicBoolean(true);
            MEStorage storage = this.getNetworkStorage();
            if (storage == null)
                return;
            list.aspects.forEach((aspect, amount) -> {
                long inserted = SupergiantEssentiaUtil.insert(storage, aspect, amount, Actionable.SIMULATE, this.part.source);
                if (inserted < amount)
                    canInsert.set(false);
            });
            if (!canInsert.get())
                return;

            AspectList inserted = new AspectList();
            AtomicBoolean actualInsertSucceeded = new AtomicBoolean(true);
            list.aspects.forEach((aspect, amount) -> {
                if (actualInsertSucceeded.get()) {
                    long insertedAmount = SupergiantEssentiaUtil.insert(storage, aspect, amount, Actionable.MODULATE, this.part.source);
                    if (insertedAmount == amount) {
                        inserted.add(aspect, amount);
                    } else {
                        if (insertedAmount > 0) {
                            SupergiantEssentiaUtil.extract(storage, aspect, insertedAmount, Actionable.MODULATE, this.part.source);
                        }
                        actualInsertSucceeded.set(false);
                    }
                }
            });
            if (!actualInsertSucceeded.get()) {
                inserted.aspects.forEach((aspect, amount) -> SupergiantEssentiaUtil.extract(storage, aspect, amount, Actionable.MODULATE, this.part.source));
                return;
            }

            if (toEmpty.getCount() > 1) {
                toEmpty.setCount(1);
                toEmpty.setTagCompound(null);
                toEmpty.setItemDamage(0);
                if (!inv.addItemStackToInventory(toEmpty)) {
                    inserted.aspects.forEach((aspect, amount) -> SupergiantEssentiaUtil.extract(storage, aspect, amount, Actionable.MODULATE, this.part.source));
                    return;
                }
                ItemStack held = inv.getItemStack();
                held.setCount(held.getCount() - 1);
                inv.setItemStack(held);
                PacketHandler.sendToPlayer(player, new PacketInvHeldUpdate(held));
            } else {
                toEmpty.setTagCompound(null);
                toEmpty.setItemDamage(0);
                inv.setItemStack(toEmpty);
                PacketHandler.sendToPlayer(player, new PacketInvHeldUpdate(toEmpty));
            }
        }
        super.onAction(player, packet);
    }

    private static boolean canReceiveFilledItem(InventoryPlayer inv, ItemStack filled) {
        if (inv.getItemStack().getCount() <= 1) {
            return true;
        }
        for (ItemStack stack : inv.mainInventory) {
            if (stack.isEmpty()) {
                return true;
            }
            if (stack.isItemEqual(filled) && ItemStack.areItemStackTagsEqual(stack, filled) && stack.getCount() < Math.min(stack.getMaxStackSize(), inv.getInventoryStackLimit())) {
                return true;
            }
        }
        return false;
    }

    public boolean isValid(Object o) {
        return true;
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        this.sendInventory(listener);
    }

    @Override
    public void detectAndSendChanges() {
        if (ForgeUtil.isServer()) {
            for (Object listener : this.listeners) {
                if (listener instanceof IContainerListener) {
                    this.sendInventory((IContainerListener) listener);
                }
            }
        }
        super.detectAndSendChanges();
    }

    private void sendInventory(IContainerListener listener) {
        if (ForgeUtil.isClient() || !(listener instanceof EntityPlayer))
            return;
        MEStorage storage = this.getNetworkStorage();
        if (storage == null)
            return;
        KeyCounter keys = SupergiantEssentiaUtil.getAvailableEssentia(storage);
        PacketMEEssentiaUpdate packet = new PacketMEEssentiaUpdate();
        for (Object2LongMap.Entry<AEKey> entry : keys) {
            if (entry.getKey() instanceof AEEssentiaKey) {
                packet.appendStack(entry.getKey(), entry.getLongValue(), false);
            }
        }
        PacketHandler.sendToPlayer((EntityPlayerMP) listener, packet);
    }

    private MEStorage getNetworkStorage() {
        if (this.part.getGridNode() == null || this.part.getGridNode().grid() == null) {
            return null;
        }
        return SupergiantEssentiaUtil.getNetworkStorage(this.part.getGridNode().grid());
    }
}
