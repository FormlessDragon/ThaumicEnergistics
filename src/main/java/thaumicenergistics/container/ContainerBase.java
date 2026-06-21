package thaumicenergistics.container;

import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumicenergistics.container.slot.SlotArcaneResult;
import thaumicenergistics.container.slot.SlotArmor;
import thaumicenergistics.container.slot.SlotGhost;
import thaumicenergistics.container.slot.SlotGhostEssentia;
import thaumicenergistics.container.slot.ThEGhostSlot;
import thaumicenergistics.container.slot.ThESlot;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketInvHeldUpdate;
import thaumicenergistics.util.EssentiaFilter;
import thaumicenergistics.util.ForgeUtil;

import java.util.Objects;

/**
 * The base container for all containers in Thaumic Energistics
 * <p>
 *
 * @author BrockWS
 */
public abstract class ContainerBase extends AEBaseContainer {

    public EntityPlayer player;

    public ContainerBase(EntityPlayer player) {
        super(Objects.requireNonNull(player, "player").inventory, null);
        this.player = player;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        if (index < 0 || index >= this.inventorySlots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = this.getSlot(index);
        ItemStack originalStack = slot.getStack().copy();
        ItemStack movedStack = super.transferStackInSlot(playerIn, index);
        if (!movedStack.isEmpty() || originalStack.isEmpty()) {
            return movedStack;
        }

        ItemStack remainingStack = slot.getStack();
        return ItemStack.areItemStacksEqual(originalStack, remainingStack)
                && ItemStack.areItemStackTagsEqual(originalStack, remainingStack)
                ? ItemStack.EMPTY
                : originalStack;
    }

    @Override
    public ItemStack slotClick(int slotID, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotID < 0)
            return super.slotClick(slotID, dragType, clickType, player);
        if (slotID >= this.inventorySlots.size())
            return ItemStack.EMPTY;

        Slot slot = this.getSlot(slotID);
        if (slot instanceof SlotGhostEssentia) {
            if (((SlotGhostEssentia) slot).getFilter() != null) {
                EssentiaFilter filter = ((SlotGhostEssentia) slot).getFilter();
                ItemStack stack = player.inventory.getItemStack().copy();
                int id = slot.getSlotIndex();

                if (stack.getItem() instanceof IEssentiaContainerItem) {
                    IEssentiaContainerItem item = (IEssentiaContainerItem) stack.getItem();
                    if (item.getAspects(stack) != null) {
                        AspectList aspects = item.getAspects(stack);
                        filter.setAspect(aspects.getAspects()[0], id);
                    }
                } else {
                    filter.setAspect(null, id);
                }
                return ItemStack.EMPTY;
            }
        }
        if (slot instanceof SlotGhost) {
            ItemStack stack = player.inventory.getItemStack().copy();
            stack.setCount(1);
            slot.putStack(stack);
            return ItemStack.EMPTY;
        }
        if (slot instanceof SlotArcaneResult && this instanceof ICraftingContainer) {
            ICraftingContainer craftingContainer = ((ICraftingContainer) this);
            ItemStack held = player.inventory.getItemStack();
            if (ForgeUtil.isServer() && (held.isEmpty() || slot.getStack().isItemEqual(held)) && (clickType == ClickType.QUICK_MOVE || slot.getStack().getMaxStackSize() - held.getCount() >= slot.getStack().getCount())) {
                int numToCraft = clickType == ClickType.QUICK_MOVE ? Integer.MAX_VALUE : 1; // if quick move, calc max craftable amount, else craft 1
                int canCraftNum = craftingContainer.tryCraft(numToCraft); // we can craft this amount
                if (canCraftNum > 0) {
                    ItemStack toCraft = slot.getStack().copy();
                    toCraft.setCount(canCraftNum);
                    if (clickType == ClickType.QUICK_MOVE) {
                        int canFitInInvNum = canCraftNum - ForgeUtil.addStackToPlayerInventory(player, toCraft, true).getCount(); // check how much fits in the player's inventory
                        if (canFitInInvNum < canCraftNum)
                            toCraft.setCount(canFitInInvNum); // if it doesn't fit, craft as much as we can fit
                        ItemStack newToStore = craftingContainer.onCraft(toCraft);
                        ForgeUtil.addStackToPlayerInventory(player, newToStore, false);
                    } else {
                        ItemStack newHeld = craftingContainer.onCraft(toCraft);
                        newHeld.grow(held.getCount());
                        player.inventory.setItemStack(newHeld);
                        PacketHandler.sendToPlayer((EntityPlayerMP) player, new PacketInvHeldUpdate(newHeld));
                    }
                }
            }
            return ItemStack.EMPTY;
        }
        return super.slotClick(slotID, dragType, clickType, player);
    }

    @Override
    protected boolean isValidQuickMoveDestination(Slot candidateSlot, ItemStack stackToMove, boolean fromPlayerSide) {
        return !(candidateSlot instanceof SlotGhost)
                && !(candidateSlot instanceof ThEGhostSlot)
                && !(candidateSlot instanceof SlotGhostEssentia)
                && !(candidateSlot instanceof SlotArcaneResult)
                && super.isValidQuickMoveDestination(candidateSlot, stackToMove, fromPlayerSide);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    protected void bindPlayerArmour(EntityPlayer player, IItemHandler inv, int offsetX, int offsetY) {
        this.addSlot(new SlotArmor(player, inv, 0, offsetX, offsetY + 8 + 18 * 3), ThESlotSemantics.PLAYER_ARMOR);
        this.addSlot(new SlotArmor(player, inv, 1, offsetX, offsetY + 8 + 18 * 2), ThESlotSemantics.PLAYER_ARMOR);
        this.addSlot(new SlotArmor(player, inv, 2, offsetX, offsetY + 8 + 18), ThESlotSemantics.PLAYER_ARMOR);
        this.addSlot(new SlotArmor(player, inv, 3, offsetX, offsetY + 8), ThESlotSemantics.PLAYER_ARMOR);
    }

    protected void bindPlayerInventory(IItemHandler player, int offsetX, int offsetY) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new ThESlot(player, 9 * i + j + 9, offsetX + 8 + 18 * j, offsetY + 2 + 18 * i),
                        SlotSemantics.PLAYER_INVENTORY);
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new ThESlot(player, i, offsetX + 8 + 18 * i, offsetY + 60),
                    SlotSemantics.PLAYER_HOTBAR);
        }
    }

    public EssentiaFilter getEssentiaFilter() {
        return null;
    }

    public void setEssentiaFilter(EssentiaFilter filter) {
        this.getEssentiaFilter().deserializeNBT(filter.serializeNBT());
    }

    public void handleJEITransfer(EntityPlayer player, NBTTagCompound tag) {

    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slotIn) { // prevent stack merging (double-click) here
        if (slotIn instanceof SlotArcaneResult)
            return false;
        return super.canMergeSlot(stack, slotIn);
    }
}
