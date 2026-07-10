package thaumicenergistics.core;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import thaumicenergistics.thaumicenergistics.Reference;

/**
 * @author Alex811
 * Registers sprites used by Thaumic Energistics client screens in the AE2 texture atlas.
 */
@Mod.EventBusSubscriber(Side.CLIENT)
public final class ThETextures {

    public static final ResourceLocation KNOWLEDGE_CORE_SLOT =
            new ResourceLocation(Reference.MOD_ID, "gui/slot/knowledge_core");

    private ThETextures() {
    }

    @SubscribeEvent
    public static void textureStitch(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(KNOWLEDGE_CORE_SLOT);
    }

}
