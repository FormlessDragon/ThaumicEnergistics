package thaumicenergistics.integration;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModAPIManager;
import thaumicenergistics.integration.invtweaks.ThEInvTweaks;
import thaumicenergistics.integration.thaumcraft.ThEThaumcraft;
import thaumicenergistics.core.ThELog;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 * @author Alex811
 */
public class ThEIntegrationLoader {
    private static final HashMap<IThEIntegration, String> INTEGRATIONS = new HashMap<>();
    private static final ModAPIManager apiManager = ModAPIManager.INSTANCE;

    static {
        registerIntegration("thaumcraft", ThEThaumcraft.class);
        registerIntegration("inventorytweaks", ThEInvTweaks.class);
    }

    private static void registerIntegration(String modId, Class<? extends IThEIntegration> integration) {
        if (Loader.isModLoaded(modId) || apiManager.hasAPI(modId)) {
            try {
                INTEGRATIONS.put(integration.getDeclaredConstructor().newInstance(), modId);
                ThELog.info("Integrations: Registered [" + modId + "]");
            } catch (InstantiationException | IllegalAccessException ex) {
                ThELog.error("Failed to instantiate an integration class", ex);
            } catch (InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        } else
            ThELog.debug("Integrations: Not found [" + modId + "]");
    }

    public static String getModId(IThEIntegration integration) {
        return INTEGRATIONS.get(integration);
    }

    public static void preInit() {
        ThELog.info("Integrations: PreInit");
        INTEGRATIONS.keySet().forEach(IThEIntegration::preInit);
    }

    public static void init() {
        ThELog.info("Integrations: Init");
        INTEGRATIONS.keySet().forEach(IThEIntegration::init);
    }

    public static void postInit() {
        ThELog.info("Integrations: PostInit");
        INTEGRATIONS.keySet().forEach(IThEIntegration::postInit);
    }
}
