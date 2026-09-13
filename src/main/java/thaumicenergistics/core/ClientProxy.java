package thaumicenergistics.core;

import ae2.client.gui.pattern.PatternGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import thaumicenergistics.client.gui.item.GuiKnowledgeCorePattern;
import thaumicenergistics.client.render.ArcaneVisKeyRenderHandler;
import thaumicenergistics.client.render.ArcaneAssemblerRenderer;
import thaumicenergistics.client.render.EssentiaKeyRenderHandler;
import thaumicenergistics.container.ContainerKnowledgeCorePattern;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.util.knowledgeCoreUtil.KnowledgeCoreUtil;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        EssentiaKeyRenderHandler.register();
        ArcaneVisKeyRenderHandler.register();
        // Init TESR
        ClientRegistry.bindTileEntitySpecialRenderer(TileArcaneAssembler.class, new ArcaneAssemblerRenderer());
        // Init pattern view
        PatternGuiHandler.register(KnowledgeCoreUtil.KnowledgeCorePatternDetails.class, pattern -> {
            InventoryPlayer inventory = Minecraft.getMinecraft().player.inventory;
            return new GuiKnowledgeCorePattern(new ContainerKnowledgeCorePattern(inventory, pattern), inventory);
        });
    }

}
