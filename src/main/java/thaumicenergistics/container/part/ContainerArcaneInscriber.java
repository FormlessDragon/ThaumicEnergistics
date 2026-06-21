package thaumicenergistics.container.part;

import ae2.api.stacks.AEItemKey;
import ae2.api.config.Actionable;
import ae2.api.util.IConfigurableObject;
import ae2.container.GuiIds;
import ae2.container.SlotSemantics;
import ae2.core.network.serverbound.GuiActionPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.common.gui.ThEGuiOpener;
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.slot.SlotArcaneGhostMatrix;
import thaumicenergistics.container.slot.SlotArcaneResult;
import thaumicenergistics.container.slot.SlotKnowledgeCore;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.integration.jei.ArcaneInscriberGhostItemPayload;
import thaumicenergistics.integration.thaumcraft.TCCraftingManager;
import thaumicenergistics.items.ItemKnowledgeCore;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketIsArcaneUpdate;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.ItemHandlerUtil;
import thaumicenergistics.util.KnowledgeCoreUtil;
import thaumicenergistics.util.ThELog;

import java.util.Optional;

/**
 * @author Alex811
 */
public class ContainerArcaneInscriber extends ContainerArcaneTerm implements IConfigurableObject {

    private static final String ACTION_KNOWLEDGE_CORE_ADD = "knowledgeCoreAdd";
    private static final String ACTION_KNOWLEDGE_CORE_DEL = "knowledgeCoreDel";
    private static final String ACTION_KNOWLEDGE_CORE_VIEW = "knowledgeCoreView";
    private static final String ACTION_GHOST_ITEM_MOVE = "moveGhostItem";
    private static final int ACTION_GHOST_ITEM_MOVE_MAX_LENGTH = GuiActionPacket.MAX_JSON_PAYLOAD_LENGTH;

    public boolean recipeIsArcane = false;

    public ContainerArcaneInscriber(InventoryPlayer ip, IArcaneTerminalHost host) {
        super(GuiIds.GuiKey.ME_STORAGE_TERMINAL, ip, host);
        this.registerInscriberActions();
    }

    private void registerInscriberActions() {
        this.registerClientAction(ACTION_KNOWLEDGE_CORE_ADD, this::requestKnowledgeCoreAdd);
        this.registerClientAction(ACTION_KNOWLEDGE_CORE_DEL, this::requestKnowledgeCoreDel);
        this.registerClientAction(ACTION_KNOWLEDGE_CORE_VIEW, this::requestKnowledgeCoreView);
        this.registerClientAction(ACTION_GHOST_ITEM_MOVE, ArcaneInscriberGhostItemPayload.class,
                ACTION_GHOST_ITEM_MOVE_MAX_LENGTH, this::receiveGhostItemMove);
    }

    public void requestKnowledgeCoreAdd() {
        if (this.isClientSide()) {
            this.sendClientAction(ACTION_KNOWLEDGE_CORE_ADD);
            return;
        }

        this.openKnowledgeCoreAdd();
    }

    public void requestKnowledgeCoreDel() {
        if (this.isClientSide()) {
            this.sendClientAction(ACTION_KNOWLEDGE_CORE_DEL);
            return;
        }

        this.openKnowledgeCoreDel();
    }

    public void requestKnowledgeCoreView() {
        if (this.isClientSide()) {
            this.sendClientAction(ACTION_KNOWLEDGE_CORE_VIEW);
            return;
        }

        this.openKnowledgeCoreView();
    }

    private void openKnowledgeCoreAdd() {
        ItemStack knowledgeCore = this.getInventory("upgrades").getStackInSlot(0);
        ItemStack result = this.getInventory("result").getStackInSlot(0);
        if (knowledgeCore.isEmpty() || result.isEmpty() || !this.recipeIsArcane) {
            return;
        }

        if (!(knowledgeCore.getItem() instanceof ItemKnowledgeCore)) {
            return;
        }

        boolean currentIsBlank = ((ItemKnowledgeCore) knowledgeCore.getItem()).isBlank();
        if (currentIsBlank) {
            Optional.of(ThEItems.KNOWLEDGE_CORE.stack(1)).ifPresent(newCore ->
                    ((InvWrapper) this.getInventory("upgrades")).getInv().setInventorySlotContents(0, newCore));
        } else if (KnowledgeCoreUtil.hasRecipe(knowledgeCore, result.getItem())) {
            return;
        }

        ThEGuiOpener.openLocatorGui(this.getPlayer(), ModGUIs.KNOWLEDGE_CORE_ADD, this.getLocator(), false);
    }

    private void openKnowledgeCoreDel() {
        ItemStack knowledgeCore = this.getInventory("upgrades").getStackInSlot(0);
        if (this.isNonBlankKnowledgeCore(knowledgeCore)) {
            ThEGuiOpener.openLocatorGui(this.getPlayer(), ModGUIs.KNOWLEDGE_CORE_DEL, this.getLocator(), false);
        }
    }

    private void openKnowledgeCoreView() {
        ItemStack knowledgeCore = this.getInventory("upgrades").getStackInSlot(0);
        if (this.isNonBlankKnowledgeCore(knowledgeCore)) {
            ThEGuiOpener.openLocatorGui(this.getPlayer(), ModGUIs.KNOWLEDGE_CORE_VIEW, this.getLocator(), false);
        }
    }

    private boolean isNonBlankKnowledgeCore(ItemStack knowledgeCore) {
        return !knowledgeCore.isEmpty()
                && knowledgeCore.getItem() instanceof ItemKnowledgeCore
                && !((ItemKnowledgeCore) knowledgeCore.getItem()).isBlank();
    }

    public void requestMoveGhostItem(int slotNumber, ItemStack stack) {
        ArcaneInscriberGhostItemPayload payload = ArcaneInscriberGhostItemPayload.fromStack(slotNumber, stack);
        if (this.isClientSide()) {
            this.sendClientAction(ACTION_GHOST_ITEM_MOVE, payload);
            return;
        }

        this.receiveGhostItemMove(payload);
    }

    private void receiveGhostItemMove(ArcaneInscriberGhostItemPayload payload) {
        if (payload == null) {
            throw this.rejectGhostItemMove("payload is missing");
        }

        ItemStack stack;
        try {
            stack = payload.toValidatedStack();
        } catch (IllegalArgumentException e) {
            ThELog.warn("Rejecting invalid Arcane Inscriber ghost item payload: {}", e.getMessage());
            throw e;
        }

        int slotNumber = payload.slotNumber;
        if (slotNumber < 0 || slotNumber >= this.inventorySlots.size()) {
            throw this.rejectGhostItemMove("slot " + slotNumber + " is outside container range 0.."
                    + (this.inventorySlots.size() - 1));
        }

        Slot slot = this.getSlot(slotNumber);
        if (!(slot instanceof SlotArcaneGhostMatrix ghostSlot)) {
            throw this.rejectGhostItemMove("slot " + slotNumber + " is not an Arcane Inscriber ghost matrix slot");
        }
        if (ghostSlot.getSlotIndex() >= 9) {
            throw this.rejectGhostItemMove("slot " + slotNumber + " is an arcane crystal ghost slot");
        }

        AEItemKey key = AEItemKey.of(stack);
        if (key == null) {
            throw this.rejectGhostItemMove("stack " + stack + " cannot be represented as an AE item key");
        }
        slot.putStack(key.wrapForDisplayOrFilter());
    }

    private IllegalArgumentException rejectGhostItemMove(String message) {
        ThELog.warn("Rejecting Arcane Inscriber ghost item move: {}", message);
        return new IllegalArgumentException("Invalid Arcane Inscriber ghost item move: " + message);
    }

    public void refreshIsArcane() {
        if (ForgeUtil.isClient()) return;
        boolean recipeIsArcane;
        InvWrapper crafting = (InvWrapper) this.getInventory("crafting");
        if (this.recipe != null && !crafting.getInv().isEmpty())
            recipeIsArcane = (TCCraftingManager.findArcaneRecipe(crafting, this.getPlayer()) != null);
        else recipeIsArcane = false;
        if (this.recipeIsArcane != recipeIsArcane) {
            this.recipeIsArcane = recipeIsArcane;
            PacketHandler.sendToPlayer((EntityPlayerMP) this.getPlayer(), new PacketIsArcaneUpdate(recipeIsArcane));
        }
    }

    @Override
    public void onMatrixChanged() {
        super.onMatrixChanged();
        refreshIsArcane();
    }

    @Override
    protected void clearCrafting() {
        IItemHandler crafting = this.getInventory("crafting");
        for (int slot = 0; slot < crafting.getSlots(); slot++)
            crafting.extractItem(slot, crafting.getStackInSlot(slot).getCount(), false);
    }

    @Override
    protected float getRequiredVis(IRecipe recipe, EntityPlayer player) {
        if (!(recipe instanceof IArcaneRecipe))
            return -1;
        return ((IArcaneRecipe) recipe).getVis();
    }

    @Override
    public void handleJEITransfer(EntityPlayer player, NBTTagCompound tag) {
        NBTBase normal = tag.getTag("normal");
        NBTBase crystals = tag.getTag("crystal");

        clearCrafting();
        this.onMatrixChanged();

        handleJEITag(0, normal, true);
        handleJEITag(9, crystals, false);

        this.onMatrixChanged();
    }

    private void handleJEITag(int startAtSlot, NBTBase ingredientGroup, boolean mustBeSingle) {
        IItemHandler crafting = this.getInventory("crafting");
        IItemHandler playerInv = this.getInventory("player");

        if (ingredientGroup == null || ingredientGroup.isEmpty()) {
            return;
        }

        if (!(ingredientGroup instanceof NBTTagList subs)) {
            ThELog.warn("Invalid JEI ingredient group: {}", ingredientGroup);
            return;
        }

        for (int i = 0; i < subs.tagCount(); i++) {
            int slot = startAtSlot + i;
            NBTBase sub = subs.get(i);
            if (!(sub instanceof NBTTagList alternatives)) {
                ThELog.warn("Invalid JEI ingredient entry: {}", sub);
                continue;
            }

            if (alternatives.tagCount() <= 0) {
                continue;
            }

            NBTTagCompound ingredient = alternatives.getCompoundTagAt(0);
            ItemStack stack = new ItemStack(ingredient);
            if (stack.isEmpty()) continue;

            ThELog.debug("Adding {} for {}", stack.getDisplayName(), slot);
            ItemStack aeExtractStack = this.storage == null
                    ? ItemStack.EMPTY
                    : this.extractItem(this.storage, stack, stack.getCount(), Actionable.SIMULATE);
            if (!aeExtractStack.isEmpty()) {
                if (mustBeSingle) aeExtractStack.setCount(1);
                crafting.insertItem(slot, aeExtractStack, false);
            }

            if (!crafting.getStackInSlot(slot).isEmpty()) // We managed to pull everything from the system
                continue;

            // Try pull from player
            ThELog.debug("Failed to pull item from ae inv, trying player inventory");

            ItemStack invExtract = ItemHandlerUtil.extract(playerInv, stack, true);
            if (!invExtract.isEmpty()) {
                if (mustBeSingle) invExtract.setCount(1);
                crafting.insertItem(slot, invExtract, false);
            }

            // If we fail to find from ae2 or inv, just make the best guess from JEI
            crafting.insertItem(slot, stack, false);
        }
    }

    @Override
    public ItemStack onCraft(ItemStack toCraft) {
        return ItemStack.EMPTY;
    }

    @Override
    public int tryCraft(int amount) {
        return 0;
    }

    @SuppressWarnings("SameParameterValue")
    @Override
    protected void addMatrixSlots(int offsetX, int offsetY) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.addSlot(new SlotArcaneGhostMatrix(this, i * 3 + j, offsetX + (j * 18), offsetY + (i * 18)),
                        SlotSemantics.CRAFTING_GRID);
            }
        }
        offsetX += 104;
        for (int i = 0; i < 3; i++) { // Y
            for (int j = 0; j < 2; j++) { // X
                this.addSlot(new SlotArcaneGhostMatrix(this, 9 + (i * 2 + j), offsetX + (j * 18), offsetY + (i * 18)),
                        ThESlotSemantics.ARCANE_CRYSTAL);
            }
        }
        offsetX -= 104;
        this.addSlot(this.resultSlot = new SlotArcaneResult(this, this.getPlayer(), 0, offsetX + 84, offsetY + 18),
                SlotSemantics.CRAFTING_RESULT);
    }

    @Override
    protected void addUpgradeSlots(int offsetX, int offsetY) {
        this.addSlot(new SlotKnowledgeCore(this.getInventory("upgrades"), 0, offsetX, offsetY), SlotSemantics.UPGRADE);
    }

    @Override
    protected float getWorldVis() {
        return Float.MAX_VALUE;
    }

    @Override
    protected float getDiscount(EntityPlayer player) {
        return 0;
    }
}
