package thaumicenergistics.part;

import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.container.ISubGui;
import ae2.items.parts.PartModels;
import ae2.parts.reporting.AbstractTerminalPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.client.gui.GuiHandler;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.integration.appeng.ThEPartModel;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.util.AEUtil;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.ItemHandlerUtil;
import thaumicenergistics.util.inventory.ThEInternalInventory;
import thaumicenergistics.util.inventory.ThEUpgradeInventory;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author BrockWS
 * @author Alex811
 */
public class PartArcaneTerminal extends AbstractTerminalPart implements IArcaneTerminalHost {

    @PartModels
    public static final ResourceLocation MODEL_BASE = new ResourceLocation(Reference.MOD_ID, "part/arcane_terminal/base");
    @PartModels
    public static final ResourceLocation MODEL_ON = new ResourceLocation(Reference.MOD_ID, "part/arcane_terminal/on");
    @PartModels
    public static final ResourceLocation MODEL_OFF = new ResourceLocation(Reference.MOD_ID, "part/arcane_terminal/off");

    private static final IPartModel MODELS_ON = new ThEPartModel(MODEL_BASE, MODEL_ON,
            new ResourceLocation(ModGlobals.MOD_ID_AE2, "part/display_status_on"));
    private static final IPartModel MODELS_OFF = new ThEPartModel(MODEL_BASE, MODEL_OFF,
            new ResourceLocation(ModGlobals.MOD_ID_AE2, "part/display_status_off"));
    private static final IPartModel MODELS_HAS_CHANNEL = new ThEPartModel(MODEL_BASE, MODEL_ON,
            new ResourceLocation(ModGlobals.MOD_ID_AE2, "part/display_status_has_channel"));

    protected final ThEInternalInventory craftingInventory;
    protected ThEInternalInventory upgradeInventory;
    private final ModGUIs gui;

    public PartArcaneTerminal(IPartItem<?> item) {
        this(item, ModGUIs.ARCANE_TERMINAL);
    }

    protected PartArcaneTerminal(IPartItem<?> item, ModGUIs gui) {
        super(item);
        this.gui = gui;
        this.craftingInventory = new ThEInternalInventory("matrix", 15, 64) {
            @Override
            public void markDirty() {
                super.markDirty();
                PartArcaneTerminal.this.saveChanges();
            }
        };
        this.upgradeInventory = new ThEUpgradeInventory("upgrades", 1, 1, this.getPartItem().asItemStack()) {
            @Override
            public void markDirty() {
                super.markDirty();
                PartArcaneTerminal.this.saveChanges();
            }
        };
    }

    @Override
    public ModGUIs getGui() {
        return this.gui;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if (name.equalsIgnoreCase("crafting")) {
            return new InvWrapper(this.craftingInventory);
        }
        if (name.equalsIgnoreCase("upgrades")) {
            return new InvWrapper(this.upgradeInventory);
        }
        return null;
    }

    @Override
    public void saveChanges() {
        if (this.getHost() != null) {
            super.saveChanges();
        }
    }

    public TileEntity getTile() {
        return this.getTileEntity();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("crafting")) {
            this.craftingInventory.deserializeNBT(tag.getTagList("crafting", 10));
        }
        if (tag.hasKey("upgrades")) {
            this.upgradeInventory.deserializeNBT(tag.getTagList("upgrades", 10));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag("crafting", this.craftingInventory.serializeNBT());
        tag.setTag("upgrades", this.upgradeInventory.serializeNBT());
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        this.addArcaneDrops(drops);
    }

    protected void addArcaneDrops(List<ItemStack> drops) {
        drops.addAll(ItemHandlerUtil.getInventoryAsList(this.getInventoryByName("crafting")));
        drops.addAll(ItemHandlerUtil.getInventoryAsList(this.getInventoryByName("upgrades")));
    }

    @Override
    public boolean hasVisSource() {
        return this.getTile() != null && this.getTile().hasWorld();
    }

    @Override
    public World getVisWorld() {
        return this.getTile().getWorld();
    }

    @Override
    public BlockPos getVisPos() {
        return this.getTile().getPos();
    }

    @Override
    public BlockPos getReturnPos() {
        return this.getTile().getPos();
    }

    @Override
    public EnumFacing getReturnSide() {
        return this.getSide();
    }

    @Override
    public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        GuiHandler.openGUI(this.getGui(), player, this.getReturnPos(), this.getReturnSide());
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return this.getPartItem().asItemStack();
    }

    @Override
    public boolean onUseItemOn(ItemStack itemStack, EntityPlayer player, EnumHand hand, Vec3d pos) {
        if (super.onUseItemOn(itemStack, player, hand, pos)) {
            return true;
        }
        if (player.isSneaking() && AEUtil.isWrench(player.getHeldItem(hand), player, this.getTile().getPos())) {
            return false;
        }
        return this.openTerminalGui(player);
    }

    @Override
    public boolean onUseWithoutItem(EntityPlayer player, Vec3d pos) {
        return this.openTerminalGui(player);
    }

    private boolean openTerminalGui(EntityPlayer player) {
        if (ForgeUtil.isServer()) {
            GuiHandler.openGUI(this.getGui(), player, this.getReturnPos(), this.getReturnSide());
        }
        this.getHost().markForUpdate();
        return true;
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }
}
