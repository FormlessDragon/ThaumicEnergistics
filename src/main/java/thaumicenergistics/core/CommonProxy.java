package thaumicenergistics.core;

import ae2.api.stacks.AEKeyTypes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumicenergistics.common.strategy.EssentiaContainerItemStrategy;
import thaumicenergistics.common.strategy.EssentiaExternalStorageStrategy;
import thaumicenergistics.common.strategy.EssentiaStackExportStrategy;
import thaumicenergistics.common.strategy.EssentiaStackImportStrategy;
import thaumicenergistics.me.key.AEEssentiaKeys;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
        AEKeyTypes.register(AEEssentiaKeys.INSTANCE);
        EssentiaContainerItemStrategy.register();
        EssentiaStackImportStrategy.register();
        EssentiaStackExportStrategy.register();
        EssentiaExternalStorageStrategy.register();
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    public EntityPlayer getPlayerEntFromCtx(MessageContext ctx) {
        return ctx.getServerHandler().player;
    }

}
