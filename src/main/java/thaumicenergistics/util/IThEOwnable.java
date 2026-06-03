package thaumicenergistics.util;

import net.minecraft.entity.player.EntityPlayer;

/**
 * @author Alex811
 */
public interface IThEOwnable {
    void setOwner(EntityPlayer player);

    EntityPlayer getOwner();

    default void initGridNodeOwner() {
        // Stage 1 Supergiant port: ownership is applied by managed grid node owners.
    }
}
