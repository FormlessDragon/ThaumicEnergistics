package thaumicenergistics.container.item;

import ae2.container.AEBaseContainer;
import ae2.container.ISubGui;
import ae2.core.gui.locator.GuiHostLocator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.items.wrapper.InvWrapper;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.container.slot.SlotGhost;
import thaumicenergistics.core.ThEFeatures;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.util.KnowledgeCoreUtil;
import thaumicenergistics.util.inventory.ThEInternalInventory;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Alex811
 */
public class ContainerKnowledgeCore extends AEBaseContainer implements ISubGui {
    public static final String ACTION_ADD_RECIPE = "knowledgeCoreAdd";
    public static final String ACTION_DELETE_RECIPE = "knowledgeCoreDelete";
    public static final String ACTION_VIEW_RECIPE = "knowledgeCoreView";
    private static final int SLOT_NUM = 9;
    private final ModGUIs GUIAction;
    private final ContainerArcaneInscriber parentContainer;
    private final GuiHostLocator parentLocator;
    private final IArcaneTerminalHost parentHost;
    private ItemStack knowledgeCoreStack;
    private ThEInternalInventory inventory;

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
        this.knowledgeCoreStack = this.parentContainer.getInventory("upgrades").getStackInSlot(0);
        initInv();
        addSlots(8, 15);
        registerClientAction(ACTION_ADD_RECIPE, Integer.class, this::handleAddRecipe);
        registerClientAction(ACTION_DELETE_RECIPE, Integer.class, this::handleDeleteRecipe);
        registerClientAction(ACTION_VIEW_RECIPE, Integer.class, this::handleViewRecipe);
    }

    private static void validateKnowledgeCoreGui(ModGUIs guiAction) {
        if (guiAction != ModGUIs.KNOWLEDGE_CORE_ADD
                && guiAction != ModGUIs.KNOWLEDGE_CORE_DEL
                && guiAction != ModGUIs.KNOWLEDGE_CORE_VIEW) {
            throw new IllegalArgumentException("Unsupported Knowledge Core gui action: " + guiAction);
        }
    }

    private void initInv() {
        inventory = new ThEInternalInventory("KCore", 9, 1);
    }

    @SuppressWarnings("SameParameterValue")
    private void addSlots(int offsetX, int offsetY) {
        for (int i = 0; i < SLOT_NUM; i++) {
            SlotGhost slotGhost = new SlotGhost(inventory, i, offsetX + (i * 18), offsetY);
            if (KnowledgeCoreUtil.hasRecipe(knowledgeCoreStack, i)) {
                KnowledgeCoreUtil.Recipe recipe = KnowledgeCoreUtil.getRecipe(knowledgeCoreStack, i);
                if (recipe != null) slotGhost.putStack(recipe.result());
            }
            this.addSlot(slotGhost, ThESlotSemantics.KNOWLEDGE_CORE);
        }
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
        validateKnowledgeCoreIndex(actionName, slotId);
        EntityPlayer player = this.getPlayer();
        if (slotId > -1) {
            switch (actionName) {
                case ACTION_ADD_RECIPE -> {
                    this.playWriteSound(player);
                    ThEInternalInventory ingredients = (ThEInternalInventory) ((InvWrapper) parentContainer.getInventory("crafting")).getInv();
                    ItemStack result = parentContainer.getInventory("result").getStackInSlot(0);
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
        if (KnowledgeCoreUtil.isEmpty(knowledgeCoreStack))
            Optional.of(ThEItems.BLANK_KNOWLEDGE_CORE.stack(1)).ifPresent(blank -> ((InvWrapper) parentContainer.getInventory("upgrades")).getInv().setInventorySlotContents(0, blank));
        parentHost.returnToMainContainer(player, this);
    }

    @Override
    public ItemStack slotClick(int slotID, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotID >= 0 && slotID < this.inventorySlots.size()) {
            Slot slot = this.getSlot(slotID);
            if (this.getSlotSemantic(slot) == ThESlotSemantics.KNOWLEDGE_CORE) {
                return ItemStack.EMPTY;
            }
        }
        return super.slotClick(slotID, dragType, clickType, player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    private static void validateKnowledgeCoreIndex(String actionName, int index) {
        if (index == -1 && ACTION_VIEW_RECIPE.equals(actionName)) {
            return;
        }
        if (index < 0 || index >= SLOT_NUM) {
            throw new IllegalArgumentException("Invalid Knowledge Core recipe index " + index
                    + " for client action " + actionName + "; expected 0-" + (SLOT_NUM - 1));
        }
    }

    public void playWriteSound(EntityPlayer player) {
        player.world.playSound(player, parentContainer.getPartPos(), new SoundEvent(ThEFeatures.instance().sounds().knowledgeCoreWrite()), SoundCategory.BLOCKS, 1, 1);
    }

    public ModGUIs getGUIAction() {
        return GUIAction;
    }

    @Override
    public GuiHostLocator getLocator() {
        return this.parentLocator;
    }

    @Override
    public IArcaneTerminalHost getHost() {
        return this.parentHost;
    }
}
