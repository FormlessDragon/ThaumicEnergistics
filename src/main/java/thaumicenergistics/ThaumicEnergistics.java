package thaumicenergistics;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import thaumicenergistics.client.gui.GuiHandler;
import thaumicenergistics.core.ThEConfig;
import thaumicenergistics.core.CommonProxy;
import thaumicenergistics.core.ThESounds;
import thaumicenergistics.core.registries.AERegistries;
import thaumicenergistics.init.internal.InitStorageCells;
import thaumicenergistics.init.internal.InitUpgrades;
import thaumicenergistics.integration.thaumcraft.ThEThaumcraft;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.command.CommandAddVis;
import thaumicenergistics.command.CommandDrainVis;
import thaumicenergistics.core.ModGlobals;
import thaumicenergistics.core.ThELog;

import java.util.Objects;

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

    private boolean commonBootstrapInitialized;
    private boolean commonSetupInitialized;
    private boolean postRegistrationInitialized;

    public static ThaumicEnergistics instance() {
        return INSTANCE;
    }

    public static CommonProxy proxy() {
        return Objects.requireNonNull(proxy, "ThE proxy has not been injected yet.");
    }

    /**
     * Called before the load event.
     *
     * @param event FMLPreInitializationEvent
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ThEConfig.init();
        ThELog.info("{} preInit", Reference.MOD_NAME);
        initializeCommonBootstrap();
        proxy().preInit(event);
    }

    /**
     * Called after the preInit event, and before the post init event.
     *
     * @param event FMLInitializationEvent
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ThELog.info("{} init", Reference.MOD_NAME);
        initializeCommonSetup();
        proxy().init(event);
    }

    /**
     * Called after the load event.
     *
     * @param event FMLPostInitializationEvent
     */
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        ThELog.info("{} postInit", Reference.MOD_NAME);
        postRegistrationInitialization();
        proxy().postInit(event);
        ThEConfig.instance().save();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        if (ModGlobals.DEBUG_MODE) {
            event.registerServerCommand(new CommandAddVis());
            event.registerServerCommand(new CommandDrainVis());
        }
    }

    private void initializeCommonBootstrap() {
        if(this.commonBootstrapInitialized) {
            return;
        }

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ThEConfig.instance());
        AERegistries.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(ThaumicEnergistics.instance(), new GuiHandler());
        ThESounds.init();

        this.commonBootstrapInitialized = true;
    }

    private void initializeCommonSetup() {
        if(this.commonSetupInitialized) {
            return;
        }

        ThEThaumcraft.init();

        this.commonSetupInitialized = true;
    }

    private void postRegistrationInitialization() {
        if(this.postRegistrationInitialized) {
            return;
        }

        InitStorageCells.init();
        InitUpgrades.init();

        this.postRegistrationInitialized = true;
    }

    public static ResourceLocation id(String id) {
        return new ResourceLocation(Reference.MOD_ID, id);
    }

}
