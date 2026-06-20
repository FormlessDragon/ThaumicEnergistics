package thaumicenergistics;

import org.junit.jupiter.api.Test;
import thaumicenergistics.api.IThEApi;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.core.ThEFeatureAccess;
import thaumicenergistics.core.ThEFeatures;
import thaumicenergistics.thaumicenergistics.Reference;
import net.minecraft.init.Bootstrap;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@SuppressWarnings("deprecation")
class ThEFeaturesTest {

    @Test
    void localBootstrapExposesFeatureDefinitions() {
        bootstrapMinecraft();
        ThEFeatureAccess features = ThEFeatures.bootstrap();

        assertAll(
                () -> assertSame(features, ThEFeatures.instance()),
                () -> assertNotNull(features.items()),
                () -> assertNotNull(features.blocks()),
                () -> assertNotNull(features.config()),
                () -> assertNotNull(features.lang()),
                () -> assertNotNull(features.textures()),
                () -> assertNotNull(features.sounds()),
                () -> assertEquals(2, features.config().tickTimeArcaneAssemblerMin()),
                () -> assertEquals("gui.thaumicenergistics.arcane_terminal",
                        features.lang().guiArcaneTerminal().getUnlocalizedKey()),
                () -> assertEquals(Reference.MOD_ID,
                        features.textures().knowledgeCoreSlot().getNamespace()),
                () -> assertEquals("gui/slot/knowledge_core",
                        features.textures().knowledgeCoreSlot().getPath()),
                () -> assertEquals(Reference.MOD_ID,
                        features.sounds().knowledgeCoreWrite().getNamespace()),
                () -> assertEquals("knowledge_core_write",
                        features.sounds().knowledgeCoreWrite().getPath()),
                () -> assertFalse(features.config().essentiaContainerCapacity().isEmpty()));
    }

    @Test
    void deprecatedFacadesDelegateToLocalFeatureDefinitions() {
        bootstrapMinecraft();
        ThEFeatureAccess features = ThEFeatures.bootstrap();
        IThEApi thaumicEnergisticsApi = ThaumicEnergisticsApi.instance();
        IThEApi publicApi = ThEApi.instance();

        assertAll(
                () -> assertSame(thaumicEnergisticsApi, ThaumicEnergisticsApi.instance()),
                () -> assertSame(publicApi, ThEApi.instance()),
                () -> assertSame(thaumicEnergisticsApi, publicApi),
                () -> assertSame(features.items(), thaumicEnergisticsApi.items()),
                () -> assertSame(features.blocks(), thaumicEnergisticsApi.blocks()),
                () -> assertSame(features.config(), thaumicEnergisticsApi.config()),
                () -> assertSame(features.lang(), thaumicEnergisticsApi.lang()),
                () -> assertSame(features.textures(), thaumicEnergisticsApi.textures()),
                () -> assertSame(features.sounds(), thaumicEnergisticsApi.sounds()),
                () -> assertSame(features.items(), publicApi.items()),
                () -> assertSame(features.blocks(), publicApi.blocks()),
                () -> assertSame(features.config(), publicApi.config()),
                () -> assertSame(features.lang(), publicApi.lang()),
                () -> assertSame(features.textures(), publicApi.textures()),
                () -> assertSame(features.sounds(), publicApi.sounds()));
    }

    @Test
    void modStartupFeatureBootstrapUsesLocalFeatureDefinitions() {
        bootstrapMinecraft();
        ThEFeatureAccess startupFeatures = ThaumicEnergistics.bootstrapFeatures();

        assertAll(
                () -> assertSame(ThEFeatures.instance(), startupFeatures),
                () -> assertEquals("knowledge_core_power_up",
                        startupFeatures.sounds().knowledgeCorePowerUp().getPath()),
                () -> assertEquals("#", startupFeatures.config().aspectSearchPrefix()),
                () -> assertEquals("tooltip.thaumicenergistics.device_online",
                        startupFeatures.lang().deviceOnline().getUnlocalizedKey()));
    }

    private static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }
}
