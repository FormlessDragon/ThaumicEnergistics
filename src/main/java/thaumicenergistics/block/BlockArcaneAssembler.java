package thaumicenergistics.block;

import ae2.block.AEBaseTileBlock;
import ae2.core.gui.locator.GuiHostLocators;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumicenergistics.common.gui.ThEGuiOpener;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.tile.TileArcaneAssembler;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Alex811
 */
public class BlockArcaneAssembler extends AEBaseTileBlock<TileArcaneAssembler> {

    public static final PropertyBool ACTIVE = PropertyBool.create("active");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockArcaneAssembler() {
        super(Material.IRON);
        this.setTileEntity(TileArcaneAssembler.class);
        this.setDefaultState(this.blockState.getBaseState()
            .withProperty(ACTIVE, Boolean.FALSE)
            .withProperty(POWERED, Boolean.FALSE));
        this.blockSoundType = SoundType.GLASS;
        this.fullBlock = false;
        this.lightOpacity = 1;
        this.translucent = true;
        this.setHarvestLevel("pickaxe", 0);
        this.setHardness(2.2F);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return createBlockState(ACTIVE, POWERED);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(ACTIVE) ? 1 : 0) | (state.getValue(POWERED) ? 2 : 0);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
            .withProperty(ACTIVE, (meta & 1) != 0)
            .withProperty(POWERED, (meta & 2) != 0);
    }

    @Override
    protected IBlockState updateBlockStateFromTileEntity(IBlockState currentState, TileArcaneAssembler tileEntity) {
        return currentState
            .withProperty(ACTIVE, tileEntity.getMainNode().isActive())
            .withProperty(POWERED, tileEntity.getMainNode().isPowered());
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ)) {
            return true;
        }

        if (player.isSneaking()) {
            return false;
        }

        TileArcaneAssembler tile = this.getTileEntity(world, pos);
        if (tile != null) {
            if(!world.isRemote) {
                ThEGuiOpener.openLocatorGui(player, ModGUIs.ARCANE_ASSEMBLER,
                    GuiHostLocators.forTile(tile), false);
            }
            return true;
        }
        return false;
    }

}
