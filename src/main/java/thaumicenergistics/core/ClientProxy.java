package thaumicenergistics.core;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import thaumicenergistics.client.render.ArcaneVisKeyRenderHandler;
import thaumicenergistics.client.render.ArcaneAssemblerRenderer;
import thaumicenergistics.client.render.EssentiaKeyRenderHandler;
import thaumicenergistics.tile.TileArcaneAssembler;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        EssentiaKeyRenderHandler.register();
        ArcaneVisKeyRenderHandler.register();
        // Init TESR
        ClientRegistry.bindTileEntitySpecialRenderer(TileArcaneAssembler.class, new ArcaneAssemblerRenderer());
    }

}
