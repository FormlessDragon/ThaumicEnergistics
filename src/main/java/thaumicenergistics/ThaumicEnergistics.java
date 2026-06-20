package thaumicenergistics;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import thaumicenergistics.core.CommonProxy;
import thaumicenergistics.core.ThEFeatureAccess;
import thaumicenergistics.core.ThEFeatures;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.client.gui.GuiHandler;
import thaumicenergistics.command.CommandAddVis;
import thaumicenergistics.command.CommandDrainVis;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.init.internal.InitUpgrades;
import thaumicenergistics.integration.ThEIntegrationLoader;
import thaumicenergistics.network.PacketHandler;

/**
 * <strong>Thaumic Energistics</strong>
 * <hr>
 * A bridge between Thaumcraft and Applied Energistics. Essentia storage management, transportation, and application.
 *
 * @author Nividica
 */
@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION, dependencies = ModGlobals.MOD_DEPENDENCIES)
@Mod.EventBusSubscriber
public class ThaumicEnergistics {

    private static final String CLIENT_PROXY = "thaumicenergistics.core.ClientProxy";
    private static final String COMMON_PROXY = "thaumicenergistics.core.CommonProxy";

    /**
     * Singleton instance
     */
    @Mod.Instance(value = Reference.MOD_ID)
    public static ThaumicEnergistics INSTANCE;

    /**
     * Proxy class that runs code that should be strictly on the physical client
     */
    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = COMMON_PROXY)
    public static CommonProxy proxy;

    /**
     * Thaumic Energistics Logger
     */
    public static Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);

    /**
     * Called before the load event.
     *
     * @param event FMLPreInitializationEvent
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("{} preInit", Reference.MOD_NAME);
        LOGGER.debug("Initialized feature access through {}", bootstrapFeatures().getClass().getName());
        MinecraftForge.EVENT_BUS.register(this);
        PacketHandler.register();

        proxy.preInit(event);

        ThEIntegrationLoader.preInit();
    }

    static ThEFeatureAccess bootstrapFeatures() {
        return ThEFeatures.bootstrap();
    }

    /**
     * Called after the preInit event, and before the post init event.
     *
     * @param event FMLInitializationEvent
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(ThaumicEnergistics.INSTANCE, new GuiHandler());
        /*if (ForgeUtil.isClient()) {
            ThEItemColors.registerItemColors();
        }*/

        InitUpgrades.init();

        proxy.init(event);

        ThEIntegrationLoader.init();
    }

    /**
     * Called after the load event.
     *
     * @param event FMLPostInitializationEvent
     */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);

        ThEIntegrationLoader.postInit();
    }

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        if (ModGlobals.DEBUG_MODE) {
            event.registerServerCommand(new CommandAddVis());
            event.registerServerCommand(new CommandDrainVis());
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        //Temporary alpha warning
        //TextComponentString s1 = new TextComponentString("Thaumic Energistics is currently in alpha. Post issues to GitHub");
        //s1.getStyle().setColor(TextFormatting.RED);
        //TextComponentString link = new TextComponentString("https://github.com/Nividica/ThaumicEnergistics");
        //link.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/Nividica/ThaumicEnergistics")).setColor(TextFormatting.GOLD);
        //
        //event.player.sendMessage(s1.appendSibling(link));
    }

    @SubscribeEvent
    public void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Reference.MOD_ID))
            ConfigManager.sync(Reference.MOD_ID, Config.Type.INSTANCE);
    }

    public static ResourceLocation id(String id) {
        return new ResourceLocation(Reference.MOD_ID, id);
    }
}
