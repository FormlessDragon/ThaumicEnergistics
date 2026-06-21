package thaumicenergistics.container.block;

import ae2.container.SlotSemantics;
import ae2.container.guisync.GuiSync;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import thaumicenergistics.container.ContainerBase;
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.slot.SlotKnowledgeCore;
import thaumicenergistics.container.slot.SlotUpgrade;
import thaumicenergistics.core.ThEFeatures;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.ThELog;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Alex811
 */
public class ContainerArcaneAssembler extends ContainerBase {
    protected TileArcaneAssembler TE;
    @GuiSync(20)
    private ArcaneAssemblerGuiState guiState = ArcaneAssemblerGuiState.EMPTY;

    public ContainerArcaneAssembler(EntityPlayer player, TileArcaneAssembler TE) {
        super(player);
        this.TE = TE;
        this.addSlot(new SlotKnowledgeCore(this.getInventory("cores"), 0, 81, 66), ThESlotSemantics.KNOWLEDGE_CORE);
        for (int i = 0; i < this.getInventory("upgrades").getSlots(); i++)
            this.addSlot(new SlotUpgrade(this.getInventory("upgrades"), i, 186, 8 + i * 18), SlotSemantics.UPGRADE);
        this.bindPlayerInventory(new PlayerMainInvWrapper(player.inventory), 0, 147);
        this.addListener(new KnowledgeCoreSlotListener());
        this.refreshGuiState();
    }

    public IItemHandler getInventory(String name) {
        return this.TE.getInventoryByName(name);
    }

    public TileArcaneAssembler getTE() {
        return TE;
    }

    public ArcaneAssemblerGuiState getGuiState() {
        return this.guiState;
    }

    @Override
    public void detectAndSendChanges() {
        if (this.isServerSide()) {
            this.refreshGuiState();
        }
        super.detectAndSendChanges();
    }

    private void refreshGuiState() {
        this.guiState = ArcaneAssemblerGuiState.from(this.TE);
    }

    public void playCoreSound(EntityPlayer player) { // plays the right sound, when the Knowledge Core gets removed or placed in the slot
        ResourceLocation sound = this.getInventory("cores").getStackInSlot(0).isEmpty()
                ? ThEFeatures.instance().sounds().knowledgeCorePowerDown()
                : ThEFeatures.instance().sounds().knowledgeCorePowerUp();
        SoundEvent soundEvent = this.resolveCoreSound(sound);
        player.world.playSound(null, TE.getPos(), soundEvent, SoundCategory.BLOCKS, 1, 1);
    }

    protected SoundEvent resolveCoreSound(ResourceLocation sound) {
        SoundEvent soundEvent = SoundEvent.REGISTRY.getObject(sound);
        if (soundEvent == null) {
            ThELog.error("Arcane Assembler core sound is not registered: {}", sound);
            throw new IllegalStateException("Arcane Assembler core sound is not registered: " + sound);
        }
        return soundEvent;
    }

    private class KnowledgeCoreSlotListener implements IContainerListener {
        private boolean opened = false;

        @Override
        @ParametersAreNonnullByDefault
        public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
            if (slotInd == 0 && opened && ForgeUtil.isServer()) {
                ContainerArcaneAssembler.this.playCoreSound(ContainerArcaneAssembler.this.player);
                ContainerArcaneAssembler.this.TE.init();
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
