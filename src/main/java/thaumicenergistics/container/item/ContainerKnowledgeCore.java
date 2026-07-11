package thaumicenergistics.container.item;

import ae2.container.AEBaseContainer;
import ae2.container.ISubGui;
import ae2.container.SlotSemantics;
import ae2.container.guisync.GuiSync;
import ae2.api.inventories.InternalInventory;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.container.slot.FakeSlot;
import ae2.container.slot.AppEngSlot;
import ae2.core.gui.locator.GuiHostLocator;
import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.util.inv.AppEngInternalInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.core.ThESounds;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.util.KnowledgeCoreUtil;

import java.util.Objects;

/**
 * @author Alex811
 */
public class ContainerKnowledgeCore extends AEBaseContainer implements ISubGui {
    public static final String ACTION_ADD_RECIPE = "knowledgeCoreAdd";
    public static final String ACTION_DELETE_RECIPE = "knowledgeCoreDelete";
    public static final String ACTION_VIEW_RECIPE = "knowledgeCoreView";
    private static final int RECIPE_SLOTS_PER_PAGE = KnowledgeCoreUtil.BASE_RECIPE_SLOTS;
    public static final String ACTION_PREVIOUS_PAGE = "knowledgeCorePreviousPage";
    public static final String ACTION_NEXT_PAGE = "knowledgeCoreNextPage";
    private final ModGUIs GUIAction;
    private final ContainerArcaneInscriber parentContainer;
    private final GuiHostLocator parentLocator;
    private final IArcaneTerminalHost parentHost;
    private final ItemStack knowledgeCoreStack;
    private final int managedInventorySlot;
    private AppEngInternalInventory inventory;
    private final IUpgradeInventory upgradeInventory;
    @GuiSync(10)
    private int page;

    public ContainerKnowledgeCore(EntityPlayer player, ModGUIs GUIAction, ContainerArcaneInscriber parent,
                                  GuiHostLocator parentLocator) {
        super(Objects.requireNonNull(player, "player").inventory, null);
        validateKnowledgeCoreGui(GUIAction);
        if (parent == null) {
            throw new IllegalArgumentException("parent cannot be null for Knowledge Core gui " + GUIAction
                + " player " + player);
        }
        if (parentLocator == null) {
            throw new IllegalArgumentException("parent locator cannot be null for Knowledge Core gui " + GUIAction
                + " player " + player + " parent " + parent);
        }
        IArcaneTerminalHost parentHost = parent.getHost();
        if (parentHost == null) {
            throw new IllegalArgumentException("parent host cannot be null for Knowledge Core gui " + GUIAction
                + " player " + player + " locator " + parentLocator + " parent " + parent);
        }
        IArcaneTerminalHost locatedHost = parentLocator.locate(player, IArcaneTerminalHost.class);
        if (locatedHost != parentHost) {
            throw new IllegalStateException("Knowledge Core parent locator mismatch for gui " + GUIAction
                + " player " + player
                + " locator " + parentLocator
                + " parent " + parent
                + " parentHost " + parentHost
                + " locatedHost " + locatedHost);
        }

        this.GUIAction = GUIAction;
        this.parentContainer = parent;
        this.parentLocator = parentLocator;
        this.parentHost = parentHost;
        this.knowledgeCoreStack = this.parentHost.getArcaneUpgradeInventory().getStackInSlot(0);
        this.managedInventorySlot = -1;
        this.upgradeInventory = KnowledgeCoreUtil.getUpgradeInventory(this.knowledgeCoreStack);
        this.initializeSharedSlots();
        registerClientAction(ACTION_ADD_RECIPE, Integer.class, this::handleAddRecipe);
        registerClientAction(ACTION_DELETE_RECIPE, Integer.class, this::handleDeleteRecipe);
        registerClientAction(ACTION_VIEW_RECIPE, Integer.class, this::handleViewRecipe);
    }

    /**
     * Opens a Knowledge Core stored in the player's inventory. This route deliberately has no Arcane Inscriber parent:
     * it is read-only for recipes but keeps the core's pattern expansion cards manageable.
     */
    public ContainerKnowledgeCore(EntityPlayer player, GuiHostLocator locator) {
        super(Objects.requireNonNull(player, "player").inventory, null);
        this.GUIAction = ModGUIs.KNOWLEDGE_CORE_MANAGE;
        this.parentContainer = null;
        this.parentLocator = null;
        this.parentHost = null;
        this.managedInventorySlot = getManagedInventorySlot(player, locator);
        this.knowledgeCoreStack = player.inventory.getStackInSlot(this.managedInventorySlot);
        validateKnowledgeCoreStack(this.knowledgeCoreStack, "player inventory slot " + this.managedInventorySlot);
        this.lockPlayerInventorySlot(this.managedInventorySlot);
        this.upgradeInventory = KnowledgeCoreUtil.getUpgradeInventory(this.knowledgeCoreStack);
        this.initializeSharedSlots();
    }

    private void initializeSharedSlots() {
        initInv();
        addSlots(8, 15);
        this.addPlayerInventorySlots(8, 94);
        registerClientAction(ACTION_PREVIOUS_PAGE, Integer.class, ignored -> this.changePage(-1));
        registerClientAction(ACTION_NEXT_PAGE, Integer.class, ignored -> this.changePage(1));
    }

    private static void validateKnowledgeCoreGui(ModGUIs guiAction) {
        if (guiAction != ModGUIs.KNOWLEDGE_CORE_ADD
            && guiAction != ModGUIs.KNOWLEDGE_CORE_DEL
            && guiAction != ModGUIs.KNOWLEDGE_CORE_VIEW) {
            throw new IllegalArgumentException("Unsupported Knowledge Core gui action: " + guiAction);
        }
    }

    private static int getManagedInventorySlot(EntityPlayer player, GuiHostLocator locator) {
        if (!(locator instanceof ItemGuiHostLocator itemLocator)) {
            throw new IllegalArgumentException("Knowledge Core inventory management requires an item locator, got "
                + locator);
        }
        Integer slot = itemLocator.getPlayerInventorySlot();
        if (slot == null || slot < 0 || slot >= player.inventory.getSizeInventory()) {
            throw new IllegalArgumentException("Knowledge Core inventory management received invalid player slot "
                + slot + " from locator " + locator);
        }
        return slot;
    }

    private static void validateKnowledgeCoreStack(ItemStack stack, String location) {
        if (stack.isEmpty() || !(stack.getItem() instanceof thaumicenergistics.items.ItemKnowledgeCore)) {
            throw new IllegalArgumentException("Expected a Knowledge Core in " + location + ", got " + stack);
        }
    }

    private void initInv() {
        inventory = new AppEngInternalInventory(RECIPE_SLOTS_PER_PAGE);
        for (int slot = 0; slot < RECIPE_SLOTS_PER_PAGE; slot++) {
            inventory.setMaxStackSize(slot, 1);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void addSlots(int offsetX, int offsetY) {
        for (int i = 0; i < RECIPE_SLOTS_PER_PAGE; i++) {
            FakeSlot slotGhost = new RecipeDisplaySlot(inventory, i, offsetX + i * 18, offsetY);
            this.addSlot(slotGhost, ThESlotSemantics.KNOWLEDGE_CORE);
        }
        for (int slot = 0; slot < this.upgradeInventory.size(); slot++) {
            this.addSlot(new ExpansionCardSlot(this.upgradeInventory, slot, 0, 0), SlotSemantics.UPGRADE);
        }
        this.refreshRecipeSlots();
    }

    public void requestAddRecipe(int slotId) {
        this.sendClientAction(ACTION_ADD_RECIPE, slotId);
    }

    public void requestDeleteRecipe(int slotId) {
        this.sendClientAction(ACTION_DELETE_RECIPE, slotId);
    }

    public void requestViewRecipe(int slotId) {
        this.sendClientAction(ACTION_VIEW_RECIPE, slotId);
    }

    public void requestPreviousPage() {
        this.sendClientAction(ACTION_PREVIOUS_PAGE, 0);
    }

    public void requestNextPage() {
        this.sendClientAction(ACTION_NEXT_PAGE, 0);
    }

    private void handleAddRecipe(int slotId) {
        this.handleClientAction(ACTION_ADD_RECIPE, slotId);
    }

    private void handleDeleteRecipe(int slotId) {
        this.handleClientAction(ACTION_DELETE_RECIPE, slotId);
    }

    private void handleViewRecipe(int slotId) {
        this.handleClientAction(ACTION_VIEW_RECIPE, slotId);
    }

    private void handleClientAction(String actionName, int slotId) {
        if (slotId >= 0) {
            slotId += this.page * RECIPE_SLOTS_PER_PAGE;
        }
        validateKnowledgeCoreIndex(actionName, slotId);
        EntityPlayer player = this.getPlayer();
        if (slotId > -1) {
            switch (actionName) {
                case ACTION_ADD_RECIPE -> {
                    this.playWriteSound(player);
                    AppEngInternalInventory ingredients = this.copyIngredients(parentContainer.getCraftingInventory());
                    ItemStack result = parentContainer.getCraftingResultInventory().getStackInSlot(0);
                    KnowledgeCoreUtil.setRecipe(knowledgeCoreStack, slotId, new KnowledgeCoreUtil.Recipe(ingredients, result, parentContainer.getCurrentRequiredVis()));
                }
                case ACTION_DELETE_RECIPE -> {
                    this.playWriteSound(player);
                    KnowledgeCoreUtil.setRecipe(knowledgeCoreStack, slotId, null);
                }
                case ACTION_VIEW_RECIPE -> {
                }
                default ->
                    throw new IllegalArgumentException("Unsupported Knowledge Core client action: " + actionName);
            }
        }
        if (KnowledgeCoreUtil.isEmpty(knowledgeCoreStack)) {
            parentHost.getArcaneUpgradeInventory().setItemDirect(0, ThEItems.BLANK_KNOWLEDGE_CORE.stack(1));
        }
        parentHost.returnToMainContainer(player, this);
    }

    @Override
    public void detectAndSendChanges() {
        if (this.isServerSide()) {
            this.page = Math.min(this.page, this.getPageCount() - 1);
            this.refreshRecipeSlots();
        }
        super.detectAndSendChanges();
    }

    private AppEngInternalInventory copyIngredients(InternalInventory source) {
        AppEngInternalInventory copy = new AppEngInternalInventory(source.size());
        for (int slot = 0; slot < source.size(); slot++) {
            copy.insertItem(slot, source.getStackInSlot(slot), false);
        }
        return copy;
    }

    @Override
    public ItemStack slotClick(int slotID, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotID >= 0 && slotID < this.inventorySlots.size()) {
            Slot slot = this.getSlot(slotID);
            if (slotID < RECIPE_SLOTS_PER_PAGE && this.getSlotSemantic(slot) == ThESlotSemantics.KNOWLEDGE_CORE) {
                return ItemStack.EMPTY;
            }
        }
        return super.slotClick(slotID, dragType, clickType, player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.managedInventorySlot < 0
            || player.inventory.getStackInSlot(this.managedInventorySlot) == this.knowledgeCoreStack;
    }

    private void validateKnowledgeCoreIndex(String actionName, int index) {
        if (index == -1 && ACTION_VIEW_RECIPE.equals(actionName)) {
            return;
        }
        if (index < 0 || index >= KnowledgeCoreUtil.getRecipeSlotCount(knowledgeCoreStack)) {
            throw new IllegalArgumentException("Invalid Knowledge Core recipe index " + index
                + " for client action " + actionName + "; expected 0-"
                + (KnowledgeCoreUtil.getRecipeSlotCount(knowledgeCoreStack) - 1));
        }
    }

    private void changePage(int delta) {
        int newPage = this.page + delta;
        if (newPage < 0 || newPage >= this.getPageCount()) {
            return;
        }
        this.page = newPage;
        this.refreshRecipeSlots();
    }

    public int getPageCount() {
        return Math.max(1, (KnowledgeCoreUtil.getRecipeSlotCount(this.knowledgeCoreStack)
            + RECIPE_SLOTS_PER_PAGE - 1) / RECIPE_SLOTS_PER_PAGE);
    }

    public boolean hasPreviousPage() {
        return this.page > 0;
    }

    public boolean hasNextPage() {
        return this.page + 1 < this.getPageCount();
    }

    public int getPage() {
        return this.page;
    }

    /**
     * Returns the native AE2 upgrade inventory displayed by the Knowledge Core management screen.
     */
    public IUpgradeInventory getUpgradeInventory() {
        return this.upgradeInventory;
    }

    private void refreshRecipeSlots() {
        int firstSlot = this.page * RECIPE_SLOTS_PER_PAGE;
        for (int i = 0; i < RECIPE_SLOTS_PER_PAGE; i++) {
            int recipeSlot = firstSlot + i;
            KnowledgeCoreUtil.Recipe recipe = recipeSlot < KnowledgeCoreUtil.getRecipeSlotCount(this.knowledgeCoreStack)
                ? KnowledgeCoreUtil.getRecipe(this.knowledgeCoreStack, recipeSlot) : null;
            this.inventory.setItemDirect(i, ItemStack.EMPTY);
            if (recipe != null) {
                this.inventory.insertItem(i, recipe.result(), false);
            }
        }
    }

    private boolean canRemoveExpansionCard() {
        int installed = KnowledgeCoreUtil.getInstalledExpansionCards(this.knowledgeCoreStack);
        if (installed <= 0) {
            return false;
        }
        int firstDisabledSlot = KnowledgeCoreUtil.BASE_RECIPE_SLOTS
            + (installed - 1) * KnowledgeCoreUtil.RECIPE_SLOTS_PER_EXPANSION_CARD;
        for (int slot = firstDisabledSlot; slot < KnowledgeCoreUtil.getRecipeSlotCount(this.knowledgeCoreStack); slot++) {
            if (KnowledgeCoreUtil.hasRecipe(this.knowledgeCoreStack, slot)) {
                return false;
            }
        }
        return true;
    }

    private final class ExpansionCardSlot extends AppEngSlot {

        private ExpansionCardSlot(IUpgradeInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canTakeStack(EntityPlayer playerIn) {
            return ContainerKnowledgeCore.this.canRemoveExpansionCard();
        }
    }

    private static final class RecipeDisplaySlot extends FakeSlot {

        private RecipeDisplaySlot(InternalInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public void putStack(ItemStack stack) {
            if (!stack.isEmpty()) {
                stack = stack.copy();
                stack.setCount(1);
            }
            super.putStack(stack);
        }
    }

    public void playWriteSound(EntityPlayer player) {
        if (this.parentContainer == null) {
            throw new IllegalStateException("Knowledge Core inventory management does not support recipe write sounds");
        }
        player.world.playSound(player, parentContainer.getPartPos(), new SoundEvent(ThESounds.instance().knowledgeCoreWrite()), SoundCategory.BLOCKS, 1, 1);
    }

    public ModGUIs getGUIAction() {
        return GUIAction;
    }

    @Override
    public GuiHostLocator getLocator() {
        if (this.parentLocator == null) {
            throw new IllegalStateException("Knowledge Core inventory management does not have a parent GUI locator");
        }
        return this.parentLocator;
    }

    @Override
    public IArcaneTerminalHost getHost() {
        if (this.parentHost == null) {
            throw new IllegalStateException("Knowledge Core inventory management does not have a parent GUI host");
        }
        return this.parentHost;
    }
}
