package thaumicenergistics.container.part;

import ae2.api.config.Actionable;
import ae2.api.stacks.AEItemKey;
import ae2.api.storage.MEStorage;
import ae2.api.storage.StorageHelper;
import ae2.container.GuiIds;
import ae2.container.SlotSemantics;
import ae2.container.me.common.ContainerMEStorage;
import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.PlayerArmorInvWrapper;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumicenergistics.container.DummyContainer;
import thaumicenergistics.container.ICraftingContainer;
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.slot.SlotArmor;
import thaumicenergistics.container.slot.SlotArcaneMatrix;
import thaumicenergistics.container.slot.SlotArcaneResult;
import thaumicenergistics.container.slot.SlotUpgrade;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.integration.thaumcraft.TCCraftingManager;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketVisUpdate;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.ItemHandlerUtil;
import thaumicenergistics.util.TCUtil;
import thaumicenergistics.util.ThELog;
import thaumicenergistics.util.inventory.ThEInternalInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContainerArcaneTerm extends ContainerMEStorage implements ICraftingContainer {

    private static final String ACTION_CLEAR_GRID = "clearGrid";
    private static final String ACTION_SET_CLEAR_ON_CLOSE = "setClearOnClose";

    protected final IArcaneTerminalHost host;
    protected final IInventory craftingResult;
    protected SlotArcaneResult resultSlot;
    protected IRecipe recipe;
    private boolean clearGridOnClose;

    public ContainerArcaneTerm(InventoryPlayer ip, IArcaneTerminalHost host) {
        this(GuiIds.GuiKey.ME_STORAGE_TERMINAL, ip, host);
    }

    protected ContainerArcaneTerm(GuiIds.GuiKey guiKey, InventoryPlayer ip, IArcaneTerminalHost host) {
        super(guiKey, ip, host);
        this.host = host;
        this.craftingResult = new ThEInternalInventory("Result", 1, 64);

        this.addMatrixSlots(32, 36);
        this.addUpgradeSlots(177, 54);
        this.addArmorSlots(ip.player, new PlayerArmorInvWrapper(ip), 8, 19);
        this.registerClientAction(ACTION_CLEAR_GRID, this::clearCraftingGrid);
        this.registerClientAction(ACTION_SET_CLEAR_ON_CLOSE, Boolean.class, this::setClearGridOnClose);
        this.updateCraftingResult();
    }

    public IArcaneTerminalHost getArcaneHost() {
        return this.host;
    }

    public IArcaneTerminalHost getHost() {
        return this.host;
    }

    @Override
    public EntityPlayer getPlayer() {
        return this.getPlayerInventory().player;
    }

    public BlockPos getPartPos() {
        return this.host.getReturnPos();
    }

    public EnumFacing getPartSide() {
        return this.host.getReturnSide();
    }

    public IRecipe getCurrentRecipe() {
        return this.recipe;
    }

    public float getCurrentRequiredVis() {
        return this.getRequiredVis(this.recipe, this.getPlayer());
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        this.sendVisInfo(listener);
    }

    @Override
    public void detectAndSendChanges() {
        if (this.isServerSide() && this.getPlayer() instanceof IContainerListener) {
            this.sendVisInfo((IContainerListener) this.getPlayer());
        }
        super.detectAndSendChanges();
    }

    protected void sendVisInfo(IContainerListener listener) {
        if (this.isClientSide() || !(listener instanceof EntityPlayerMP player)) {
            return;
        }
        PacketHandler.sendToPlayer(player,
                new PacketVisUpdate(this.getWorldVis(), this.getCurrentRequiredVis(), this.getDiscount(this.getPlayer())));
    }

    @Override
    public IItemHandler getInventory(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "crafting":
            case "upgrades":
                return this.host.getInventoryByName(name);
            case "result":
                return new InvWrapper(this.craftingResult);
            case "player":
                return new PlayerInvWrapper(this.getPlayerInventory());
            default:
                return null;
        }
    }

    @Override
    public void onMatrixChanged() {
        if (this.isClientSide()) {
            return;
        }

        this.updateCraftingResult();
        this.detectAndSendChanges();
    }

    private void updateCraftingResult() {
        if (this.isClientSide()) {
            return;
        }

        this.craftingResult.setInventorySlotContents(0, ItemStack.EMPTY);

        IItemHandler matrix = this.getInventory("crafting");
        this.recipe = TCCraftingManager.findArcaneRecipe(matrix, this.getPlayer());
        if (this.recipe != null) {
            this.craftingResult.setInventorySlotContents(0,
                    TCCraftingManager.getCraftingResult(matrix, (IArcaneRecipe) this.recipe));
            return;
        }

        InventoryCrafting inventory = new InventoryCrafting(new DummyContainer(), 3, 3);
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            inventory.setInventorySlotContents(i, matrix.getStackInSlot(i));
        }

        this.recipe = CraftingManager.findMatchingRecipe(inventory, this.getPlayer().world);
        if (this.recipe != null) {
            this.craftingResult.setInventorySlotContents(0, this.recipe.getCraftingResult(inventory));
        }
    }

    @Override
    public ItemStack slotClick(int slotID, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotID < 0 || slotID >= this.inventorySlots.size()) {
            return super.slotClick(slotID, dragType, clickType, player);
        }

        Slot slot = this.getSlot(slotID);
        if (slot instanceof SlotArcaneResult) {
            this.handleArcaneResultSlotClick(slot, clickType, player);
            return ItemStack.EMPTY;
        }

        return super.slotClick(slotID, dragType, clickType, player);
    }

    private void handleArcaneResultSlotClick(Slot slot, ClickType clickType, EntityPlayer player) {
        if (this.isClientSide()) {
            return;
        }

        ItemStack result = slot.getStack();
        ItemStack held = player.inventory.getItemStack();
        if (result.isEmpty() || (!held.isEmpty() && !result.isItemEqual(held))) {
            return;
        }
        if (clickType != ClickType.QUICK_MOVE && result.getMaxStackSize() - held.getCount() < result.getCount()) {
            return;
        }

        int requested = clickType == ClickType.QUICK_MOVE ? Integer.MAX_VALUE : 1;
        int canCraft = this.tryCraft(requested);
        if (canCraft <= 0) {
            return;
        }

        ItemStack toCraft = result.copy();
        toCraft.setCount(canCraft);
        if (clickType == ClickType.QUICK_MOVE) {
            int canFit = canCraft - ForgeUtil.addStackToPlayerInventory(player, toCraft, true).getCount();
            if (canFit <= 0) {
                return;
            }
            toCraft.setCount(canFit);
            ForgeUtil.addStackToPlayerInventory(player, this.onCraft(toCraft), false);
        } else {
            ItemStack newHeld = this.onCraft(toCraft);
            if (!held.isEmpty()) {
                newHeld.grow(held.getCount());
            }
            player.inventory.setItemStack(newHeld);
        }

        if (player instanceof EntityPlayerMP serverPlayer && serverPlayer.openContainer == this) {
            this.syncInventoryActionState(serverPlayer);
        }
    }

    @Override
    public int tryCraft(int amount) {
        this.onMatrixChanged();
        if (this.recipe == null || this.isClientSide()) {
            return 0;
        }

        float canCraft = amount;
        if (this.recipe instanceof IArcaneRecipe) {
            float visRequired = ((IArcaneRecipe) this.recipe).getVis() * (1f - this.getDiscount(this.getPlayer()));
            if (visRequired <= 0) {
                return amount;
            }
            canCraft = this.getWorldVis() / visRequired;
        }

        return Math.min(amount, (int) canCraft);
    }

    @Override
    public ItemStack onCraft(ItemStack toCraft) {
        if (toCraft.isEmpty() || this.recipe == null) {
            return ItemStack.EMPTY;
        }

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
                if (crafting.getStackInSlot(j).isEmpty()) {
                    continue;
                }

                ItemStack extract = crafting.extractItem(j, Integer.MAX_VALUE, false);
                if (!remaining.get(j).isEmpty()) {
                    crafting.insertItem(j, remaining.get(j), false);
                } else {
                    crafting.insertItem(j, this.getRefill(extract), false);
                }
            }

            if (this.getCurrentRequiredVis() > 0) {
                TCUtil.drainVis(this.host.getVisWorld(),
                        this.host.getVisPos(),
                        this.getCurrentRequiredVis(),
                        this.getInventory("upgrades").getStackInSlot(0).isEmpty() ? 0 : 1);
            }

            inv = this.getInvCrafting(crafting, this.recipe);
            if (!this.recipe.matches(inv, this.getPlayer().world)) {
                craftAgain = false;
            }
            if (this.getWorldVis() < this.getCurrentRequiredVis()) {
                craftAgain = false;
            }

            timesCrafted++;
        } while (roomLeft > 0 && roomLeft >= crafted.getCount() && craftAgain);

        crafted.setCount(timesCrafted * crafted.getCount());
        this.onMatrixChanged();
        this.detectAndSendChanges();

        if (crafted.getCount() > 0) {
            crafted.onCrafting(this.getPlayer().world, this.getPlayer(), crafted.getCount());
            FMLCommonHandler.instance().firePlayerCraftingEvent(this.getPlayer(), crafted, inv);

            if (this.recipe != null && !this.recipe.isDynamic()) {
                this.getPlayer().unlockRecipes(Lists.newArrayList(this.recipe));
            }
        }

        return crafted;
    }

    private NonNullList<ItemStack> getRemaining(IRecipe recipe, InventoryCrafting inv) {
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(inv);
        AspectList crystals = this.recipe instanceof IArcaneRecipe ? ((IArcaneRecipe) this.recipe).getCrystals() : null;

        for (int i = 0; i < remaining.size(); i++) {
            if (i < 9) {
                boolean hasLeftover = !remaining.get(i).isEmpty();
                ItemStack existing = inv.getStackInSlot(i);
                if (existing.getCount() > 1) {
                    if (!hasLeftover) {
                        existing.shrink(1);
                    }
                    remaining.set(i, existing);
                }
            } else {
                if (crystals == null || crystals.size() < 1) {
                    break;
                }

                ItemStack crystalStack = inv.getStackInSlot(i);
                if (crystalStack.isEmpty()) {
                    continue;
                }

                Aspect crystalAspect = TCUtil.getCrystalAspect(crystalStack);
                if (crystals.getAmount(crystalAspect) > 0) {
                    crystalStack.shrink(crystals.getAmount(crystalAspect));
                }
                if (crystalStack.getCount() > 0) {
                    remaining.set(i, crystalStack);
                }
            }
        }

        return remaining;
    }

    private ItemStack getRefill(ItemStack stack) {
        if (this.storage != null
                && this.extractItem(this.storage, stack, stack.getCount(), Actionable.SIMULATE).getCount() == stack.getCount()) {
            return this.extractItem(this.storage, stack, stack.getCount(), Actionable.MODULATE);
        }
        return ItemStack.EMPTY;
    }

    private InventoryCrafting getInvCrafting(IItemHandler handler, IRecipe recipe) {
        if (recipe instanceof IArcaneRecipe) {
            return TCCraftingManager.getInvFromItemHandler(handler);
        }

        InventoryCrafting inv = new InventoryCrafting(new DummyContainer(), 3, 3);
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            inv.setInventorySlotContents(i, this.getInventory("crafting").getStackInSlot(i).copy());
        }
        return inv;
    }

    protected float getWorldVis() {
        if (!this.host.hasVisSource()) {
            return 0;
        }
        World world = this.host.getVisWorld();
        BlockPos pos = this.host.getVisPos();
        float vis = AuraHelper.getVis(world, pos);
        if (!this.getInventory("upgrades").getStackInSlot(0).isEmpty()) {
            vis += AuraHelper.getVis(world, pos.add(-16, 0, -16));
            vis += AuraHelper.getVis(world, pos.add(-16, 0, 0));
            vis += AuraHelper.getVis(world, pos.add(-16, 0, 16));

            vis += AuraHelper.getVis(world, pos.add(0, 0, -16));
            vis += AuraHelper.getVis(world, pos.add(0, 0, 16));

            vis += AuraHelper.getVis(world, pos.add(16, 0, -16));
            vis += AuraHelper.getVis(world, pos.add(16, 0, 0));
            vis += AuraHelper.getVis(world, pos.add(16, 0, 16));
        }
        return vis;
    }

    protected float getRequiredVis(IRecipe recipe, EntityPlayer player) {
        if (!(recipe instanceof IArcaneRecipe)) {
            return -1;
        }
        return ((IArcaneRecipe) recipe).getVis() * (1f - this.getDiscount(player));
    }

    protected float getDiscount(EntityPlayer player) {
        return TCCraftingManager.getDiscount(player);
    }

    protected ItemStack extractItem(MEStorage storage, ItemStack stack, int amount, Actionable mode) {
        if (storage == null || stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            return ItemStack.EMPTY;
        }

        long extracted = StorageHelper.poweredExtraction(this.energySource, storage, key, amount, this.getActionSource(), mode);
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
        if (key == null) {
            return stack;
        }

        long inserted = StorageHelper.poweredInsert(this.energySource, storage, key, amount, this.getActionSource(), mode);
        int remaining = amount - (int) Math.min(inserted, Integer.MAX_VALUE);
        if (remaining <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack remainder = stack.copy();
        remainder.setCount(remaining);
        return remainder;
    }

    public void handleJEITransfer(EntityPlayer player, NBTTagCompound tag) {
        if (tag == null || tag.isEmpty()) {
            return;
        }

        NBTBase normal = tag.getTag("normal");
        NBTBase crystals = tag.getTag("crystal");

        if (!this.clearCraftingIntoNetworkForJEI()) {
            this.onMatrixChanged();
            return;
        }
        this.onMatrixChanged();

        this.handleJEITag(0, normal);
        this.handleJEITag(9, crystals);

        this.onMatrixChanged();
    }

    private void handleJEITag(int startAtSlot, NBTBase ingredientGroup) {
        IItemHandler crafting = this.getInventory("crafting");
        IItemHandler playerInv = this.getInventory("player");

        if (ingredientGroup == null || ingredientGroup.isEmpty()) {
            return;
        }

        if (!(ingredientGroup instanceof NBTTagList)) {
            ThELog.warn("Invalid JEI ingredient group: {}", ingredientGroup);
            return;
        }

        NBTTagList subs = (NBTTagList) ingredientGroup;
        for (int i = 0; i < subs.tagCount(); i++) {
            int slot = startAtSlot + i;
            NBTBase sub = subs.get(i);
            if (!(sub instanceof NBTTagList)) {
                ThELog.warn("Invalid JEI ingredient entry: {}", sub);
                continue;
            }

            NBTTagList alternatives = (NBTTagList) sub;
            if (alternatives.tagCount() <= 0) {
                continue;
            }

            NBTTagCompound ingredient = alternatives.getCompoundTagAt(0);
            ItemStack stack = new ItemStack(ingredient);
            if (stack.isEmpty()) {
                continue;
            }

            ThELog.debug("Adding {} for {}", stack.getDisplayName(), slot);
            ItemStack aeExtract = this.storage == null
                    ? ItemStack.EMPTY
                    : this.extractItem(this.storage, stack, stack.getCount(), Actionable.MODULATE);
            if (!aeExtract.isEmpty()) {
                crafting.insertItem(slot, aeExtract, false);
            }

            if (crafting.getStackInSlot(slot).getCount() >= stack.getCount()) {
                continue;
            }

            ThELog.debug("Failed to pull item from ae inv, trying player inventory");
            stack.shrink(crafting.getStackInSlot(slot).getCount());

            ItemStack invExtract = ItemHandlerUtil.extract(playerInv, stack, false);
            if (!invExtract.isEmpty()) {
                crafting.insertItem(slot, invExtract, false);
            }
        }
    }

    public void clearCraftingGrid() {
        if (this.isClientSide()) {
            this.sendClientAction(ACTION_CLEAR_GRID);
            return;
        }

        this.clearCrafting();
        this.onMatrixChanged();
    }

    public void setClearGridOnClose(boolean clearGridOnClose) {
        if (this.isClientSide()) {
            this.sendClientAction(ACTION_SET_CLEAR_ON_CLOSE, clearGridOnClose);
            return;
        }

        this.clearGridOnClose = clearGridOnClose;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        if (this.isServerSide() && this.clearGridOnClose) {
            this.clearCrafting();
            this.onMatrixChanged();
        }
        super.onContainerClosed(player);
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slotIn) {
        if (slotIn instanceof SlotArcaneResult) {
            return false;
        }
        return super.canMergeSlot(stack, slotIn);
    }

    protected void clearCrafting() {
        this.clearCraftingIntoNetwork();
    }

    private boolean clearCraftingIntoNetwork() {
        if (this.storage == null) {
            return false;
        }

        boolean clearSuccess = true;
        IItemHandler crafting = this.getInventory("crafting");
        for (int slot = 0; slot < crafting.getSlots(); slot++) {
            ItemStack stack = crafting.extractItem(slot, Integer.MAX_VALUE, true);
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack remainder = this.insertItem(this.storage, stack, stack.getCount(), Actionable.MODULATE);
            int inserted = stack.getCount() - remainder.getCount();
            if (inserted > 0) {
                crafting.extractItem(slot, inserted, false);
            }
            if (!crafting.getStackInSlot(slot).isEmpty()) {
                clearSuccess = false;
            }
        }
        return clearSuccess;
    }

    private boolean clearCraftingIntoNetworkForJEI() {
        if (this.storage == null) {
            return false;
        }

        List<NetworkReservation> reservations = new ArrayList<>();
        IItemHandler crafting = this.getInventory("crafting");
        for (int slot = 0; slot < crafting.getSlots(); slot++) {
            ItemStack stack = crafting.extractItem(slot, Integer.MAX_VALUE, true);
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack remainder = this.insertItem(this.storage, stack, stack.getCount(), Actionable.MODULATE);
            int inserted = stack.getCount() - remainder.getCount();
            if (inserted > 0) {
                ItemStack reserved = stack.copy();
                reserved.setCount(inserted);
                reservations.add(new NetworkReservation(slot, reserved));
            }
            if (inserted < stack.getCount()) {
                this.rollbackNetworkReservations(reservations);
                return false;
            }
        }

        for (NetworkReservation reservation : reservations) {
            crafting.extractItem(reservation.slot, reservation.stack.getCount(), false);
        }
        return true;
    }

    private void rollbackNetworkReservations(List<NetworkReservation> reservations) {
        for (int i = reservations.size() - 1; i >= 0; i--) {
            NetworkReservation reservation = reservations.get(i);
            this.extractItem(this.storage, reservation.stack, reservation.stack.getCount(), Actionable.MODULATE);
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected void addMatrixSlots(int offsetX, int offsetY) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new SlotArcaneMatrix(this, i * 3 + j, offsetX + (j * 18), offsetY + (i * 18)),
                        SlotSemantics.CRAFTING_GRID);
            }
        }

        offsetX += 104;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 2; j++) {
                this.addSlot(new SlotArcaneMatrix(this, 9 + (i * 2 + j), offsetX + (j * 18), offsetY + (i * 18)),
                        ThESlotSemantics.ARCANE_CRYSTAL);
            }
        }

        offsetX -= 104;
        this.addSlot(this.resultSlot = new SlotArcaneResult(this, this.getPlayer(), 0, offsetX + 84, offsetY + 18),
                SlotSemantics.CRAFTING_RESULT);
    }

    @SuppressWarnings("SameParameterValue")
    protected void addArmorSlots(EntityPlayer player, IItemHandler inventory, int offsetX, int offsetY) {
        this.addSlot(new SlotArmor(player, inventory, 3, offsetX, offsetY), ThESlotSemantics.PLAYER_ARMOR);
        this.addSlot(new SlotArmor(player, inventory, 2, offsetX, offsetY + 18), ThESlotSemantics.PLAYER_ARMOR);
        this.addSlot(new SlotArmor(player, inventory, 1, offsetX, offsetY + 18 * 2), ThESlotSemantics.PLAYER_ARMOR);
        this.addSlot(new SlotArmor(player, inventory, 0, offsetX, offsetY + 18 * 3), ThESlotSemantics.PLAYER_ARMOR);
    }

    @SuppressWarnings("SameParameterValue")
    protected void addUpgradeSlots(int offsetX, int offsetY) {
        this.addSlot(new SlotUpgrade(this.getInventory("upgrades"), 0, offsetX, offsetY), SlotSemantics.UPGRADE);
    }

    private static final class NetworkReservation {
        private final int slot;
        private final ItemStack stack;

        private NetworkReservation(int slot, ItemStack stack) {
            this.slot = slot;
            this.stack = stack;
        }
    }
}
