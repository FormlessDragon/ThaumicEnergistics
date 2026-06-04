package thaumicenergistics.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumicenergistics.common.strategy.EssentiaContainerItemStrategy;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
        EssentiaContainerItemStrategy.register();
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    public EntityPlayer getPlayerEntFromCtx(MessageContext ctx) {
        return ctx.getServerHandler().player;
    }

}
