package thaumicenergistics.block;

import ae2.core.gui.locator.GuiHostLocators;
import net.minecraft.block.SoundType;
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
public class BlockArcaneAssembler extends BlockBase<TileArcaneAssembler> {

    public BlockArcaneAssembler() {
        super(TileArcaneAssembler.class);
        this.blockSoundType = SoundType.GLASS;
        this.fullBlock = false;
        this.lightOpacity = 1;
        this.translucent = true;
        this.setHarvestLevel("pickaxe", 0);
        this.setHardness(2.2F);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote || hand != EnumHand.MAIN_HAND)
            return !player.isSneaking();
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileArcaneAssembler tile) {
            ThEGuiOpener.openLocatorGui(player, ModGUIs.ARCANE_ASSEMBLER,
                    GuiHostLocators.forTile(tile), false);
            return true;
        }
        return false;
    }

}
