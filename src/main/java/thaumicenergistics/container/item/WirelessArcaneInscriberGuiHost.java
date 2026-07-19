package thaumicenergistics.container.item;

import ae2.api.inventories.InternalInventory;
import ae2.api.util.DimensionalBlockPos;
import ae2.container.ISubGui;
import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.helpers.WirelessTerminalGuiHost;
import ae2.items.contents.StackDependentSupplier;
import ae2.items.tools.powered.WirelessTerminalItem;
import ae2.items.tools.powered.WirelessTerminals;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.InternalInventoryHost;
import ae2.util.inv.SupplierInternalInventory;
import ae2.util.inv.filter.IAEItemFilter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumicenergistics.api.storage.IArcaneInscriberHost;
import thaumicenergistics.client.gui.ModGUIs;
import thaumicenergistics.items.ItemKnowledgeCore;
import thaumicenergistics.part.inventory.ArcaneInscriberMatrixInventory;

import java.util.function.BiConsumer;

public class WirelessArcaneInscriberGuiHost extends WirelessTerminalGuiHost<WirelessTerminalItem> implements IArcaneInscriberHost {

    private static final String TAG_ARCANE_MATRIX = "arcaneMatrix";
    private static final String TAG_KNOWLEDGE_CORE = "knowledgeCore";

    private final SupplierInternalInventory<InternalInventory> craftingInventory;
    private final SupplierInternalInventory<InternalInventory> knowledgeCoreInventory;

    public WirelessArcaneInscriberGuiHost(WirelessTerminalItem stackItem,
                                          WirelessTerminalItem terminalItem,
                                          EntityPlayer player,
                                          ItemGuiHostLocator locator,
                                          BiConsumer<EntityPlayer, ISubGui> returnToMainContainer) {
        super(stackItem, terminalItem, player, locator, returnToMainContainer);
        this.craftingInventory = new SupplierInternalInventory<>(
            new StackDependentSupplier<>(this::getItemStack,
                stack -> createCraftingInventory(player, stack, terminalItem)));
        this.knowledgeCoreInventory = new SupplierInternalInventory<>(
            new StackDependentSupplier<>(this::getItemStack,
                stack -> createKnowledgeCoreInventory(player, stack, terminalItem)));
    }

    private static InternalInventory createCraftingInventory(EntityPlayer player, ItemStack stack,
                                                             WirelessTerminalItem terminal) {
        var craftingInventory = new ArcaneInscriberMatrixInventory(new InternalInventoryHost() {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inventory) {
                inventory.writeToNBT(WirelessTerminals.getTerminalData(stack, terminal), TAG_ARCANE_MATRIX);
            }

            @Override
            public boolean isClientSide() {
                return player.world.isRemote;
            }
        });
        NBTTagCompound data = WirelessTerminals.getExistingTerminalData(stack, terminal);
        if (data != null) {
            craftingInventory.readFromNBT(data, TAG_ARCANE_MATRIX);
        }
        return craftingInventory;
    }

    private static InternalInventory createKnowledgeCoreInventory(EntityPlayer player, ItemStack stack,
                                                                  WirelessTerminalItem terminal) {
        var knowledgeCoreInventory = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inventory) {
                inventory.writeToNBT(WirelessTerminals.getTerminalData(stack, terminal), TAG_KNOWLEDGE_CORE);
            }

            @Override
            public boolean isClientSide() {
                return player.world.isRemote;
            }
        }, 1, 1, new KnowledgeCoreFilter());
        NBTTagCompound data = WirelessTerminals.getExistingTerminalData(stack, terminal);
        if (data != null) {
            knowledgeCoreInventory.readFromNBT(data, TAG_KNOWLEDGE_CORE);
        }
        return knowledgeCoreInventory;
    }

    private DimensionalBlockPos getLinkedPosition() {
        WirelessTerminalItem terminal = getTerminalItem();
        return terminal.getLinkedPosition(getItemStack(), terminal);
    }

    @Override
    public InternalInventory getKnowledgeCoreInventory() {
        return this.knowledgeCoreInventory;
    }

    @Override
    public ModGUIs getGui() {
        return ModGUIs.WIRELESS_ARCANE_INSCRIBER;
    }

    @Override
    public InternalInventory getArcaneCraftingInventory() {
        return this.craftingInventory;
    }

    @Override
    public boolean hasVisSource() {
        return this.getLinkedPosition() != null && this.getLinkStatus().connected();
    }

    @Override
    public World getVisWorld() {
        DimensionalBlockPos linkedPosition = this.getLinkedPosition();
        return linkedPosition != null ? linkedPosition.getLevel() : this.getPlayer().world;
    }

    @Override
    public BlockPos getVisPos() {
        DimensionalBlockPos linkedPosition = this.getLinkedPosition();
        return linkedPosition != null ? linkedPosition.getPos() : this.getPlayer().getPosition();
    }

    @Override
    public BlockPos getReturnPos() {
        return this.getPlayer().getPosition();
    }

    @Override
    public EnumFacing getReturnSide() {
        return EnumFacing.UP;
    }

    private static final class KnowledgeCoreFilter implements IAEItemFilter {

        @Override
        public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof ItemKnowledgeCore;
        }
    }

}
