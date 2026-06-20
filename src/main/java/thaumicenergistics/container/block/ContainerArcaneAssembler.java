package thaumicenergistics.container.block;

import ae2.container.SlotSemantics;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import thaumicenergistics.container.ContainerBase;
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.slot.SlotKnowledgeCore;
import thaumicenergistics.container.slot.SlotUpgrade;
import thaumicenergistics.core.ThEFeatures;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketPlaySound;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.util.ForgeUtil;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Alex811
 */
public class ContainerArcaneAssembler extends ContainerBase {
    protected TileArcaneAssembler TE;

    public ContainerArcaneAssembler(EntityPlayer player, TileArcaneAssembler TE) {
        super(player);
        this.TE = TE;
        this.addSlot(new SlotKnowledgeCore(this.getInventory("cores"), 0, 81, 66), ThESlotSemantics.KNOWLEDGE_CORE);
        for (int i = 0; i < this.getInventory("upgrades").getSlots(); i++)
            this.addSlot(new SlotUpgrade(this.getInventory("upgrades"), i, 186, 8 + i * 18), SlotSemantics.UPGRADE);
        this.bindPlayerInventory(new PlayerMainInvWrapper(player.inventory), 0, 147);
        this.addListener(new KnowledgeCoreSlotListener());
        if (ForgeUtil.isServer())
            TE.subscribe(player);   // subscribe to aspect availability updates
    }

    public IItemHandler getInventory(String name) {
        return this.TE.getInventoryByName(name);
    }

    public TileArcaneAssembler getTE() {
        return TE;
    }

    public void playCoreSound(EntityPlayer player) { // plays the right sound, when the Knowledge Core gets removed or placed in the slot
        if (this.getInventory("cores").getStackInSlot(0).isEmpty()) {
            player.world.playSound(player, TE.getPos(), new SoundEvent(ThEFeatures.instance().sounds().knowledgeCorePowerDown()), SoundCategory.BLOCKS, 1, 1);
            PacketHandler.sendToPlayer((EntityPlayerMP) player, new PacketPlaySound(TE.getPos(), ThEFeatures.instance().sounds().knowledgeCorePowerDown(), SoundCategory.BLOCKS, 1, 1));
        } else {
            player.world.playSound(player, TE.getPos(), new SoundEvent(ThEFeatures.instance().sounds().knowledgeCorePowerUp()), SoundCategory.BLOCKS, 1, 1);
            PacketHandler.sendToPlayer((EntityPlayerMP) player, new PacketPlaySound(TE.getPos(), ThEFeatures.instance().sounds().knowledgeCorePowerUp(), SoundCategory.BLOCKS, 1, 1));
        }
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
