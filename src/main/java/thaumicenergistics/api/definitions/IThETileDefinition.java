package thaumicenergistics.api.definitions;

import net.minecraft.tileentity.TileEntity;

import java.util.Optional;

public interface IThETileDefinition extends IThEBlockDefinition {

    Optional<? extends Class<? extends TileEntity>> maybeEntity();
}
