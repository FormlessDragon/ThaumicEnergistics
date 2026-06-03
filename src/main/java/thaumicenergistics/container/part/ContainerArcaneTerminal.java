package thaumicenergistics.container.part;

import ae2.api.config.Actionable;
import ae2.api.networking.crafting.ICraftingService;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import ae2.container.implementations.ContainerCraftAmount;
import ae2.core.gui.locator.PartLocator;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerArmorInvWrapper;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.items.ItemsTC;
import thaumicenergistics.config.AESettings;
import thaumicenergistics.container.ActionType;
import thaumicenergistics.container.ContainerBaseTerminal;
import thaumicenergistics.container.DummyContainer;
import thaumicenergistics.container.ICraftingContainer;
import thaumicenergistics.container.slot.SlotArcaneMatrix;
import thaumicenergistics.container.slot.SlotArcaneResult;
import thaumicenergistics.container.slot.SlotUpgrade;
import thaumicenergistics.integration.thaumcraft.TCCraftingManager;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketInvHeldUpdate;
import thaumicenergistics.network.packets.PacketMEItemUpdate;
import thaumicenergistics.network.packets.PacketUIAction;
import thaumicenergistics.network.packets.PacketVisUpdate;
import thaumicenergistics.part.PartBase;
import thaumicenergistics.part.PartSharedTerminal;
import thaumicenergistics.util.*;
import thaumicenergistics.util.inventory.ThEInternalInventory;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author BrockWS
 * @author Alex811
 */
public class ContainerArcaneTerminal extends ContainerBaseTerminal implements ICraftingContainer {

    public IRecipe recipe;

    protected PartSharedTerminal part;
    protected IInventory craftingResult;
    protected SlotArcaneResult resultSlot;
    private boolean isValidContainer = true;

    public ContainerArcaneTerminal(EntityPlayer player, PartSharedTerminal part) {
        super(player, part);
        this.part = part;

        this.addMatrixSlots(32, 36);
        this.addUpgradeSlots(177, 54);

        this.bindPlayerInventory(new PlayerMainInvWrapper(player.inventory), 0, 106);
        this.bindPlayerArmour(player, new PlayerArmorInvWrapper(player.inventory), 8, 19);
    }

    @Override
    protected AESettings.SUBJECT getAESettingSubject() {
        return AESettings.SUBJECT.ARCANE_TERMINAL;
    }

    @Override
    public PartBase getPart() {
        return this.part;
    }

    @Override
    public void onAction(EntityPlayerMP player, PacketUIAction packet) {
        // TODO: Give inventoryInsert/inventoryExtract IEnergyGrid to extract power
        MEStorage storage = this.getNetworkStorage();
        if (storage == null)
            return;
        if (packet.action == ActionType.PICKUP_OR_SETDOWN) { // Normal lmb
            if (player.inventory.getItemStack().isEmpty() && packet.requestedKey instanceof AEItemKey) { // PICKUP
                AEItemKey key = (AEItemKey) packet.requestedKey;
                ItemStack request = key.toStack((int) Math.min(key.getMaxStackSize(), Integer.MAX_VALUE));
                ItemStack extracted = this.extractItem(storage, request, request.getCount(), Actionable.MODULATE);

                player.inventory.setItemStack(extracted);
                PacketHandler.sendToPlayer(player, new PacketInvHeldUpdate(player.inventory.getItemStack()));
            } else if (!player.inventory.getItemStack().isEmpty()) { // Set down
                ItemStack remainder = this.insertItem(storage, player.inventory.getItemStack(), player.inventory.getItemStack().getCount(), Actionable.MODULATE);

                player.inventory.setItemStack(remainder);
                PacketHandler.sendToPlayer(player, new PacketInvHeldUpdate(player.inventory.getItemStack()));
            }
        } else if (packet.action == ActionType.SPLIT_OR_PLACE_SINGLE) { // Normal rmb
            if (player.inventory.getItemStack().isEmpty() && packet.requestedKey instanceof AEItemKey) { // Grab half
                AEItemKey key = (AEItemKey) packet.requestedKey;
                long available = storage.extract(key, key.getMaxStackSize(), Actionable.SIMULATE, this.part.source);

                int toPull = (int) Math.ceil((double) available / 2);
                ItemStack extracted = toPull > 0 ? key.toStack((int) storage.extract(key, toPull, Actionable.MODULATE, this.part.source)) : ItemStack.EMPTY;

                player.inventory.setItemStack(extracted);
                PacketHandler.sendToPlayer(player, new PacketInvHeldUpdate(player.inventory.getItemStack()));
            } else if (!player.inventory.getItemStack().isEmpty()) { // Drop single
                ItemStack remainder = this.insertItem(storage, player.inventory.getItemStack(), 1, Actionable.MODULATE);
                if (remainder.isEmpty()) {
                    ItemStack stack2 = player.inventory.getItemStack();
                    stack2.setCount(stack2.getCount() - 1);
                    if (stack2.isEmpty())
                        player.inventory.setItemStack(ItemStack.EMPTY);
                    PacketHandler.sendToPlayer(player, new PacketInvHeldUpdate(player.inventory.getItemStack()));
                }
            }
        } else if ((packet.action == ActionType.SCROLL_UP || packet.action == ActionType.PICKUP_SINGLE) && packet.requestedKey instanceof AEItemKey) { // Shift rmb
            ItemStack held = player.inventory.getItemStack();
            AEItemKey key = (AEItemKey) packet.requestedKey;
            if (!held.isEmpty() && (held.getCount() >= held.getMaxStackSize() || !key.matches(held)))
                return;
            ItemStack stack = this.extractItem(storage, key.toStack(), 1, Actionable.MODULATE);
            if (!stack.isEmpty()) {
                if (!held.isEmpty())
                    held.grow(1);
                else
                    held = stack;
            }
            player.inventory.setItemStack(held);
            PacketHandler.sendToPlayer(player, new PacketInvHeldUpdate(player.inventory.getItemStack()));
        } else if (packet.action == ActionType.SCROLL_DOWN && !player.inventory.getItemStack().isEmpty()) {
            ItemStack held = player.inventory.getItemStack();
            ItemStack remainder = this.insertItem(storage, held, 1, Actionable.MODULATE);
            if (!remainder.isEmpty()) // Failed to insert one item
                return;
            if (held.getCount() > 1) {
                held.shrink(1);
            } else {
                held = ItemStack.EMPTY;
            }
            player.inventory.setItemStack(held);
            PacketHandler.sendToPlayer(player, new PacketInvHeldUpdate(player.inventory.getItemStack()));
        } else if (packet.action == ActionType.SHIFT_MOVE && packet.requestedKey instanceof AEItemKey) {
            AEItemKey key = (AEItemKey) packet.requestedKey;
            ItemStack toMove = key.toStack(key.getMaxStackSize());
            ItemStack remainder = ForgeUtil.addStackToPlayerInventory(player, toMove, true);
            int extractAmount = toMove.getCount() - remainder.getCount();
            ItemStack extracted = this.extractItem(storage, toMove, extractAmount, Actionable.MODULATE);
            if (!extracted.isEmpty())
                ForgeUtil.addStackToPlayerInventory(player, extracted, false);
        } else if (packet.action == ActionType.AUTO_CRAFT) {
            if (!packet.requestedCraftable || !(packet.requestedKey instanceof AEItemKey))
                return;
            ContainerCraftAmount.open(player, new PartLocator(this.part.getLocation().getPos(), this.part.side), packet.requestedKey, 1, this);
        } else if (packet.action == ActionType.CLEAR_GRID) {
            clearCrafting();
        }
        this.onMatrixChanged();
    }

    protected void clearCrafting() {
        MEStorage storage = this.getNetworkStorage();
        if (storage == null)
            return;
        IItemHandler crafting = this.getInventory("crafting");
        for (int slot = 0; slot < crafting.getSlots(); slot++) {
            ItemStack stack = crafting.extractItem(slot, Integer.MAX_VALUE, true);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack remainder = this.insertItem(storage, stack, stack.getCount(), Actionable.MODULATE);
            int inserted = stack.getCount() - remainder.getCount();
            if (inserted > 0) {
                crafting.extractItem(slot, inserted, false);
            }
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        if (ForgeUtil.isClient() || index < 0 || index > this.inventorySlots.size())
            return super.transferStackInSlot(playerIn, index);
        Slot slot = this.inventorySlots.get(index);
        if (slot.getHasStack() && !slot.getStack().isEmpty()) {
            MEStorage storage = this.getNetworkStorage();
            if (storage != null) {
                ItemStack remaining = this.insertItem(storage, slot.getStack(), slot.getStack().getCount(), Actionable.MODULATE);
                slot.putStack(remaining);
            }
            this.detectAndSendChanges();
        }

        return super.transferStackInSlot(playerIn, index);
    }

    @Override
    public void detectAndSendChanges() {
        if (ForgeUtil.isServer()) {
            TileEntity terminal = part.getTile();
            if (terminal != null
                    && terminal.getWorld().getTileEntity(terminal.getPos()) != terminal) {
                this.setValidContainer(false);
            }

            if (this.player instanceof IContainerListener)
                this.sendVisInfo((IContainerListener) this.player);

            for (final Object c : this.listeners) {
                if (c instanceof EntityPlayer) {
                    this.sendInventory((IContainerListener) c);
                }
            }

            super.detectAndSendChanges();
        }
    }

    @Override
    public void handleJEITransfer(EntityPlayer player, NBTTagCompound tag) {
        NBTBase normal = tag.getTag("normal");
        NBTBase crystals = tag.getTag("crystal");

        this.clearCrafting();
        this.onMatrixChanged();

        handleJEITag(0, normal);
        handleJEITag(9, crystals);

        this.onMatrixChanged();
    }

    private void handleJEITag(int startAtSlot, NBTBase ingredientGroup) {
        IItemHandler crafting = this.getInventory("crafting");
        IItemHandler playerInv = this.getInventory("player");

        if (ingredientGroup == null || ingredientGroup.isEmpty()) {
            // TODO: Probably check if its already in the slot
            return;
        }
        NBTTagList subs = (NBTTagList) ingredientGroup;
        for (int i = 0; i < subs.tagCount(); i++) {
            int slot = startAtSlot + i;
            NBTTagCompound ingredient = ((NBTTagList) subs.get(i)).getCompoundTagAt(0);
            ItemStack stack = new ItemStack(ingredient);
            if (stack.isEmpty()) continue;

            ThELog.debug("Adding {} for {}", stack.getDisplayName(), slot);
            MEStorage storage = this.getNetworkStorage();
            ItemStack aeExtract = storage == null ? ItemStack.EMPTY : this.extractItem(storage, stack, stack.getCount(), Actionable.MODULATE);
            if (!aeExtract.isEmpty())
                crafting.insertItem(slot, aeExtract, false);

            if (crafting.getStackInSlot(slot).getCount() >= stack.getCount()) // We managed to pull everything from the system
                continue;

            // Try pull from player
            ThELog.debug("Failed to pull item from ae inv, trying player inventory");
            stack.shrink(crafting.getStackInSlot(slot).getCount());

            ItemStack invExtract = ItemHandlerUtil.extract(playerInv, stack, false);
            if (!invExtract.isEmpty())
                crafting.insertItem(slot, invExtract, false);
        }
        ThELog.debug("Failed to find valid item");
    }

    public boolean isValid(Object verificationToken) {
        return true;
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        this.sendVisInfo(listener);
        this.sendInventory(listener);
        this.onMatrixChanged();
    }

    @Override
    public void onMatrixChanged() {
        if (ForgeUtil.isClient())
            return;
        this.craftingResult.setInventorySlotContents(0, ItemStack.EMPTY);
        this.detectAndSendChanges();
        IItemHandler matrix = this.getInventory("crafting");
        this.recipe = TCCraftingManager.findArcaneRecipe(matrix, this.player);
        if (this.recipe != null) {
            this.craftingResult.setInventorySlotContents(0, TCCraftingManager.getCraftingResult(this.getInventory("crafting"), (IArcaneRecipe) this.recipe));
            this.detectAndSendChanges();
            return;
        }
        InventoryCrafting inventory = new InventoryCrafting(new DummyContainer(), 3, 3);
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            inventory.setInventorySlotContents(i, matrix.getStackInSlot(i));
        }
        this.recipe = CraftingManager.findMatchingRecipe(inventory, this.player.world);
        if (this.recipe != null) {
            this.craftingResult.setInventorySlotContents(0, this.recipe.getCraftingResult(inventory));
            this.detectAndSendChanges();
        }
    }

    @Override
    public int tryCraft(int amount) {
        this.onMatrixChanged();
        if (this.recipe == null || ForgeUtil.isClient())
            return 0;
        float canCraft = amount;
        if (this.recipe instanceof IArcaneRecipe) {
            float visRequired = ((IArcaneRecipe) this.recipe).getVis() * (1f - this.getDiscount(this.player));
            canCraft = this.getWorldVis() / visRequired;
        }

        return Math.min(amount, (int) canCraft);
    }

    @Override
    public ItemStack onCraft(ItemStack toCraft) {
        if (toCraft.isEmpty())
            return ItemStack.EMPTY;
        IItemHandler crafting = this.getInventory("crafting");
        InventoryCrafting inv = this.getInvCrafting(crafting, this.recipe);
        ItemStack crafted = this.recipe.getCraftingResult(inv);
        int roomLeft = Math.min(crafted.getMaxStackSize(), toCraft.getCount() * crafted.getCount());
        int timesCrafted = 0;
        boolean craftAgain = true;
        do {
            roomLeft -= crafted.getCount();
            NonNullList<ItemStack> remaining = this.getRemaining(this.recipe, inv);

            for (int j = 0; j < remaining.size(); j++) {
                if (crafting.getStackInSlot(j).isEmpty()) // The slot is empty so ignore it
                    continue;
                ItemStack extract = crafting.extractItem(j, Integer.MAX_VALUE, false);
                if (!remaining.get(j).isEmpty()) { // We still have some remaining
                    crafting.insertItem(j, remaining.get(j), false);
                } else {
                    crafting.insertItem(j, this.getRefill(extract), false);
                }
            }
            if (this.getCurrentRequiredVis() > 0)
                TCUtil.drainVis(this.part.getTile().getWorld(),
                        this.part.getTile().getPos(),
                        this.getCurrentRequiredVis(),
                        this.getInventory("upgrades").getStackInSlot(0).isEmpty() ? 0 : 1);

            // Re-craft safety checks
            inv = this.getInvCrafting(crafting, this.recipe);

            if (!this.recipe.matches(inv, this.player.world)) // Check if we can craft again
                craftAgain = false;

            if (this.getWorldVis() < this.getCurrentRequiredVis())
                craftAgain = false;

            timesCrafted++;
        } while (roomLeft > 0 && roomLeft >= crafted.getCount() && craftAgain);
        crafted.setCount(timesCrafted * crafted.getCount());
        this.onMatrixChanged();
        this.detectAndSendChanges();
        if (crafted.getCount() > 0) {
            crafted.onCrafting(this.player.world, this.player, crafted.getCount());
            FMLCommonHandler.instance().firePlayerCraftingEvent(this.player, crafted, inv);

            if (this.recipe != null && !this.recipe.isDynamic()) {
                this.player.unlockRecipes(Lists.newArrayList(this.recipe));
            }
        }
        return crafted;
    }

    @Override
    public IItemHandler getInventory(String name) {
        switch (name.toLowerCase()) {
            case "crafting":
            case "upgrades":
                return this.part.getInventoryByName(name);
            case "result":
                return new InvWrapper(this.craftingResult);
            case "player":
                return new PlayerInvWrapper(this.player.inventory);
        }
        return null;
    }

    public EntityPlayer getPlayer() {
        return this.player;
    }

    public BlockPos getPartPos() {
        return this.part.getLocation().getPos();
    }

    public net.minecraft.util.EnumFacing getPartSide() {
        return this.part.side;
    }

    @SuppressWarnings("SameParameterValue")
    protected void addMatrixSlots(int offsetX, int offsetY) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlotToContainer(new SlotArcaneMatrix(this, i * 3 + j, offsetX + (j * 18), offsetY + (i * 18)));
            }
        }
        offsetX += 104;
        for (int i = 0; i < 3; i++) { // Y
            for (int j = 0; j < 2; j++) { // X
                this.addSlotToContainer(new SlotArcaneMatrix(this, 9 + (i * 2 + j), offsetX + (j * 18), offsetY + (i * 18)));
            }
        }
        offsetX -= 104;
        this.craftingResult = new ThEInternalInventory("Result", 1, 64);
        this.addSlotToContainer(this.resultSlot = new SlotArcaneResult(this, this.player, 0, offsetX + 84, offsetY + 18));
        this.onMatrixChanged();
    }

    protected void addUpgradeSlots(int offsetX, int offsetY) {
        this.addSlotToContainer(new SlotUpgrade(this.getInventory("upgrades"), 0, offsetX, offsetY)/* {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return ThEApi.instance().items().upgradeArcane().isSameAs(stack);
            }
        }*/);
    }

    protected void sendVisInfo(IContainerListener listener) {
        if (ForgeUtil.isClient() || !(listener instanceof EntityPlayerMP))
            return;
        PacketHandler.sendToPlayer((EntityPlayerMP) this.player, new PacketVisUpdate(this.getWorldVis(), this.getCurrentRequiredVis(), this.getDiscount(this.player)));
    }

    protected float getWorldVis() {
        TileEntity te = this.part.getTile();
        float vis = AuraHelper.getVis(te.getWorld(), te.getPos());
        if (!this.getInventory("upgrades").getStackInSlot(0).isEmpty()) {
            vis += AuraHelper.getVis(te.getWorld(), te.getPos().add(-16, 0, -16));
            vis += AuraHelper.getVis(te.getWorld(), te.getPos().add(-16, 0, 0));
            vis += AuraHelper.getVis(te.getWorld(), te.getPos().add(-16, 0, 16));

            vis += AuraHelper.getVis(te.getWorld(), te.getPos().add(0, 0, -16));
            vis += AuraHelper.getVis(te.getWorld(), te.getPos().add(0, 0, 16));

            vis += AuraHelper.getVis(te.getWorld(), te.getPos().add(16, 0, -16));
            vis += AuraHelper.getVis(te.getWorld(), te.getPos().add(16, 0, 0));
            vis += AuraHelper.getVis(te.getWorld(), te.getPos().add(16, 0, 16));
        }
        return vis;
    }

    protected float getRequiredVis(IRecipe recipe, EntityPlayer player) {
        if (!(recipe instanceof IArcaneRecipe))
            return -1;
        return ((IArcaneRecipe) recipe).getVis() * (1f - this.getDiscount(player));
    }

    public float getCurrentRequiredVis() {
        return this.getRequiredVis(this.recipe, this.player);
    }

    protected float getDiscount(EntityPlayer player) {
        return TCCraftingManager.getDiscount(player);
    }

    private NonNullList<ItemStack> getRemaining(IRecipe recipe, InventoryCrafting inv) {
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(inv);
        AspectList crystals = this.recipe instanceof IArcaneRecipe ? ((IArcaneRecipe) this.recipe).getCrystals() : null;
        for (int i = 0; i < remaining.size(); i++) {
            if (i < 9) {
                boolean hasLeftover = !remaining.get(i).isEmpty();
                ItemStack existing = inv.getStackInSlot(i);
                if (existing.getCount() > 1) { // We had more than one
                    if (!hasLeftover)
                        existing.shrink(1);
                    remaining.set(i, existing);
                }
            } else {
                if (crystals == null || crystals.size() < 1) // We don't require crystals in this recipe
                    break;
                ItemStack crystalStack = inv.getStackInSlot(i);
                if (crystalStack.isEmpty())
                    continue;
                Aspect crystalAspect = TCUtil.getCrystalAspect(crystalStack);
                if (crystals.getAmount(crystalAspect) > 0) // We require X aspects in this recipe
                    crystalStack.shrink(crystals.getAmount(crystalAspect));
                if (crystalStack.getCount() > 0)
                    remaining.set(i, crystalStack);
            }
        }
        return remaining;
    }

    private ItemStack getRefill(ItemStack stack) {
        // TODO: Fuzzy selection
        MEStorage storage = this.getNetworkStorage();
        if (storage != null && this.extractItem(storage, stack, stack.getCount(), Actionable.SIMULATE).getCount() == stack.getCount()) {// Make sure we actually have enough to pull
           /* try {
                GridUtil.getEnergyGrid(this.part.getGridNode()).extractAEPower(1, Actionable.MODULATE, PowerMultiplier.CONFIG);
            } catch (GridAccessException ignored) {

            }*/
            return this.extractItem(storage, stack, stack.getCount(), Actionable.MODULATE);
        }
        return ItemStack.EMPTY;
    }

    protected void sendInventory(IContainerListener listener) {
        if (ForgeUtil.isClient() || !(listener instanceof EntityPlayerMP))
            return;

        try {
            MEStorage storage = this.getNetworkStorage();
            if (storage == null)
                return;
            PacketMEItemUpdate packet = new PacketMEItemUpdate();
            boolean firstPacket = true;
            KeyCounter stacks = storage.getAvailableStacks();
            Map<AEKey, Long> storedItems = new LinkedHashMap<>();

            for (Object2LongMap.Entry<AEKey> stack : stacks) {
                if (!(stack.getKey() instanceof AEItemKey)) {
                    continue;
                }
                storedItems.put(stack.getKey(), stack.getLongValue());
            }

            ICraftingService craftingService = this.part.getGridNode() == null || this.part.getGridNode().grid() == null
                    ? null
                    : this.part.getGridNode().grid().getCraftingService();
            Iterable<AEKey> craftables = craftingService == null
                    ? java.util.Collections.emptySet()
                    : craftingService.getCraftables(key -> key instanceof AEItemKey);

            for (AEKey key : craftables) {
                storedItems.putIfAbsent(key, 0L);
            }

            for (Map.Entry<AEKey, Long> stack : storedItems.entrySet()) {
                boolean craftable = craftingService != null && craftingService.isCraftable(stack.getKey());
                try {
                    packet.appendStack(stack.getKey(), stack.getValue(), craftable);
                } catch (BufferOverflowException e) {
                    packet.setClearExisting(firstPacket);
                    PacketHandler.sendToPlayer((EntityPlayerMP) listener, packet);
                    firstPacket = false;

                    packet = new PacketMEItemUpdate();
                    packet.setClearExisting(false);
                    packet.appendStack(stack.getKey(), stack.getValue(), craftable);
                }
            }
            packet.setClearExisting(firstPacket);
            PacketHandler.sendToPlayer((EntityPlayerMP) listener, packet);
        } catch (IOException e) {
            ThELog.error("sendInventory", e);
        }
    }

    protected MEStorage getNetworkStorage() {
        return this.part.getInventory();
    }

    protected ItemStack extractItem(MEStorage storage, ItemStack stack, int amount, Actionable mode) {
        if (storage == null || stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        AEItemKey key = AEItemKey.of(stack);
        long extracted = storage.extract(key, amount, mode, this.part.source);
        if (extracted <= 0) {
            return ItemStack.EMPTY;
        }
        return key.toStack((int) Math.min(extracted, Integer.MAX_VALUE));
    }

    protected ItemStack insertItem(MEStorage storage, ItemStack stack, int amount, Actionable mode) {
        if (storage == null || stack.isEmpty() || amount <= 0) {
            return stack;
        }
        AEItemKey key = AEItemKey.of(stack);
        long inserted = storage.insert(key, amount, mode, this.part.source);
        int remaining = amount - (int) Math.min(inserted, Integer.MAX_VALUE);
        if (remaining <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack remainder = stack.copy();
        remainder.setCount(remaining);
        return remainder;
    }

    private InventoryCrafting getInvCrafting(IItemHandler handler, IRecipe recipe) {
        if (recipe instanceof IArcaneRecipe)
            return TCCraftingManager.getInvFromItemHandler(handler);
        InventoryCrafting inv = new InventoryCrafting(new DummyContainer(), 3, 3);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            inv.setInventorySlotContents(i, this.getInventory("crafting").getStackInSlot(i).copy());
        }
        return inv;
    }

    private boolean isCrystalRequired(IRecipe recipe, ItemStack stack) {
        if (!(recipe instanceof IArcaneRecipe) || stack.isEmpty() || !(stack.getItem() instanceof IEssentiaContainerItem) || stack.getItem() != ItemsTC.crystalEssence)
            return false;
        AspectList aspect = ((IEssentiaContainerItem) stack.getItem()).getAspects(stack);
        return ((IArcaneRecipe) recipe).getCrystals().getAmount(aspect.getAspects()[0]) > 0;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (this.isValidContainer()) {
            if (part.getTile() instanceof IInventory) {
                return ((IInventory) part.getTile()).isUsableByPlayer(player);
            }
            return true;
        }
        return false;
    }

    public boolean isValidContainer() {
        return isValidContainer;
    }

    public void setValidContainer(boolean validContainer) {
        isValidContainer = validContainer;
    }
}
