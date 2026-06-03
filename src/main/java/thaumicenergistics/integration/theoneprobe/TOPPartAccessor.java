package thaumicenergistics.integration.theoneprobe;

import ae2.api.parts.IPart;
import ae2.api.parts.IPartHost;
import api.java.mcjty.theoneprobe.api.IProbeHitData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import thaumicenergistics.part.PartBase;

/**
 * @author Alex811
 */
public class TOPPartAccessor {
    public static PartBase getPart(TileEntity te, IProbeHitData data) {
        if (te instanceof IPartHost) {
            BlockPos pos = data.getPos();
            Vec3d partPos = data.getHitVec().add(-pos.getX(), -pos.getY(), -pos.getZ());
            IPart part = ((IPartHost) te).selectPartLocal(partPos).part;
            if (part instanceof PartBase)
                return (PartBase) part;
        }
        return null;
    }
}
