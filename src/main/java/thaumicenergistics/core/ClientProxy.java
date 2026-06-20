package thaumicenergistics.core;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumicenergistics.client.gui.ThEClientGuiOpener;
import thaumicenergistics.client.render.ArcaneAssemblerRenderer;
import thaumicenergistics.client.render.EssentiaKeyRenderHandler;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;
import thaumicenergistics.tile.TileArcaneAssembler;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        EssentiaKeyRenderHandler.register();
        // Init TESR
        ClientRegistry.bindTileEntitySpecialRenderer(TileArcaneAssembler.class, new ArcaneAssemblerRenderer());
    }

    public EntityPlayer getPlayerEntFromCtx(MessageContext ctx) {
        return Minecraft.getMinecraft().player;
    }

    @Override
    public void openLocatorGui(PacketOpenLocatorGUI message, MessageContext ctx) {
        ThEClientGuiOpener.openLocatorGui(message);
    }

}
