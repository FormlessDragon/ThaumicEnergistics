package thaumicenergistics.container.item;

import ae2.container.ISubGui;
import ae2.core.gui.locator.GuiHostLocator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.items.wrapper.InvWrapper;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.container.ActionType;
import thaumicenergistics.container.ContainerBase;
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.container.slot.SlotGhost;
import thaumicenergistics.core.ThEFeatures;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.network.packets.PacketUIAction;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.KnowledgeCoreUtil;
import thaumicenergistics.util.inventory.ThEInternalInventory;

import java.util.Optional;

/**
 * @author Alex811
 */
public class ContainerKnowledgeCore extends ContainerBase implements ISubGui {
    private static final int SLOT_NUM = 9;
    private final ModGUIs GUIAction;
    private final ContainerArcaneInscriber parentContainer;
    private final GuiHostLocator parentLocator;
    private final IArcaneTerminalHost parentHost;
    private ItemStack knowledgeCoreStack;
    private ThEInternalInventory inventory;

    public ContainerKnowledgeCore(EntityPlayer player, ModGUIs GUIAction, ContainerArcaneInscriber parent,
                                  GuiHostLocator parentLocator) {
        super(player);
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

    @Override
    public void onAction(EntityPlayerMP player, PacketUIAction packet) {
        if (ForgeUtil.isServer()) {
            this.handleServerAction(player, packet);
        }
    }

    void handleServerAction(EntityPlayer player, PacketUIAction packet) {
        validateKnowledgeCoreAction(packet.action);
        validateKnowledgeCoreIndex(packet.action, packet.index);
        if (packet.index > -1) {
            switch (packet.action) {
                case KNOWLEDGE_CORE_ADD -> {
                    this.playWriteSound(player);
                    ThEInternalInventory ingredients = (ThEInternalInventory) ((InvWrapper) parentContainer.getInventory("crafting")).getInv();
                    ItemStack result = parentContainer.getInventory("result").getStackInSlot(0);
                    KnowledgeCoreUtil.setRecipe(knowledgeCoreStack, packet.index, new KnowledgeCoreUtil.Recipe(ingredients, result, parentContainer.getCurrentRequiredVis()));
                }
                case KNOWLEDGE_CORE_DEL -> {
                    this.playWriteSound(player);
                    KnowledgeCoreUtil.setRecipe(knowledgeCoreStack, packet.index, null);
                }
                case KNOWLEDGE_CORE_VIEW -> {
                }
            }
        }
        if (KnowledgeCoreUtil.isEmpty(knowledgeCoreStack))
            Optional.of(ThEItems.BLANK_KNOWLEDGE_CORE.stack(1)).ifPresent(blank -> ((InvWrapper) parentContainer.getInventory("upgrades")).getInv().setInventorySlotContents(0, blank));
        parentHost.returnToMainContainer(player, this);
    }

    private static void validateKnowledgeCoreIndex(ActionType action, int index) {
        if (index == -1 && action == ActionType.KNOWLEDGE_CORE_VIEW) {
            return;
        }
        if (index < 0 || index >= SLOT_NUM) {
            throw new IllegalArgumentException("Invalid Knowledge Core recipe index " + index
                    + " for packet action " + action + "; expected 0-" + (SLOT_NUM - 1));
        }
    }

    private static void validateKnowledgeCoreAction(ActionType action) {
        if (action != ActionType.KNOWLEDGE_CORE_ADD
                && action != ActionType.KNOWLEDGE_CORE_DEL
                && action != ActionType.KNOWLEDGE_CORE_VIEW) {
            throw new IllegalArgumentException("Unsupported Knowledge Core packet action: " + action);
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
