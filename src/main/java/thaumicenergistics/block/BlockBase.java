package thaumicenergistics.block;

import ae2.block.AEBaseTileBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import thaumicenergistics.tile.ThENetworkTile;

public class BlockBase<T extends ThENetworkTile> extends AEBaseTileBlock<T> {

    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockBase(Class<T> tileClass) {
        super(Material.IRON);
        this.setTileEntity(tileClass);
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(ACTIVE, Boolean.FALSE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return createBlockState(ACTIVE);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(ACTIVE) ? 1 : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(ACTIVE, meta != 0);
    }

    @Override
    protected IBlockState updateBlockStateFromTileEntity(IBlockState currentState, T tileEntity) {
        return currentState.withProperty(ACTIVE, tileEntity.isActive());
    }

}
