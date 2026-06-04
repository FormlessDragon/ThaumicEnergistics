package mcjty.theoneprobe.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public interface IProbeHitData {
    BlockPos getPos();

    Vec3d getHitVec();
}
