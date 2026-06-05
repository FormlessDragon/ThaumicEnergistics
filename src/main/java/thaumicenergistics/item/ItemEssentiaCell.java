package thaumicenergistics.item;

import ae2.api.config.FuzzyMode;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.AEKeyType;
import ae2.api.storage.StorageCells;
import ae2.api.storage.cells.IBasicCellItem;
import ae2.api.storage.cells.StorageCell;
import ae2.items.contents.CellConfig;
import ae2.util.ConfigInventory;
import com.google.common.base.Preconditions;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.items.ItemBase;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.me.key.AEEssentiaKeys;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.client.render.IThEModel;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.util.ForgeUtil;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author BrockWS
 */
public class ItemEssentiaCell extends ItemBase implements IBasicCellItem, IThEModel {

    private final String size;
    private final int bytes;
    private final int types;

    public ItemEssentiaCell(String size, int bytes, int types) {
        super("essentia_cell_" + size);

        this.size = size;
        this.bytes = bytes;
        this.types = types;

        this.setMaxStackSize(1);
        this.setMaxDamage(0);
        this.setHasSubtypes(false);
        this.setCreativeTab(ModGlobals.CREATIVE_TAB);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!player.isSneaking())
            return super.onItemRightClick(world, player, hand);
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty())
            return super.onItemRightClick(world, player, hand);
        StorageCell cell = StorageCells.getCellInventory(held, null);
        if (cell == null || !cell.getAvailableStacks().isEmpty()) // Only try to separate cell if empty
            return super.onItemRightClick(world, player, hand);

        Optional<ItemStack> cellComponentOptional = this.getComponentOfCell(held);
        if (cellComponentOptional.isEmpty())
            return super.onItemRightClick(world, player, hand);

        ItemStack emptyCasing = ae2.core.definitions.AEItems.ITEM_CELL_HOUSING.stack();
        ItemStack cellComponent = cellComponentOptional.get();
        player.setHeldItem(hand, ItemStack.EMPTY);

        this.addOrDrop(player, cellComponent);
        this.addOrDrop(player, emptyCasing);

        if (player.inventoryContainer != null)
            player.inventoryContainer.detectAndSendChanges();

        return ActionResult.newResult(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    private void addOrDrop(EntityPlayer player, ItemStack stack) {
        ItemStack remainder = ForgeUtil.addStackToPlayerInventory(player, stack, false);
        if (!remainder.isEmpty()) {
            player.dropItem(remainder, false);
        }
    }

    private Optional<ItemStack> getComponentOfCell(ItemStack stack) {
        Preconditions.checkNotNull(stack);
        Preconditions.checkNotNull(stack.getItem());
        Preconditions.checkNotNull(stack.getItem().getRegistryName());
        Preconditions.checkNotNull(stack.getItem().getRegistryName().getPath());
        ItemStack optional;
        switch (stack.getItem().getRegistryName().getPath().split("_")[2]) {
            case "1k":
                optional = ThEItems.ESSENTIA_COMPONENT_1K.stack(1);
                break;
            case "4k":
                optional = ThEItems.ESSENTIA_COMPONENT_4K.stack(1);
                break;
            case "16k":
                optional = ThEItems.ESSENTIA_COMPONENT_16K.stack(1);
                break;
            case "64k":
                optional = ThEItems.ESSENTIA_COMPONENT_64K.stack(1);
                break;
            default:
                return Optional.empty();
        }
        return Optional.of(optional);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        this.addCellInformationToTooltip(stack, tooltip);
    }

    @Override
    public AEKeyType getKeyType() {
        return AEEssentiaKeys.INSTANCE;
    }

    @Override
    public int getBytes(ItemStack itemStack) {
        return this.bytes;
    }

    @Override
    public int getBytesPerType(ItemStack itemStack) {
        return 8;
    }

    @Override
    public int getTotalTypes(ItemStack itemStack) {
        return this.types;
    }

    @Override
    public boolean isBlackListed(ItemStack itemStack, AEKey key) {
        return !(key instanceof AEEssentiaKey);
    }

    @Override
    public boolean storableInStorageCell() {
        return false;
    }

    @Override
    public boolean isStorageCell(ItemStack itemStack) {
        return true;
    }

    @Override
    public double getIdleDrain() {
        return 1;
    }

    @Override
    public boolean isEditable(ItemStack itemStack) {
        return true;
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack itemStack) {
        return CellConfig.create(Collections.singleton(AEEssentiaKeys.INSTANCE), itemStack);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack itemStack) {
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack itemStack, FuzzyMode fuzzyMode) {

    }

    @Override
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(Reference.MOD_ID + ":cell/essentia_cell_" + this.size, "inventory"));
    }
}
