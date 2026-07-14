package thaumicenergistics.container.block;

import ae2.container.guisync.GuiSync;
import ae2.container.implementations.UpgradeableContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.slot.SlotKnowledgeCore;
import thaumicenergistics.core.ThESounds;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.core.ThELog;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Alex811
 */
public class ContainerArcaneAssembler extends UpgradeableContainer<TileArcaneAssembler> {

    @GuiSync(20)
    private ArcaneAssemblerGuiState guiState = ArcaneAssemblerGuiState.EMPTY;

    public ContainerArcaneAssembler(InventoryPlayer ip, TileArcaneAssembler host) {
        super(ip, host);

        this.addSlot(
            new SlotKnowledgeCore(this.getHost().getCoreInventory(), 0, 81, 66),
            ThESlotSemantics.KNOWLEDGE_CORE);
        this.addListener(new KnowledgeCoreSlotListener());
        this.refreshGuiState();
    }

    public ArcaneAssemblerGuiState getGuiState() {
        return this.guiState;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public void detectAndSendChanges() {
        if (this.isServerSide()) {
            this.refreshGuiState();
        }
        super.detectAndSendChanges();
    }

    private void refreshGuiState() {
        this.guiState = ArcaneAssemblerGuiState.from(this.getHost());
    }

    public void playCoreSound(EntityPlayer player) { // plays the right sound, when the Knowledge Core gets removed or placed in the slot
        ResourceLocation sound = this.getHost().getCoreInventory().getStackInSlot(0).isEmpty()
                ? ThESounds.instance().knowledgeCorePowerDown()
                : ThESounds.instance().knowledgeCorePowerUp();
        SoundEvent soundEvent = this.resolveCoreSound(sound);
        player.world.playSound(null, getHost().getPos(), soundEvent, SoundCategory.BLOCKS, 1, 1);
    }

    protected SoundEvent resolveCoreSound(ResourceLocation sound) {
        SoundEvent soundEvent = SoundEvent.REGISTRY.getObject(sound);
        if (soundEvent == null) {
            ThELog.error("Arcane Assembler core sound is not registered: {}", sound);
            throw new IllegalStateException("Arcane Assembler core sound is not registered: " + sound);
        }
        return soundEvent;
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

    private class KnowledgeCoreSlotListener implements IContainerListener {
        private boolean opened = false;

        @Override
        @ParametersAreNonnullByDefault
        public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
            if (slotInd == 0 && opened && ForgeUtil.isServer()) {
                ContainerArcaneAssembler.this.playCoreSound(ContainerArcaneAssembler.this.getPlayer());
                ContainerArcaneAssembler.this.getHost().init();
            }
            opened = true;
        }

        // ignored //
        @Override
        @ParametersAreNonnullByDefault
        public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
        }

        @Override
        @ParametersAreNonnullByDefault
        public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {
        }

        @Override
        @ParametersAreNonnullByDefault
        public void sendAllWindowProperties(Container containerIn, IInventory inventory) {
        }
    }

}
